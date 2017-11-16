package com.halsign.vgate.DAO;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.util.Table;

public class DAOManager {
	private static DataSource src;
	private Connection con;
	private static Logger logger = LoggerFactory.getLogger(DAOManager.class);
	public static DAOManager getInstance() {
		return DAOManagerSingleton.INSTANCE.get();
	}

	public void open() throws SQLException {
		try {
			if (this.con == null || this.con.isClosed()) {
				this.con = src.getConnection();
			}
		} catch (SQLException e) {
			logger.error("Open database connection failed", e);
			throw e;
		}
	}

	public static void setDataSource(DataSource datasource) {
		src = datasource;
	}

	public void close() {
		try {
			if (this.con != null && !this.con.isClosed())
				this.con.close();
		} catch (SQLException e) {
			logger.error("Close database connection failed", e);
		}
	}

	private DAOManager() {}

	private static class DAOManagerSingleton {

		public static final ThreadLocal<DAOManager> INSTANCE;
		static {
			ThreadLocal<DAOManager> dm;
			try {
				dm = new ThreadLocal<DAOManager>() {
					@Override
					protected DAOManager initialValue() {
						try {
							return new DAOManager();
						} catch (Exception e) {
							logger.error("DAOManager init failed", e);
							return null;
						}
					}
				};
			} catch (Exception e) {
				logger.error("", e);
				dm = null;
			}
			INSTANCE = dm;
		}

	}
	
	public GenericDAO<?> getDAO(Table t) throws SQLException {
		if (this.con == null || this.con.isClosed())
			this.open();

		switch (t) {
		case TEMPLATE:
			return new TemplateDAO(this.con);
		case PUBLISHTASK:
			return new PublishTaskDAO(this.con);
		case CREATEVMTASK:
			return new CreateVMTaskDAO(this.con);
		case CALLBACKPARAM:
			return new CallBackParamDAO(this.con);
		default:
			throw new SQLException("Trying to link to an unexistant table.");
		}

	}
}
