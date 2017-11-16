package com.halsign.vgate.thread;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.HalsignCallBack;
import com.halsign.vgate.PublishTask;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.Template;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.DAO.PublishTaskDAO;
import com.halsign.vgate.util.Table;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.halsign.vgate.util.VmUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool.Record;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VdiType;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

public class PublishThread implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(PublishThread.class);
	public PublishThread(HalsignCallBack callBack, PublishTask task, Template source) {
		this.callBack = callBack;
		this.task = task;
		this.source = source;
	}
	private HalsignCallBack callBack;
	private PublishTask task;
	private Template source;
	
	@Override
	public void run() {
		VgateConnection src = null;
		VgateConnection dst = null;
		DAOManager daoManager = DAOManager.getInstance();
		int retryCount = task.getRetry_count();
		try {
			PublishTaskDAO publishDAO = ((PublishTaskDAO)daoManager.getDAO(Table.PUBLISHTASK));
			src = VgateConnectionPool.getInstance().connect(source.getHost_IP(), source.getHost_UserName(), source.getHost_Password(), 1800, 5);
			dst = VgateConnectionPool.getInstance().connect(task.getHost_IP(), task.getHost_username(), task.getHost_password(), 1800, 5);
			
			String vmUUID = source.getUUID();
			String srcHostUUID = source.getHost_UUID();
			String destHostUUID = task.getHost_UUID();
			String dataTag = task.getTemplate_user_vdi_tag();
			String systemTag = task.getTemplate_system_vdi_tag();
			String viftag = task.getTemplate_vif_tag();
			logger.info("VIF TAG IS: " + viftag);
			logger.info("publish template " + vmUUID + " from " + srcHostUUID + " to " + destHostUUID);
			Map<String, SR> srs = VgateUtil.matchSR(dst, destHostUUID, systemTag, dataTag);
			List<Network> netWorks = VgateUtil.getNetWorksByVifTag(dst, viftag, destHostUUID);
			logger.info("netWorks size: " + netWorks.size());
			if (srs.size() == 0) {
				publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.FAILED.ordinal());
				callBack.onTemplatePublishFail(dst, VgateMessageConstants.ERROR_CODE_SR_LOCAL_GET_FAILURE, source.getUUID(), task.getHost_UUID());
			} else if (!"".equals(viftag) && netWorks.size() != 1) {
				publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.FAILED.ordinal());
				callBack.onTemplatePublishFail(dst, VgateMessageConstants.ERROR_CODE_VIF_GET_FAILURE, source.getUUID(), task.getHost_UUID());
			} else {
				String uuid = copyTemplate(src, vmUUID, dst, destHostUUID, srs, netWorks);
				if (!"".equals(uuid)) {
					logger.info("The new published template uuid is: " + uuid);
					callBack.onTemplatePublishSuccess(dst, vmUUID, destHostUUID);
					// update the template table
					boolean hostSRShared = VgateUtil.isHostSRShared(dst, srs);
					VgateUtil.insertTemplate(uuid, source.getGold_UUID(),
							task.getHost_UUID(), task.getHost_IP(),
							task.getHost_username(), task.getHost_password(),
							hostSRShared);
					// update the task status
					publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.SUCCESS.ordinal());
				} else {
					logger.error("Can't get published template uuid");
					if (retryCount == 0) {
						logger.error("Copy template failed after retry!");
						publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.FAILED.ordinal());
						callBack.onTemplatePublishFail(dst, VgateMessageConstants.ERROR_CODE_TEMPLATE_COPY_FAILURE, source.getUUID(), task.getHost_UUID());
					} else {
						publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.PENDING.ordinal());
						publishDAO.updateTaskRetryCount(task.getTask_ID(), retryCount - 1);
						logger.info("reduce the retry count and retry to publish again.");
					}
				}
			}
		} catch (XenAPIException | XmlRpcException | VgateException e) {
			logger.error("", e);
			PublishTaskDAO publishDAO;
			try {
				publishDAO = ((PublishTaskDAO)daoManager.getDAO(Table.PUBLISHTASK));
				if (retryCount == 0) {
					logger.error("Copy template failed after retry!");
					publishDAO.updateTaskStatus(task.getTask_ID(), TaskStatus.FAILED.ordinal());
					callBack.onTemplatePublishFail(dst, VgateMessageConstants.ERROR_CODE_TEMPLATE_COPY_FAILURE, source.getUUID(), task.getHost_UUID());
				} else {
					publishDAO.updateTaskStatusToRetry(task.getTask_ID());
					publishDAO.updateTaskRetryCount(task.getTask_ID(), retryCount - 1);
					logger.info("reduce the retry count and retry to publish again.");
				}
			} catch (SQLException e2) {
				logger.error("Database error", e2);
			}
		} catch (SQLException e) {
			logger.error("Database error", e);
		} finally {
			VgateUtil.disconnect(src);
			VgateUtil.disconnect(dst);
			daoManager.close();
		}
	}
	
	private String copyTemplate(VgateConnection src, String vmUUID, VgateConnection dst, String hostUUID, Map<String, SR> srs, List<Network> networks) throws BadServerResponse, XenAPIException, XmlRpcException {
		String retUUID = "";
		VM vm = VM.getByUuid(src, vmUUID);
		Host host = Host.getByUuid(dst, hostUUID);
		for (Map.Entry<String, SR> entry : srs.entrySet()) {
			logger.info("vdi type: " + entry.getKey());
			logger.info("SR UUID: " + entry.getValue().getUuid(dst));
		}
		Host affinity = Host.getByUuid(dst, hostUUID);
		if (isSamePool(src, dst)) {
			logger.info("Same pool copy start");
			VM newVM = vm.copy(src, "VM_METADATA_ONLY", null);
			newVM.setAffinity(dst, affinity);
			try {
				VmUtil.copyVdiOfTemplate(src, vm, newVM, srs);
				if (networks.size() == 1) {
					VmUtil.createVIFOfTemplate(src, newVM, networks.get(0));
				}
			} catch (Exception e) {
				newVM.destroy(dst);
				throw e;
			}
			retUUID = newVM.getUuid(src);
			logger.info("Same pool copy finished");
		} else {
			logger.info("cross pool copy start");
			Network targetNetwork = null;
			Network network = host.getManagementIface(dst).getNetwork(dst);
			if (networks.size() == 1) {
				targetNetwork = networks.get(0);
			} else {
				targetNetwork = network;
			}
			logger.info("target network is: " + targetNetwork.getUuid(dst));
			Map<String, String> migrateReceive = host.migrateReceive(dst, network, new HashMap<String, String>());
			
			Map<VDI, SR> vdiMap = new HashMap<VDI, SR>();
			Map<VIF, Network> vifMap = new HashMap<VIF, Network>();
			Map<String, String> options = new HashMap<String, String>();
			options.put("copy", "true");
			Set<VBD> vbDs = vm.getVBDs(src);
			for (VBD vbd : vbDs) {
				if (!vbd.getEmpty(src) && vbd.getType(src) == Types.VbdType.DISK ) {
					VDI vdi = vbd.getVDI(src);
					if (vdi.getType(src) == VdiType.USER) {
						vdiMap.put(vdi, srs.get("usr") != null ? srs.get("usr") : srs.get("sys"));
					}
					if (vdi.getType(src) == VdiType.SYSTEM) {
						vdiMap.put(vdi, srs.get("sys") != null ? srs.get("sys") : srs.get("usr"));
					}
				}
			}
			Iterator<VIF> iterator = vm.getVIFs(src).iterator();
			if (iterator.hasNext()) {
				vifMap.put(iterator.next(), targetNetwork);
			}
			VM newVM = vm.migrateSend(src, migrateReceive, false, vdiMap, vifMap, options);
			newVM.setAffinity(dst, affinity);
			logger.info("Cross pool copy finished");
			Set<Network> nts = Network.getAll(dst);
			for (Network nw : nts) {
				logger.info("------------------------" + nw.getUuid(dst));
				Set<PIF> piFs = nw.getPIFs(dst);
				if (piFs.size() == 0) {
					try {
						nw.destroy(dst); 
					} catch (Exception e) {
					}
				}
			}
			
			retUUID = newVM.getUuid(dst);
		}
		return retUUID;
	}

	private boolean isSamePool(VgateConnection src, VgateConnection dst) throws BadServerResponse, XenAPIException, XmlRpcException {
		Record pool1 = VgateUtil.getPool(src);
		Record pool2 = VgateUtil.getPool(dst);
		
		return pool1.uuid.equals(pool2.uuid);
	}
}