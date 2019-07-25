package spittr.web;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import redis.clients.jedis.JedisPool;
import spittr.cfg.GlobalConstants;
import spittr.service.*;
import spittr.util.MysqlUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InitListener implements ServletContextListener {

    private static ExecutorService executor = Executors.newFixedThreadPool(GlobalConstants.VIRTUAL_NODE_NUM - 1);
    private static ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        ServerConfig serverConfig = ServerConfig.getInstance();
        WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
        MysqlUtil mysqlUtil = context.getBean(MysqlUtil.class);
        JedisPool jedisPool = context.getBean(JedisPool.class);
        switch (GlobalConstants.SIM_TYPE) {
            case 1:
                for (int i = 0; i < GlobalConstants.VIRTUAL_NODE_NUM - 1; i++) {
                    executor.submit(new CostumerBaseService(jedisPool, mysqlUtil));
                }
                break;
            case 2:
                for (int i = 0; i < GlobalConstants.VIRTUAL_NODE_NUM - 1; i++) {
                    executor.submit(new CostumerService(jedisPool, mysqlUtil));
                }
                monitorExecutor.scheduleAtFixedRate(new MonitorService(jedisPool, mysqlUtil), GlobalConstants.MONITOR_PERIOD, GlobalConstants.MONITOR_PERIOD, TimeUnit.SECONDS);
                break;
            case 3:
                for (int i = 0; i < GlobalConstants.VIRTUAL_NODE_NUM - 1; i++) {
                    executor.submit(new CostumerWIthSubsService(jedisPool, mysqlUtil));
                }
                executor.submit(() -> {
                    jedisPool.getResource().psubscribe(new ExceptionNotificationListener(jedisPool, mysqlUtil), "__keyspace@0__:*test:content*");//过期队列
                });

                break;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
