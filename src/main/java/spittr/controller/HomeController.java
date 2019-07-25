package spittr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.util.HashUtil;

@RestController
@RequestMapping(value = "/user")
public class HomeController {

    @Autowired
    private JedisPool jedisPool;

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String home() {
        return "home";
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public String process(@RequestParam(value = "id") long id, @RequestParam(value = "content") String content) {
        jedisPool.getResource().lpush(GlobalConstants.REDIS_SRC_DATA_KEY, id + "_" + content);
        return "id: " + id + " content: " + content;
    }

    @RequestMapping(value = "/autoget", method = RequestMethod.GET)
    public String autoGet() {
        Jedis jedis = jedisPool.getResource();
        try {
            String id = Long.toString(jedis.incr("auto_incr"));
            String content = "test:content:" + id;
            jedis.lpush(GlobalConstants.REDIS_SRC_DATA_KEY, id + "_" + content);
            return "id: " + id + " content: " + content;
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
    }
    @RequestMapping(value = "/autogetwithsubs", method = RequestMethod.GET)
    public String autoGetWithSubs() {
        Jedis jedis = jedisPool.getResource();
        try {
            String id = Long.toString(jedis.incr("auto_incr"));
            String content = "test:content:" + id;
            Transaction transaction = jedis.multi();
            transaction.lpush(GlobalConstants.REDIS_SRC_DATA_KEY, id + "_" + content);
            transaction.set(id + "_" + content, "test", "NX", "EX", 5);
            transaction.exec();
            return "id: " + id + " content: " + content;
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
    }
}
