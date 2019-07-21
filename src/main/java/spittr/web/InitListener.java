package spittr.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServerConfig serverConfig = ServerConfig.getInstance();
//        Jedis jedis = new Jedis(GlobalConstants.REDIS_SRC_DATA_ROLL_KEY);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
