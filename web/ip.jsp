<%@ page import="java.net.InetAddress" %>
<%@ page import="spittr.web.ServerConfig" %><%
    InetAddress addr = InetAddress.getLocalHost();
    out.println("Local HostAddress:" + addr.getHostAddress());
    ServerConfig serverConfig = ServerConfig.getInstance();
    out.println("Local Hash:");
    for(int hash : serverConfig.getVirtualNodes().keySet()) {
        out.println("Node name: " + serverConfig.getVirtualNodes().get(hash) + " Hash: " + hash);
    }
%>
