package com.youu.mysql.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import com.mysql.cj.conf.ConnectionUrl;
import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.jdbc.JdbcPropertySetImpl;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.h2.value.DataType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @Author Timmy
 * @Description
 * @Date 2021/6/24
 */
@Slf4j
public class H2MySQLJDBCTest {
    @Test
    public void test() throws SQLException, InterruptedException {
        Server server = Server.createTcpServer("-tcpPort", "9101", "-baseDir", "~/h2/tcp", "-ifNotExists");
        server.start();
        org.h2.Driver.load();
        try (Connection conn = DriverManager.getConnection(
            "jdbc:h2:tcp://localhost:9101/~/h2/tcp/d1;database_to_upper=false", "sa", "sa")) {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("show tables");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            System.out.println("=============");
            stat.execute("drop table if exists t1");
            stat.execute("drop table if exists T1");
            stat.execute("create table if not exists t1(id int,name varchar(16))");
            rs = stat.executeQuery("select database()");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            rs = stat.executeQuery("show tables");
            System.out.println("=============");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        server.shutdown();
    }

    @Test
    public void test1() {
        org.h2.Driver.load();
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/h2/test;database_to_upper=false", "sa", "sa")) {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("show tables");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            System.out.println("=============");
            //h2  create database is create schema
            stat.execute("create schema d2");
            stat.execute("drop table if exists t1");
            stat.execute("drop table if exists T1");
            stat.execute("create table if not exists t1(id int,name varchar(16))");
            rs = stat.executeQuery("select database()");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            rs = stat.executeQuery("show tables");
            System.out.println("=============");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    @Test
    public void test2() {
        org.h2.Driver.load();
        try (Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:d1;MODE=MYSQL;database_to_upper=false;INIT=CREATE SCHEMA IF NOT EXISTS d1", "sa", "sa")) {
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("show tables");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            System.out.println("=============");
            stat.execute("drop table if exists t1");
            stat.execute("drop table if exists T1");
            stat.execute("create table if not exists t1(id int,name varchar(16))");
            rs = stat.executeQuery("show databases");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
            rs = stat.executeQuery("show tables");
            System.out.println("=============");
            while (rs.next()) {
                System.out.println((rs.getString(1)));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    @Test
    public void test3() {
        org.h2.Driver.load();
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:d1;MODE=MYSQL;database_to_upper=false", "sa",
            "sa")) {
            Statement stat = conn.createStatement();
            stat.execute("CREATE TABLE `t1` (\n"
                + "  `id` bigint unsigned NOT NULL AUTO_INCREMENT,\n"
                + "  `s0` decimal(8,0) DEFAULT NULL,\n"
                + "  `s1` double DEFAULT NULL,\n"
                + "  `s2` text,\n"
                + "  `s3` blob,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8");
            stat.execute("insert into t1 values(1,null,null,null,null)");

            ResultSet resultSet = stat.executeQuery("select * from t1");

            System.out.println(resultSet);

            ResultSetMetaData data = resultSet.getMetaData();
            int columnCount = data.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                System.out.println(data.getColumnName(i));
                int columnType = data.getColumnType(i);
                System.out.println(columnType);
                System.out.println(data.getColumnTypeName(i));
                System.out.println("===============");
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    @Test
    public void test4() {
        org.h2.Driver.load();
        ArrayList<DataType> types = DataType.getTypes();
        System.out.println(types.size());
        for (DataType type : types) {
            log.info("{} {} {}", type.name, type.sqlType, type);
        }
    }

    @Test
    public void testJDBCUrl() {
        String s = "jdbc:mysql://localhost:33050/d1?useSSL=true";
        int ds = s.indexOf("/", 13);
        int de = s.indexOf("?", ds);
        System.out.println(ds);
        System.out.println(de);
        System.out.println(s.substring(ds, de));
        System.out.println(s.replaceFirst(s.substring(ds, de), "/" + "aaa"));

        s = "jdbc:mysql://localhost:33050/d1";
        ds = s.indexOf("/", 13);
        de = s.indexOf("?", ds);
        de = de > 0 ? de : s.length();
        System.out.println(ds);
        System.out.println(de);
        System.out.println(s.substring(ds, de));
        System.out.println(s.replaceFirst(s.substring(ds, de), "/" + "aaa"));

        s = "jdbc:mysql://localhost:33050?useSSL=true";
        ds = s.indexOf("/", 13);
        de = s.indexOf("?", ds);
        System.out.println(ds);
        System.out.println(de);
    }

    /**
     * 以 --default-authentication-plugin=caching_sha2_password 方式启动mysql(8.0后默认)
     *
     * @throws SQLException
     */
    @Ignore
    @Test
    public void testMySQL1() throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost:3306?useSSL=false";
        Properties info = new Properties();
        info.put("user", "root");
        info.put("password", "pass");
        ConnectionUrl conStr = ConnectionUrl.getConnectionUrlInstance(jdbcUrl, info);
        JdbcPropertySetImpl propertySet = new JdbcPropertySetImpl();
        propertySet.initializeProperties(info);

        JdbcConnection instance = ConnectionImpl.getInstance(conStr.getMainHost());
        Statement statement = instance.createStatement();
        ResultSet resultSet = statement.executeQuery("select 1");
        while (resultSet.next()) {
            System.out.println(resultSet.getInt(1));
        }
        resultSet.close();
        instance.close();

    }

    /**
     * 以 --default-authentication-plugin=mysql_native_password 方式启动mysql,但是创建一个caching_sha2_password的用户sha2
     *
     * @throws SQLException
     */
    @Ignore
    @Test
    public void testMySQL2() throws SQLException {
        String jdbcUrl
            = "jdbc:mysql://localhost:3306?useSSL=false";
        Properties info = new Properties();
        info.put("user", "sha2");
        info.put("password", "pass");
        ConnectionUrl conStr = ConnectionUrl.getConnectionUrlInstance(jdbcUrl, info);
        JdbcPropertySetImpl propertySet = new JdbcPropertySetImpl();
        propertySet.initializeProperties(info);

        JdbcConnection instance = ConnectionImpl.getInstance(conStr.getMainHost());
        Statement statement = instance.createStatement();
        ResultSet resultSet = statement.executeQuery("select 1");
        while (resultSet.next()) {
            System.out.println(resultSet.getInt(1));
        }
        resultSet.close();
        instance.close();

    }
}