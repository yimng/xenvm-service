package com.halsign.vgate.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.halsign.vgate.CreateVMTask;
import com.halsign.vgate.Policy;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.util.Encryptor;

public class CreateVMTaskDAO extends GenericDAO<CreateVMTask> {
	protected CreateVMTaskDAO(Connection con) {
		super(con);
	}
	
	public void insert(CreateVMTask task) throws SQLException {
		PreparedStatement stm = 
				this.con.prepareStatement("INSERT vgatedb.createtask VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		stm.setString(1, task.getTask_ID());
		stm.setString(2, task.getGold_UUID());
		stm.setString(3, task.getHost_UUID());
		stm.setString(4, task.getHost_IP());
		stm.setString(5, task.getHost_username());
		stm.setString(6, Encryptor.encrypt(task.getHost_password()));
		stm.setInt(7, task.getTask_status().ordinal());
		stm.setInt(8, task.getCount());
		stm.setInt(9, task.getRetry_count());
		stm.setDouble(10, task.getVm_memory());
		stm.setDouble(11, task.getSys_disk_size());
		stm.setDouble(12, task.getData_disk_size());
		stm.setInt(13, task.getSocket_count());
		stm.setInt(14, task.getCpu_count());
		stm.setInt(15, task.getPolicy().ordinal());
		stm.setString(16, task.getCallBackId());
		stm.executeUpdate();
	}
	
	public List<CreateVMTask> query() throws SQLException {
		List<CreateVMTask> list = new ArrayList<CreateVMTask>();
		PreparedStatement stm = this.con.prepareStatement("SELECT * FROM vgatedb.createtask WHERE TASK_STATUS = 0 ");
		ResultSet rs = stm.executeQuery();
		while(rs.next()) {
			CreateVMTask task = new CreateVMTask();
			task.setTask_ID(rs.getString("TASK_ID"));
			task.setGold_UUID(rs.getString("GOLD_UUID"));
			task.setHost_UUID(rs.getString("HOST_UUID"));
			task.setHost_IP(rs.getString("HOST_IP"));
			task.setHost_username(rs.getString("HOST_USERNAME"));
			task.setHost_password(Encryptor.decrypt(rs.getString("HOST_PASSWORD")));
			task.setTask_status(TaskStatus.values()[rs.getInt("TASK_STATUS")]);
			task.setCount(rs.getInt("TASK_COUNT"));
			task.setRetry_count(rs.getInt("RETRY_COUNT"));
			task.setVm_memory(rs.getInt("VMMEMORY"));
			task.setSys_disk_size(rs.getInt("SYSDISKSIZE"));
			task.setData_disk_size(rs.getInt("DATADISKSIZE"));
			task.setSocket_count(rs.getInt("SOCKETCOUNT"));
			task.setCpu_count(rs.getInt("CPUCOUNT"));
			task.setPolicy(Policy.values()[rs.getInt("POLICY")]);
			task.setCallBackId(rs.getString("CALLBACKPARAM"));
			list.add(task);
		}
		return list;
	}
	
	public void updateTaskCount(String taskId, int count, int status) throws SQLException {
		PreparedStatement stm = 
				this.con.prepareStatement("UPDATE vgatedb.createtask SET TASK_COUNT = ?, TASK_STATUS = ? WHERE TASK_ID = ?");
		stm.setInt(1, count);
		stm.setInt(2, status);
		stm.setString(3, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskStatus(String taskId, int status) throws SQLException {
		PreparedStatement stm =
				this.con.prepareStatement("UPDATE vgatedb.createtask SET TASK_STATUS = ? WHERE TASK_ID = ?");
		stm.setInt(1, status);
		stm.setString(2, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskRetryCount(String taskUUID, int retryCount) throws SQLException {
		PreparedStatement stm =
				this.con.prepareStatement("UPDATE vgatedb.createtask SET RETRY_COUNT = ? WHERE TASK_ID = ?");
		stm.setInt(1, retryCount);
		stm.setString(2, taskUUID);
		stm.executeUpdate();
	}
}
