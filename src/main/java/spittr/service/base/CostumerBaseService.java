package spittr.service.base;

import com.google.common.base.Splitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.MysqlUtil;

import java.util.List;

public class CostumerBaseService implements Runnable {

    private JedisPool jedisPool;
    private MysqlUtil mysqlUtil;

    public CostumerBaseService(JedisPool jedisPool, MysqlUtil mysqlUtil) {
        this.jedisPool = jedisPool;
        this.mysqlUtil = mysqlUtil;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getId() + " thread started.");
        Jedis jedis = jedisPool.getResource();
        while(true) {
            List<String> data = jedis.brpop(GlobalConstants.REDIS_TIMEOUT, GlobalConstants.REDIS_SRC_DATA_KEY);
            if(data == null || data.size() == 0) continue;
            if(data.get(0).equals(GlobalConstants.REDIS_SRC_DATA_KEY)) {
                processData(data.get(1));
            } else {
                processData(data.get(0));
            }
        }
    }

    private void processData(String data) {
        System.out.println(Thread.currentThread().getId() + " processing " + data);
        List<String> datas = Splitter.on("_").splitToList(data);
        mysqlUtil.exec(String.format("insert into data_sim values(%s, '%s', %s)", datas.get(0), datas.get(1), Integer.toString((int)(System.currentTimeMillis() / 1000))));
    }
}
