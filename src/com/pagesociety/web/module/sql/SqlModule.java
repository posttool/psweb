package com.pagesociety.web.module.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.WebModule;

public class SqlModule extends WebModule
{
	private static final String PARAM_DRIVER_NAME = "driver-name";
	private static final String PARAM_DRIVER_CLASS = "driver-class";
	private static final String PARAM_HOST = "host";
	private static final String PARAM_DATABASE = "database";
	private static final String PARAM_USERNAME = "username";
	private static final String PARAM_PASSWORD = "password";
	private String driver_name = "mysql";
	private String driver_class = "com.mysql.jdbc.Driver";
	private String host = "localhost";
	private String database;
	private String username;
	private String password;

	public void init(WebApplication app, Map<String, Object> config)
			throws InitializationException
	{
		super.init(app, config);
		if (GET_OPTIONAL_CONFIG_PARAM(PARAM_DRIVER_NAME, config) != null)
			driver_name = GET_OPTIONAL_CONFIG_PARAM(PARAM_DRIVER_NAME, config);
		if (GET_OPTIONAL_CONFIG_PARAM(PARAM_DRIVER_CLASS, config) != null)
			driver_class = GET_OPTIONAL_CONFIG_PARAM(PARAM_DRIVER_CLASS, config);
		if (GET_OPTIONAL_CONFIG_PARAM(PARAM_HOST, config) != null)
			host = GET_OPTIONAL_CONFIG_PARAM(PARAM_HOST, config);
		database = GET_REQUIRED_CONFIG_PARAM(PARAM_DATABASE, config);
		username = GET_REQUIRED_CONFIG_PARAM(PARAM_USERNAME, config);
		password = GET_REQUIRED_CONFIG_PARAM(PARAM_PASSWORD, config);
		Connection con = null;
		try
		{
			Class.forName(driver_class).newInstance();
			con = get_connection();
			if (!con.isClosed())
				INFO("Successfully connected to MySQL server ...");
		}
		catch (Exception e)
		{
			throw new InitializationException("SQLException", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				throw new InitializationException("SQLException (on close)", e);
			}
		}
	}

	private Connection get_connection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:" + driver_name + "://" + host + "/" + database, username, password);
	}

	public SqlResults executeStatement(String sql) throws WebApplicationException
	{
		SqlResults sql_results = new SqlResults();
		Connection con = null;
		try
		{
			con = get_connection();
			Statement stmt = con.createStatement();
			stmt.execute(sql);
			ResultSet rs = stmt.getResultSet();
			ResultSetMetaData rsmd = rs.getMetaData();
			int n = rsmd.getColumnCount();
			List<List<Object>> results = new ArrayList<List<Object>>();
			while (rs.next())
			{
				List<Object> res = new ArrayList<Object>();
				for (int i = 0; i < n; i++)
					res.add(rs.getObject(i + 1));
				results.add(res);
			}
			sql_results.results = results;
			sql_results.columns = n;
			sql_results.columnLabel = new String[n];
			for (int i = 0; i < n; i++)
				sql_results.columnLabel[i] = rsmd.getColumnLabel(i + 1);
			sql_results.columnType = new String[n];
			for (int i = 0; i < n; i++)
				sql_results.columnType[i] = rsmd.getColumnTypeName(i + 1);
		}
		catch (Exception e)
		{
			throw new WebApplicationException("SQLException", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				throw new WebApplicationException("SQLException (on close)", e);
			}
		}
		return sql_results;
	}

	public class SqlResults
	{
		public List<List<Object>> results;
		public int columns;
		public String[] columnLabel;
		public String[] columnType;
	}
}
