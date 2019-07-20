package spittr.web;

import lombok.Data;
import spittr.util.HashUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Data
public class ServerConfig {
    private int serverHash;
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
            serverConfig.setServerHash(HashUtil.getHash(addr.getHostAddress()));
        }
        return serverConfig;
    }


}
