package com.halsign.vgate.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.halsign.vgate.PublishTask;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.spec.TemplateSpec;
import com.halsign.vgate.util.Encryptor;

public class PublishTaskDAO extends GenericDAO<PublishTask> {

	protected PublishTaskDAO(Connection con) {
		super(con);
	}

	public void insert(PublishTask task) throws SQLException {
		PreparedStatement stm = 
				this.con.prepareStatement("INSERT vgatedb.publishtask VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		String password = Encryptor.encrypt(task.getHost_password());
		stm.setString(1, task.getTask_ID());
		stm.setString(2, task.getHost_UUID());
		stm.setString(3, task.getHost_IP());
		stm.setString(4, task.getHost_username());
		stm.setString(5, password);
		stm.setInt(6, task.getTask_status().ordinal());
		stm.setString(7, task.getSource_UUID());
		stm.setString(8, task.getGold_UUID());
		stm.setInt(9, task.getRetry_count());
		stm.setString(10, task.getTemplate_system_vdi_tag());
		stm.setString(11, task.getTemplate_user_vdi_tag());
		stm.setString(12, task.getTemplate_vif_tag());
		
		stm.executeUpdate();
	}
	public List<PublishTask> query() throws SQLException {
		List<PublishTask> list = new ArrayList<PublishTask>();
		PreparedStatement stm = this.con.prepareStatement("SELECT * FROM vgatedb.publishtask where TASK_STATUS = 0");
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			PublishTask task = new PublishTask();
			task.setTask_ID(rs.getString("TASK_ID"));
			task.setHost_UUID(rs.getString("HOST_UUID"));
			task.setHost_IP(rs.getString("HOST_IP"));
			task.setHost_username(rs.getString("HOST_USERNAME"));
			task.setHost_password(Encryptor.decrypt(rs.getString("HOST_PASSWORD")));
			task.setTask_status(TaskStatus.values()[rs.getInt("TASK_STATUS")]);
			task.setSource_UUID(rs.getString("SOURCE_TEMPLATE_UUID"));
			task.setGold_UUID(rs.getString("GOLD_TEMPLATE_UUID"));
			task.setRetry_count(rs.getInt("RETRY_COUNT"));
			task.setTemplate_system_vdi_tag(rs.getString("TEMPLATE_SYSTEM_VDI_TAG"));
			task.setTemplate_user_vdi_tag(rs.getString("TEMPLATE_USER_VDI_TAG"));
			task.setTemplate_vif_tag(rs.getString("TEMPLATE_VIF_TAG"));
			list.add(task);
		}
		return list;
	}

	public int countTaskBySource(String UUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("SELECT COUNT(*) FROM vgatedb.publishtask WHERE SOURCE_TEMPLATE_UUID = ? AND (TASK_STATUS = 1 OR TASK_STATUS = 0)");
		stm.setString(1, UUID);
		ResultSet rs = stm.executeQuery();
		int count = 0;
		while (rs.next()) {
			count = rs.getInt(1);
		}
		
		return count;
	}
	
	public int countTaskDownloading(String hostUUID, String goldTemplateUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("SELECT COUNT(*) FROM vgatedb.publishtask WHERE HOST_UUID = ? AND GOLD_TEMPLATE_UUID = ? AND (TASK_STATUS = 1 OR TASK_STATUS = 0)");
		stm.setString(1, hostUUID);
		stm.setString(2, goldTemplateUUID);
		ResultSet rs = stm.executeQuery();
		int count = 0;
		while (rs.next()) {
			count = rs.getInt(1);
		}
		
		return count;
	}
	
	public int getPublishTaskStatus(String hostUUID, String templateUUID) throws SQLException {
		int status = -1;
		PreparedStatement stm = this.con.prepareStatement("SELECT TASK_STATUS FROM vgatedb.publishtask WHERE HOST_UUID = ? AND SOURCE_TEMPLATE_UUID = ?");
		stm.setString(1, hostUUID);
		stm.setString(2, templateUUID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			status = rs.getInt(1);
		}
		return status;
	}
	public int countTaskPublishFailed(String hostUUID, String goldTemplateUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("SELECT COUNT(*) FROM vgatedb.publishtask WHERE HOST_UUID = ? AND GOLD_TEMPLATE_UUID = ? AND TASK_STATUS = 2");
		stm.setString(1, hostUUID);
		stm.setString(2, goldTemplateUUID);
		ResultSet rs = stm.executeQuery();
		int count = 0;
		while (rs.next()) {
			count = rs.getInt(1);
		}
		
		return count;
	}
	
	public TemplateSpec getTemplateSpecByGoldTemplate(String templateUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("SELECT TEMPLATE_SYSTEM_VDI_TAG, TEMPLATE_USER_VDI_TAG, TEMPLATE_VIF_TAG FROM vgatedb.publishtask WHERE GOLD_TEMPLATE_UUID = ?");
		stm.setString(1, templateUUID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			TemplateSpec spec = new TemplateSpec();
			spec.setSystemVdiTag(rs.getString(1));
			spec.setUserVdiTag(rs.getString(2));
			spec.setVifTag(rs.getString(3));
			return spec;
		}
		return null;
	}
	
	public void updateTaskStatus(String taskId, int status) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET TASK_STATUS = ? where TASK_ID = ?");
		stm.setInt(1, status);
		stm.setString(2, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskStatusToRetry(String taskId) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET TASK_STATUS = ?, SOURCE_TEMPLATE_UUID = ? where TASK_ID = ?");
		stm.setInt(1, 0);
		stm.setString(2, "");
		stm.setString(3, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskSource(String taskId, String source) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET TASK_STATUS = 1, SOURCE_TEMPLATE_UUID = ? where TASK_ID = ?");
		stm.setString(1, source);
		stm.setString(2, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskByHostUUID(String hostUUID, String sourceUUID, int status) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET SOURCE_TEMPLATE_UUID = ?, TASK_STATUS = ? where HOST_UUID = ?");
		stm.setString(1, sourceUUID);
		stm.setInt(2, status);
		stm.setString(3, hostUUID);
		stm.executeUpdate();
	}
	
	public void updateTaskRetryCount(String taskId, int retryCount) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET RETRY_COUNT = ? WHERE TASK_ID = ?");
		stm.setInt(1, retryCount);
		stm.setString(2, taskId);
		stm.executeUpdate();
	}
	
	public void updateTaskStatus() throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("UPDATE vgatedb.publishtask SET TASK_STATUS = 0 where TASK_STATUS = 1");
		stm.executeUpdate();
	}
	
	public List<String> getHostWithDownloadbyGoldUUID(String goldUUID) throws SQLException {
		List<String> hosts = new ArrayList<String>();
		PreparedStatement stm = this.con.prepareStatement("SELECT HOST_UUID FROM vgatedb.publishtask WHERE GOLD_TEMPLATE_UUID = ? AND (TASK_STATUS = 1 OR TASK_STATUS = 0)");
		stm.setString(1, goldUUID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			hosts.add(rs.getString("HOST_UUID"));
		}
		return hosts;
	}
	
}