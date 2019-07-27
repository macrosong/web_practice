package spittr.service.hasing;

import com.google.common.base.Splitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.util.List;

public class CostumerHashService implements Runnable {
    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;
    private int hashNum;

    public CostumerHashService(JedisPool jedisPool, MysqlUtil mysqlUtil, int hashNum) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
        this.hashNum = hashNum;
    }

    @Override
    public void run() {
        try {
            System.out.println(Thread.currentThread().getId() + " hash: " + hashNum + " thread started.");
            Jedis jedis = jedisPool.getResource();
            while(true) {
                String data = jedis.brpoplpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + hashNum, GlobalConstants.REDIS_LIST_2_DATA_PREFIX + hashNum, GlobalConstants.REDIS_TIMEOUT);
                System.out.println("<Costumer> hash: " + hashNum + " process data: " + data);
                if(data == null) continue;
                processData(data);
                jedis.lrem(GlobalConstants.REDIS_LIST_2_DATA_PREFIX + hashNum, 1, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processData(String data) {
        System.out.println(Thread.currentThread().getId() + " processing " + data);
        List<String> datas = Splitter.on("_").splitToList(data);
        mysqlUtil.exec(String.format("insert into data_sim values(%s, '%s', %s)", datas.get(0), datas.get(1), Integer.toString((int)(System.currentTimeMillis() / 1000))));
    }
}
