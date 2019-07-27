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

    // public static final int SIM_TYPE = 1;    // 基本队列
    // public static final int SIM_TYPE = 2;    // 安全队列
    // public static final int SIM_TYPE = 3;    // 基于键空间通知
     public static final int SIM_TYPE = 4;    // 基于一致性哈希

    public static final String REDIS_SERVER_SET = "server_set";
    public static final String REDIS_SERVER_PING_PREFIX = "server_ping_";
    public static final String REDIS_SERVER_ADD_SERVER_PREFIX = "add_server_";
    public static final String REDIS_SERVER_MIGRATE_SERVER_PREFIX = "migrate_server_";
    public static final String REDIS_LIST_DATA_PREFIX = "list_data_";
    public static final String REDIS_LIST_2_DATA_PREFIX = "list_data_2_";
    public static final String HASH_FORMAT = "%s&&VN%s";

}
