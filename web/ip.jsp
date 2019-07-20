<%@ page import="java.net.InetAddress" %><%
    InetAddress addr = InetAddress.getLocalHost();
    out.println("Local HostAddress:" + addr.getHostAddress());
    String hostname = addr.getHostName();
    out.println("Local host name: " + hostname);
%>
