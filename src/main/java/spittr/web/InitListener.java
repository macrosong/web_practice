package spittr.web;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import redis.clients.jedis.JedisPool;
import spittr.cfg.GlobalConstants;
import spittr.service.base.CostumerBaseService;
import spittr.service.hasing.CostumerHashService;
import spittr.service.hasing.HashMonitorListener;
import spittr.service.hasing.PingService;
import spittr.service.hasing.ServerConfig;
import spittr.service.keyspacenoti.CostumerWIthSubsService;
import spittr.service.keyspacenoti.ExceptionNotificationListener;
import spittr.service.safequeue.CostumerService;
import spittr.service.safequeue.MonitorService;
import spittr.util.MysqlUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.*;

public class InitListener implements ServletContextListener {

    private static ExecutorService executor = Executors.newFixedThreadPool(GlobalConstants.VIRTUAL_NODE_NUM);
    private static ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
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
            case 4:
                String hostIp = ServerConfig.init(jedisPool);
                for (int i = 0; i < GlobalConstants.VIRTUAL_NODE_NUM - 1; i++) {
                    executor.submit(new CostumerHashService(jedisPool, mysqlUtil, ServerConfig.getInstance().getServerIpHashsMap().get(hostIp).get(i)));
                }
                executor.submit(() -> {
                    jedisPool.getResource().psubscribe(new HashMonitorListener(jedisPool, mysqlUtil, hostIp), "__keyspace@0__:*");//过期队列
                });
                monitorExecutor.scheduleAtFixedRate(new PingService(jedisPool, hostIp), 0, 60, TimeUnit.SECONDS);
                break;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
