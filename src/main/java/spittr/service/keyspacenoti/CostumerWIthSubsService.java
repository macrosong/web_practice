package spittr.service.keyspacenoti;

import com.google.common.base.Splitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.util.List;

public class CostumerWIthSubsService implements Runnable {

    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;

    public CostumerWIthSubsService(JedisPool jedisPool, MysqlUtil mysqlUtil) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getId() + " thread started.");
        Jedis jedis = jedisPool.getResource();
        while(true) {
            String data = jedis.brpoplpush(GlobalConstants.REDIS_SRC_DATA_KEY, GlobalConstants.REDIS_SRC_DATA_2_KEY, GlobalConstants.REDIS_TIMEOUT);
            if(data == null) continue;
            processData(data);
            Transaction multi = jedis.multi();
            multi.del(data);
            multi.lrem(GlobalConstants.REDIS_SRC_DATA_2_KEY, 1, data);
            multi.exec();
        }
    }

    private void processData(String data) {
        System.out.println(Thread.currentThread().getId() + " processing " + data);
        List<String> datas = Splitter.on("_").splitToList(data);
        mysqlUtil.exec(String.format("insert into data_sim values(%s, '%s', %s)", datas.get(0), datas.get(1), Integer.toString((int)(System.currentTimeMillis() / 1000))));
    }
}
