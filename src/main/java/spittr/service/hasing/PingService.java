package spittr.service.hasing;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import spittr.cfg.GlobalConstants;

public class PingService implements Runnable {
    private JedisPool jedisPool;
    private String ip;

    public PingService(JedisPool jedisPool, String ip) {
        this.jedisPool = jedisPool;
        this.ip = ip;
    }

    @Override
    public void run() {
        Jedis jedis = jedisPool.getResource();
        jedis.setex(GlobalConstants.REDIS_SERVER_PING_PREFIX + ip, 120, "value");
    }
}
