package spittr.controller;

import jdk.nashorn.internal.objects.Global;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import spittr.cfg.GlobalConstants;
import spittr.service.hasing.ServerConfig;
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
    @RequestMapping(value = "/autogetwithhash", method = RequestMethod.GET)
    public String autoGetWithHash() {
        Jedis jedis = jedisPool.getResource();
        try {
            String id = Long.toString(jedis.incr("auto_incr"));
            String content = "test:content:" + id;
            int hash = ServerConfig.getInstance().getHash(content);
            jedis.lpush(GlobalConstants.REDIS_LIST_DATA_PREFIX + hash, id + "_" + content);
            return "<producer> hash: " + hash + " id: " + id + " content: " + id + "_" + content;
        } finally {
            if(jedis != null) {
                jedis.close();
            }
        }
    }

    @RequestMapping(value = "/hashinfo", method = RequestMethod.GET)
    public String hashinfo() {
        return ServerConfig.getInstance().getServerIpHashsMap().toString();
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    public String refresh() {
        ServerConfig.getInstance().refreshServer();
        return "success";
    }
}
