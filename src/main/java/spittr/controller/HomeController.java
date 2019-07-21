package spittr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisPool;
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
        int hash = HashUtil.getHash(Long.toString(id));
        jedisPool.getResource().zadd(GlobalConstants.REDIS_SRC_DATA_ROLL_KEY, hash, id + "_" + content);
        return "id: " + id + " content: " + content + " hash: " + hash;
    }
}
