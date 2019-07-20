<%@ page import="java.net.InetAddress" %>
<%@ page import="spittr.web.ServerConfig" %><%
    InetAddress addr = InetAddress.getLocalHost();
    out.println("Local HostAddress:" + addr.getHostAddress());
    ServerConfig serverConfig = ServerConfig.getInstance();
    out.println("Local Hash:" + serverConfig.getServerHash());
%>
