package spittr.web;

import lombok.Data;
import spittr.util.HashUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.SortedMap;
import java.util.TreeMap;

import static spittr.cfg.GlobalConstants.VIRTUAL_NODE_NUM;

@Data
public class ServerConfig {
    //虚拟节点，key表示虚拟节点的hash值，value表示虚拟节点的名称
    private SortedMap<Integer, String> virtualNodes = new TreeMap<Integer, String>();
    private static ServerConfig serverConfig;

    private ServerConfig() {}
    public static ServerConfig getInstance() {
        if(serverConfig == null) {
            serverConfig = new ServerConfig();
            InetAddress addr = null;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String nodeName = String.format("%s&&VN%s", addr.getHostAddress(), String.valueOf(i));
                serverConfig.getVirtualNodes().put(HashUtil.getHash(nodeName), nodeName);
            }
        }
        return serverConfig;
    }


}
