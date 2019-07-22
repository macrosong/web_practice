package spittr.cfg;

public class GlobalConstants {
    public static final int VIRTUAL_NODE_NUM = 8;
    public static final String REDIS_SRC_DATA_KEY = "src_data";
    public static final String REDIS_SRC_DATA_2_KEY = "src_data_2";
    public static final String REDIS_SRC_DATA_TIME_KEY = "src_data_time";
    public static final int REDIS_TIMEOUT = 60;
    public static final int MONITOR_PERIOD = 300;
    public static final String REDIS_MONITOR_TASK_LOCK = "redis_monitor_task_lock";
    public static final int MONITOR_LOCK_TIME = 60;
}