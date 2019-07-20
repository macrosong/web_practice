package spittr.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServerConfig serverConfig = ServerConfig.getInstance();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
