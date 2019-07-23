package spittr.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MonitorService implements Runnable {
    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;

    public MonitorService(JedisPool jedisPool, MysqlUtil mysqlUtil) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getId() + " monitor thread started.");
        Jedis jedis = jedisPool.getResource();
        // 争夺任务锁
        if(jedis.set(GlobalConstants.REDIS_MONITOR_TASK_LOCK, "lock", "NX", "EX", GlobalConstants.MONITOR_LOCK_TIME) == null) {
            return;
        }
        // 获取所有的正在处理的list
        List<String> processingList = jedis.lrange(GlobalConstants.REDIS_SRC_DATA_TIME_KEY, 0, -1);
        if(processingList == null) return;
        for(String processData : processingList) {
            Double procTime = jedis.zscore(GlobalConstants.REDIS_SRC_DATA_TIME_KEY, processData);
            // 消费者可能断在brpoplpush之后，zset之前
            // 或者处理超时，即断在了事务之前
            if(procTime == null || System.currentTimeMillis() - procTime * 1000 >= GlobalConstants.MONITOR_PERIOD) {
                // 进一步判断数据是否处理完毕
                if(needProcess(processData)) {
                    Transaction multi = jedis.multi();
                    multi.zrem(GlobalConstants.REDIS_SRC_DATA_TIME_KEY, processData);
                    multi.lrem(GlobalConstants.REDIS_SRC_DATA_2_KEY, 1, processData);
                    multi.lpush(GlobalConstants.REDIS_SRC_DATA_KEY, processData);
                    multi.exec();
                }
            }
        }

        // 释放任务锁
        jedis.del(GlobalConstants.REDIS_MONITOR_TASK_LOCK);
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