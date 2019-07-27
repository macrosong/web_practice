package spittr.service.hasing;

import com.google.common.base.Splitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class HashMonitorListener extends JedisPubSub {
    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;
    private String host;

    public HashMonitorListener(JedisPool jedisPool, MysqlUtil mysqlUtil, String host) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
        this.host = host;
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        System.out.println("onPSubscribe " + pattern + " " + subscribedChannels);
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        System.out.println(
                "pattern = [" + pattern + "], channel = [" + channel + "], message = [" + message + "]");
        Jedis jedis = jedisPool.getResource();
        try {
            //收到消息 key的键值，处理过期提醒
            String data = Splitter.on(":").splitToList(channel).get(1);
            ServerConfig serverConfig = ServerConfig.getInstance();
            if(data.startsWith(GlobalConstants.REDIS_SERVER_ADD_SERVER_PREFIX)) {
                // 处理添加服务器信息
                String ip = data.substring(GlobalConstants.REDIS_SERVER_ADD_SERVER_PREFIX.length());
                if(!ip.equals(host)) {
                    serverConfig.addServer(ip);
                }
                System.out.println(serverConfig.getServerIpHashsMap());
            } else if(data.startsWith(GlobalConstants.REDIS_SERVER_PING_PREFIX)) {
                // 有服务器离线了，处理数据迁移
                String ip = data.substring(GlobalConstants.REDIS_SERVER_PING_PREFIX.length());
                List<Integer> hashes = serverConfig.getServerIpHashsMap().get(ip);
                int currentMigrateHash = 0;
                for(int hash : hashes) {
                    if(jedis.set(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX + hash, "test", "nx", "ex", GlobalConstants.REDIS_TIMEOUT) != null) {
                        currentMigrateHash = hash;
                        break;
                    }
                }
                serverConfig.removeServer(ip);
                // 有移植作业，开始进行
                if(currentMigrateHash != 0) {
                    int nextHash = serverConfig.getNextHash(currentMigrateHash);
                    // 队列移植
                    while(jedis.rpoplpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + currentMigrateHash, GlobalConstants.REDIS_LIST_DATA_PREFIX + nextHash) != null);
                    // 当前正在进行的队列移植
                    List<String> processingList = jedis.lrange(GlobalConstants.REDIS_LIST_2_DATA_PREFIX + currentMigrateHash, 0, -1);
                    if(processingList != null) {
                        for(String currentData : processingList) {
                            if(needProcess(data)) {
                                jedis.lpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + nextHash, currentData);
                            }
                        }
                    }
                    // 清理现场
                    Transaction transaction = jedis.multi();
                    transaction.del(GlobalConstants.REDIS_LIST_DATA_PREFIX + currentMigrateHash);
                    transaction.del(GlobalConstants.REDIS_LIST_2_DATA_PREFIX + currentMigrateHash);
                    transaction.del(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX + currentMigrateHash);
                    transaction.exec();
                }
            } else if(data.startsWith(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX)) {
                // 有人在处理数据迁移的时候断线了
                int currentMigrateHash = Integer.parseInt(data.substring(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX.length()));
                if(jedis.set(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX + currentMigrateHash, "test", "nx", "ex", GlobalConstants.REDIS_TIMEOUT) != null) {
                    int nextHash = serverConfig.getNextHash(currentMigrateHash);
                    // 队列移植
                    while(jedis.rpoplpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + currentMigrateHash, GlobalConstants.REDIS_LIST_DATA_PREFIX + nextHash) != null);
                    // 当前正在进行的队列移植
                    List<String> processingList = jedis.lrange(GlobalConstants.REDIS_LIST_2_DATA_PREFIX + currentMigrateHash, 0, -1);
                    if(processingList != null) {
                        for(String currentData : processingList) {
                            if(needProcess(data)) {
                                jedis.lpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + nextHash, currentData);
                            }
                        }
                    }
                    // 清理现场
                    Transaction transaction = jedis.multi();
                    transaction.del(GlobalConstants.REDIS_LIST_DATA_PREFIX + currentMigrateHash);
                    transaction.del(GlobalConstants.REDIS_LIST_2_DATA_PREFIX + currentMigrateHash);
                    transaction.del(GlobalConstants.REDIS_SERVER_MIGRATE_SERVER_PREFIX + currentMigrateHash);
                    transaction.exec();
                }
            }
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
    }

    private boolean needProcess(String data) {
        List<String> dataList = Splitter.on("_").splitToList(data);
        ResultSet resultSet = mysqlUtil.selectSQL("select uid, data from data_sim where uid=" + dataList.get(0));
        try {
            if(resultSet != null && resultSet.next()) {
                // 已经处理完毕的，无需再处理了
                if(resultSet.getString("data").equals(dataList.get(1))) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }
}
