package spittr.service;

import com.google.common.base.Splitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ExceptionNotificationListener extends JedisPubSub {

    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;

    public ExceptionNotificationListener(JedisPool jedisPool, MysqlUtil mysqlUtil) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
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
            // 锁住data，不让别的进程处理
            jedis.set(data, "test", "NX", "EX", 10);

            // 判断是否因为处理超时而还没有来的及处理，若不是，则重新加入队列，否则不释放锁结束
            List<String> processingList = jedis.lrange(GlobalConstants.REDIS_SRC_DATA_2_KEY, 0, -1);
            if(processingList == null) return;
            if(processingList.contains(data)) return;
            // 判断是否已经处理过，若处理过，释放锁，结束
            if(!needProcess(data)) {
                jedis.del(data);
                return;
            }
            jedis.lpush(GlobalConstants.REDIS_SRC_DATA_KEY, data);
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
