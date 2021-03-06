package com.less.aspider.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class DBHelper {

	private static String sDBName = "aspider.db";
	private static Connection connection = null;
	private static PreparedStatement psStatement = null;
	private static ResultSet resultSet = null;
	private static String sqliteDriver = "org.sqlite.JDBC";
	private static String mysqlDriver = "com.mysql.jdbc.Driver";
	private static String sqliteUrl = "jdbc:sqlite:" + sDBName;
	private static String mysqlUrl = "jdbc:mysql://localhost:3306/" + sDBName;

	public static final int TYPE_MYSQL = 0x001;
	public static final int TYPE_SQLITE = 0x002;
	private static int sType = TYPE_SQLITE;

	public static void setType(int type) {
		sType = type;
	}

	public static void setDBName(String dbName) {
		sDBName = dbName;
		sqliteUrl = "jdbc:sqlite:" + sDBName;
		mysqlUrl = "jdbc:mysql://localhost:3306/" + sDBName;
	}

	static {
		try {
			if (sType == TYPE_MYSQL) {
				Class.forName(mysqlDriver);
			} else if(sType == TYPE_SQLITE){
				Class.forName(sqliteDriver);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** 获取连接 */
	private static Connection getConnection(){
		try {
			Connection connection = null;
			// sqlite不需要用户名密码 ,没有test.db 则会自动创建文件
			if (sType == TYPE_SQLITE) {
				connection = DriverManager.getConnection(sqliteUrl);
			} else if(sType == TYPE_MYSQL) {
				connection = DriverManager.getConnection(mysqlUrl, "root","root");
			}
			return connection;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 关闭资源 */
	private static void close(){
		try {
			if(resultSet != null) {
				resultSet.close();
			}
			if(psStatement != null) {
				psStatement.close();
			}
			if(connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** rawSQL(可读 ->仅支持查询) */
	public static List<Map<String,String>> rawSQLMapList(String sql,String ... params){
		connection = getConnection();
		ArrayList<Map<String,String>> list = new ArrayList<>();
		try {
			psStatement = connection.prepareStatement(sql);
			if(params != null){
				for(int i = 0;i < params.length;i++){
					psStatement.setObject(i+1, params[i]);
				}
			}
			resultSet = psStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int column = resultSetMetaData.getColumnCount();// 数据库的字段列数

			Map<String,String> map = null;// 每一个map就是一个对象(记录)
			while(resultSet.next()){
				map = new HashMap<>();
				for(int i =  1;i <= column;i++){
					// String fieldClass = resultSetMetaData.getColumnClassName(i);
					// if(fieldClass.equals("java.lang.String")) resultSet.getString(i);
					String fieldName = resultSetMetaData.getColumnName(i).toLowerCase();
					map.put(fieldName, resultSet.getString(i));
				}

				list.add(map);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/** rawSQL(可读 ->仅支持查询) */
	public static List<Object[]> rawSQLObjsList(String sql,String ... params){
		connection = getConnection();
		List<Object[]> list = new ArrayList<>();
		try {
			psStatement = connection.prepareStatement(sql);
			if(params != null){
				for(int i = 0;i < params.length;i++){
					psStatement.setString(i+1, params[i]);
				}
			}
			resultSet = psStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int column = resultSetMetaData.getColumnCount();// 数据库的字段列数

			while(resultSet.next()){
				Object [] obj = new Object[column];// 每个数组表示一条数据库记录,里面存放的是字段数组
				for(int i = 0;i < column;i++){
					obj[i] = resultSet.getObject(i+1);
				}
				list.add(obj);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			close();
		}
		return list;
	}

	/** execSQL(可写 ->:支持 create,insert,update,delete,drop等数据库更新相关的操作 ) */
	public static boolean execSQL(String sql,String ... params){
		boolean flag = false;
		connection = getConnection();
		try {
			psStatement = connection.prepareStatement(sql);

			if(params != null){
				for(int i = 0;i < params.length;i++){
					psStatement.setString(i+1, params[i]);
				}
			}
			//
			if(psStatement.executeUpdate() == 1){
				flag = true;
			}

		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			close();
		}
		return flag;
	}

	/** execSQL(可写 ->:支持 create,insert,update,delete,drop等数据库更新相关的操作 ) 多条SQL需要事务操作。 */
	public static boolean execSQLAll(String [] sqls,String [][] params) throws SQLException{
		boolean flag = false;
		connection = getConnection();
		connection.setAutoCommit(false);// 事务开始 相当于 beginTransation();

		try {
			for(int i = 0;i < sqls.length;i++){
				if(params[i] != null){
					psStatement = connection.prepareStatement(sqls[i]);

					for(int j = 0;j < params[i].length;j++){
						psStatement.setString(j + 1, params[i][j]);
					}
					psStatement.executeUpdate();

				}
			}

			connection.commit();
			flag = true;

		}catch (Exception e) {
			connection.rollback();// 事务回滚
			e.printStackTrace();
		}finally {
			close();
		}
		return flag;
	}
}