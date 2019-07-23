package spittr.util;

import java.sql.*;

public class MysqlUtil {
    private Connection conn = null;
    PreparedStatement statement = null;

    // connect to MySQL
    public MysqlUtil() {
        String url = "jdbc:mysql://172.17.0.4:3306/web_practice?characterEncoding=UTF-8";
        String username = "root";
        String password = "1"; // 加载驱动程序以连接数据库
        try {
            Class.forName("com.mysql.jdbc.Driver" );
            conn = DriverManager.getConnection( url,username, password );
        }
        //捕获加载驱动程序异常
        catch ( ClassNotFoundException cnfex ) {
            System.err.println(
                    "装载 JDBC/ODBC 驱动程序失败。" );
            cnfex.printStackTrace();
        }
        //捕获连接数据库异常
        catch ( SQLException sqlex ) {
            System.err.println( "无法连接数据库" );
            sqlex.printStackTrace();
        }
    }

    // disconnect to MySQL
    public void deconnSQL() {
        try {
            if (conn != null)
                conn.close();
        } catch (Exception e) {
            System.out.println("关闭数据库问题 ：");
            e.printStackTrace();
        }
    }

    // execute selection language
    public ResultSet selectSQL(String sql) {
        ResultSet rs = null;
        try {
            statement = conn.prepareStatement(sql);
            rs = statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    // execute insertion language
    public boolean exec(String sql) {
        try {
            statement = conn.prepareStatement(sql);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("插入数据库时出错：" + sql);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("插入时出错：" + sql);
            e.printStackTrace();
        }
        return false;
    }
}
