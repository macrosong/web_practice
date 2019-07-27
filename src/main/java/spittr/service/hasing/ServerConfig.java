package spittr.service.hasing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import spittr.cfg.GlobalConstants;
import spittr.util.HashUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static spittr.cfg.GlobalConstants.VIRTUAL_NODE_NUM;

@Data
public class ServerConfig {
    // 读写锁
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    // 服务器hash
    private TreeSet<Integer> serverHash = Sets.newTreeSet();
    // 服务器ip-全部虚拟地址
    private Map<String, List<Integer>> serverIpHashsMap = Maps.newHashMap();
    private SortedMap<Integer, String> virtualNodes = new TreeMap<>();
    private JedisPool jedisPool;

    private String hostAddress;

    private static ServerConfig serverConfig;
    private ServerConfig() {}

    // 获取数据的hash值，即知道应该放入哪个队列里
    public int getHash(String data) {
        int hash = HashUtil.getHash(data);
        r.lock();
        return calcNextHash(hash);
    }

    private int calcNextHash(int hash) {
        try {
            Integer floor = serverHash.floor(hash);
            if (floor == null) {
                return serverHash.first();
            }
            return floor;
        } finally {
            r.unlock();
        }
    }

    // 获取当前hash的移植后的hash值，即知道应该放入哪个队列里
    public int getNextHash(int hash) {
        r.lock();
        return calcNextHash(hash);
    }

    // 添加服务器
    public void addServer(String ip) {
        w.lock();
        try {
            addServer0(ip);
        } finally {
            w.unlock();
        }
    }

    // 移除服务器
    public void removeServer(String ip) {
        w.lock();
        try {
            List<Integer> hashList = serverIpHashsMap.get(ip);
            if(hashList == null) return;
            for(int hash : hashList) {
                serverHash.remove(hash);
            }
            serverIpHashsMap.remove(ip);
        } finally {
            w.unlock();
        }
    }

    // 刷新所有服务器
    public void refreshServer() {
        w.lock();
        Jedis jedis = jedisPool.getResource();
        try {
            // 取全部ip地址
            Set<String> servers = jedis.smembers(GlobalConstants.REDIS_SERVER_SET);
            Iterator<String> iterator = servers.iterator();
            while(iterator.hasNext()) {
                String ip = iterator.next();
                addServer0(ip);
            }
        } finally {
            w.unlock();
            jedis.close();
        }
    }



    public static ServerConfig getInstance() {
        return serverConfig;
    }

    public static String init(JedisPool jedisPool) {
        if(serverConfig == null) {
            serverConfig = new ServerConfig();
            serverConfig.setJedisPool(jedisPool);
            Jedis jedis = jedisPool.getResource();
            InetAddress addr = null;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String hostAddress = addr.getHostAddress();
            serverConfig.setHostAddress(hostAddress);
            // 取全部ip地址
            Set<String> servers = jedis.smembers(GlobalConstants.REDIS_SERVER_SET);
            // 不包含当前服务器，需要发送add_server消息给各服务器
            if(!servers.contains(hostAddress)) {
                servers.add(hostAddress);
                jedis.set(GlobalConstants.REDIS_SERVER_ADD_SERVER_PREFIX + hostAddress, "a", "nx", "ex", 5);
            }
            Iterator<String> iterator = servers.iterator();
            while(iterator.hasNext()) {
                String ip = iterator.next();
                addServer0(ip);
            }
            jedis.close();

        }
        return serverConfig.getHostAddress();
    }

    private static void addServer0(String ip) {
        for(int i = 0; i < VIRTUAL_NODE_NUM; i++) {
            String nodeName = String.format(GlobalConstants.HASH_FORMAT, ip, String.valueOf(i));
            int hash = HashUtil.getHash(nodeName);
            List<Integer> integerList = serverConfig.getServerIpHashsMap().get(ip);
            if(integerList == null) {
                integerList = Lists.newArrayList();
                serverConfig.getServerIpHashsMap().put(ip, integerList);
            }
            integerList.add(hash);
            serverConfig.getServerHash().add(hash);
        }
    }


}
