package com.halsign.vgate.thread;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.CreateVMTask;
import com.halsign.vgate.HalsignCallBack;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.DAO.CallBackParamDAO;
import com.halsign.vgate.DAO.CreateVMTaskDAO;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.util.Table;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.halsign.vgate.util.VmUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

public class CreateVMThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CreateVMThread.class);
	private HalsignCallBack callback;
	private String templateUUID;
	private CreateVMTask task;
	public CreateVMThread(HalsignCallBack callback, CreateVMTask task, String templateUUID) {
		this.callback = callback;
		this.task = task;
		this.templateUUID = templateUUID;
	}
	
	@Override
	public void run() {
		DAOManager daoManager = DAOManager.getInstance();
		Properties callBackParam = new Properties();
		
		int count = task.getCount();
		logger.info("There are " + count + " vms to be created on host " + task.getHost_UUID());
		VgateConnection con = null;
		try {
			Reader reader = ((CallBackParamDAO) daoManager.getDAO(
					Table.CALLBACKPARAM)).get(task.getCallBackId());
			callBackParam.load(reader);
			con = VgateConnectionPool.getInstance().connect(task.getHost_IP(), task.getHost_username(), task.getHost_password(), 600, 5);
			Host affinity = Host.getByUuid(con, task.getHost_UUID());
			logger.info("The template uuid is: " + templateUUID);
			VM template = VM.getByUuid(con, templateUUID);
			while (count > 0) {
				count--;
				try {
					logger.info("new vm name is: " + template.getNameLabel(con));
					VM newVm = template.createClone(con, template.getNameLabel(con) + "_VM_" + (task.getCount() - count));
					newVm.setIsATemplate(con, false);
					newVm.setAffinity(con, affinity);
					logger.info("set vm sysdisk and datadisk size to: " + task.getSys_disk_size() + " " + task.getData_disk_size());
					VmUtil.resizeVdis(con, newVm, this.task.getSys_disk_size(), this.task.getData_disk_size());
					logger.info("set vm cpu count and socket count to: " + task.getCpu_count() + " " + task.getSocket_count());
					VmUtil.setVCPU(con, newVm, task.getCpu_count(), task.getSocket_count());
					logger.info("set vm memory to: " + task.getVm_memory());
					VmUtil.changeMemory(con, newVm, task.getVm_memory() * VgateConstants.BINARY_MEGA);
					String vmUuid = newVm.getUuid(con);
					logger.info("The new created VM uuid is: " + vmUuid);
					//HalsignCache.getInstance().refreshVmCache(vmUuid);
					callback.onVmCreateSuccess(con, task.getHost_UUID(), callBackParam, newVm);
				} catch (XenAPIException e) {
					if (e.toString().startsWith("SR_BACKEND_FAILURE_44")) {
					callback.onVmCreateFail(con, task.getHost_UUID(),
							VgateMessageConstants.ERROR_CODE_SR_LOCAL_OUT_OF_SPACE,
							callBackParam);
					} else {
						callback.onVmCreateFail(con, task.getHost_UUID(),
								VgateMessageConstants.ERROR_CODE_VM_CREATE_FAILURE,
								callBackParam);
					}
					logger.error("", e);
				}
			}
			
		} catch (VgateException | XmlRpcException e1) {
			callback.onVmCreateFail(con, task.getHost_UUID(),
					VgateMessageConstants.ERROR_CODE_TEMPLATE_NOT_EXIST,
					callBackParam);
			logger.error("Create VM Failed", e1);
		} catch (SQLException e1) {
			logger.info("", e1);
		} catch (IOException e1) {
			logger.info("", e1);
		} finally {
			int status;
			if (count == 0) {
				status = TaskStatus.SUCCESS.ordinal();
			} else {
				status = TaskStatus.FAILED.ordinal();
			}
			try {
				((CreateVMTaskDAO) daoManager.getDAO(
						Table.CREATEVMTASK)).updateTaskCount(task.getTask_ID(),
						count, status);
			} catch (SQLException e) {
				logger.error("update createtask failed" + e);
			}
			VgateUtil.disconnect(con);
			daoManager.close();
		}
	}

}
