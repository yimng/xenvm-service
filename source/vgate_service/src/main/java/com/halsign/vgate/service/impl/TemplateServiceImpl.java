package com.halsign.vgate.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.Template;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.DAO.PublishTaskDAO;
import com.halsign.vgate.DAO.TemplateDAO;
import com.halsign.vgate.cache.HalsignCache;
import com.halsign.vgate.cache.entity.HostCacheEntity;
import com.halsign.vgate.cache.entity.PoolCacheEntity;
import com.halsign.vgate.cache.entity.SREntity;
import com.halsign.vgate.service.TemplateService;
import com.halsign.vgate.spec.TemplateSpec;
import com.halsign.vgate.thread.Worker;
import com.halsign.vgate.util.Table;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.halsign.vgate.util.VmUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Pool.Record;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VdiType;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

public class TemplateServiceImpl implements TemplateService {

	final static Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

	private boolean checkTemplateSpec(VgateConnection con, List<Record> pools, TemplateSpec spec, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VBD> vbDs = vm.getVBDs(con);
		for (VBD vbd : vbDs) {
			if (VgateUtil.isValidVbd(con, vbd)) {
				VDI vdi = vbd.getVDI(con);
				if (vdi.getType(con) == VdiType.SYSTEM) {
					if (spec.getSystemVdiTag() == null || spec.getSystemVdiTag().equals("")) {
						//template has system vdi, but the spec does't specify the system tag
						logger.error("template has system vdi, but the spec does't specify the system tag");
						return false;
					}
				}
				if (vdi.getType(con) == VdiType.USER) {
					if (spec.getUserVdiTag() == null || spec.getUserVdiTag().equals("")) {
						//template has user vdi, but the spec does't specify the user tag
						logger.error("template has user vdi, but the spec does't specify the user tag");
						return false;
					}
				}
			}
		}
		
		HalsignCache cache = HalsignCache.getInstance();
		for (Record pool : pools) {
			PoolCacheEntity poolCacheEntity;
			List<String> hostUUIDs;
			try {
				poolCacheEntity = cache.getPoolCacheEntity(pool.uuid);
			} catch (ExecutionException e1) {
				e1.printStackTrace();
				return false;
			}
			
			hostUUIDs = poolCacheEntity.getHostUUIDs();
			for (String hostUUID : hostUUIDs) {
				HostCacheEntity entity;
				try {
					entity = cache.getHostCacheEntity(hostUUID);
				} catch (ExecutionException e) {
					e.printStackTrace();
					continue;
				}
				List<SREntity> sRs = entity.getSRs();
				if (spec.getSystemVdiTag() != null && !spec.getSystemVdiTag().equals("")) {
					SREntity sys = null;
					for (SREntity sr : sRs) {
						if (sr.getTags().contains(spec.getSystemVdiTag())) {
							sys = sr;
						}
					}
					if (sys == null) {
						//The spec specify the sys tag, but the host does't have the system sr
						logger.error("The spec specify the system vdi tag, but the SR on host " + entity.getIp() + " doesn't have the " + spec.getSystemVdiTag() + " tag");
						return false;
					}
				}
				if (spec.getUserVdiTag() != null && !spec.getUserVdiTag().equals("")) {
					SREntity usr = null;
					for (SREntity sr : sRs) {
						if (sr.getTags().contains(spec.getUserVdiTag())) {
							usr = sr;
						}
					}
					if (usr == null) {
						//The spec specify the user tag, but the host does't have the user sr
						logger.error("The spec specify the user vdi tag, but the SR on host " + entity.getIp() + " does't have the " + spec.getUserVdiTag() + " tag");
						return false;
					}
				}
			}
			
		}
		return true;
	}
	
	
	
		
	@Override
	public List<Template> listTemplate(VgateConnection connection) throws VgateException {
//		if(connection == null || (!VgateUtil.isConnectionValid(connection))) {
//			logger.error("[listTemplate()]The connection is invalid.");
//			throw new VgateException("The connection is invalid");
//		}
//		DAOManager daoManager = DAOManager.getInstance();
//		List<Template> list = new ArrayList<Template>();
//		try {
//			for(Template template : ((TemplateDAO) daoManager.getDAO(Table.TEMPLATE)).query()) {
//				if (VgateUtil.isTheTemplateExistInDbAndSr(connection, template.getGoldenTemplateUuid()))
//				{
//					list.add(template);
//				}
//			}
//			return list;
//		} catch (SQLException e) {
//			String message = "Failed to query the record due to "+e.toString();
//			VgateUtil.printErroLog(message, e);
//			
//			logger.error(message);
//			//e.printStackTrace();
//		}

		return null;
	}
	
	
//	public Template getTemplateByUuid(String templateUuid) {
//		Template template = null;
//		DAOManager daoManager = DAOManager.getInstance();
//		try {
//			TemplateDAO tDao = (TemplateDAO) daoManager.getDAO(Table.TEMPLATE);
//			List<Template> tList = tDao.getTemplateByUuid(templateUuid);
//			
//			if(tList != null && tList.size() != 0) {
//				template = tList.get(0);
//			}
//			
//		} catch (SQLException e) {
//			String message = "Failed to query the record due to "+e.toString();
//			VgateUtil.printErroLog(message, e);
//			
//			logger.error(message);
//			//e.printStackTrace();
//		}
//		return template;
//	}

	@Override
	public VgateMessage deleteTemplate(String templateUuid) throws VgateException {
		
		DAOManager daoManager = DAOManager.getInstance();
		try {
			List<Template> templates = ((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).findTemplatesByGoldTemplate(templateUuid);
			VgateConnection connection = null;
			for (Template template : templates) {
				try {
					connection = VgateConnectionPool.getInstance().connect(
							template.getHost_IP(), template.getHost_UserName(),
							template.getHost_Password(), 600, 5);
					VM localTemp = VM.getByUuid(connection, template.getUUID());
					VmUtil.deleteVMwithVDI(connection, localTemp);
				} catch (Exception e) {
					logger.error("", e);
				} finally {
					// disconnect
					if (connection != null) {
			            try{
			                Session.logout(connection);
			                connection.dispose();
			                connection = null;
			            } catch (Exception e ) {
			            	logger.error("", e);
			            }
			        }
				}
			}
		} catch (SQLException e1) {
			logger.error("", e1);
		}
		try {
			((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).deleteTemplateByGoldTempateUUID(templateUuid);
		} catch (SQLException e) {
			logger.error("", e);
		}
		return new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
		
	}
	
	/*
	private void deleteVdiForVM(VgateConnection connection, VM vm) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VBD> vbds = vm.getVBDs(connection);
		if(vbds == null || vbds.size() == 0) {
			logger.warn("vbd not exist. vm uuid = " + vm.getUuid(connection) + ".");
		} else {
			for(VBD vbd : vbds) {
				if (vbd.getEmpty(connection) || vbd.getType(connection) == com.xensource.xenapi.Types.VbdType.CD) {
					continue;
				}
				VDI vdi = vbd.getVDI(connection);
				if(vdi == null) {
					logger.warn("vdi not exist.");
				} else {
					logger.info("destroy vdi.");
					vdi.destroy(connection);
				}
			}
		}
	}*/

	@Override
	public VgateMessage insertTemplate(VgateConnection conn, String tmpUUID, String srUUID) throws VgateException {
		try {
			VM vm = VM.getByUuid(conn, tmpUUID);
			if (vm == null) {
				logger.error("[insertTemplate()]passed in vm does not exist.");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_MSG_VM_GET_FAILURE);
			}
			
			
			SR sr = SR.getByUuid(conn, srUUID);
			if (sr == null) {
				logger.error("[insertTemplate()]passed in sr does not exist.");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_CODE_SR_NFS_NOT_EXIST);
			}
			
//			Template goldenTemplate = TemplateUtil.generateTemplate(conn, vm, sr, new Date());
			logger.info("[TaskProcessThreadImpl.publishTemplateTask()]Insert record into table template. ");
//			((TemplateDAO) daomanager.getDAO(Table.TEMPLATE)).insert(goldenTemplate);
			
////			TemplateRelationship templateRelationship = TemplateUtil.generateTemplateRelationForPublishTm(conn, vm, goldenTemplate, copiedVmUuid);
////			logger.info("[TaskProcessThreadImpl.publishTemplateTask()]Insert record into table template_relationship. ");
////			((TemplateRelationshipDAO) daomanager.getDAO(Table.TEMPLATERELATIONSHIP)).insert(templateRelationship);
//			
		} catch (XenAPIException | XmlRpcException e) {
			logger.error("insertTemplate",e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
					VgateMessageConstants.ERROR_CODE_TEMPLATE_INSERT);
			
		} catch (Exception e) {
			String message = "[TemplateServiceImpl.insertTemplate():Exception]Failed to inert template due to " + e.toString();
			logger.error(message, e);
			
			throw new VgateException(message, e);
		}
		return new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
	}
	

	@Override
	public VgateTask publishTemplateAsync(List<VgateConnection> connections,
			String vmUuid, com.halsign.vgate.spec.TemplateSpec templateSpec) throws BadServerResponse,
			XenAPIException, XmlRpcException {
		HalsignCache cache = HalsignCache.getInstance();
		try {
			// initial cache
			cache.initializeCaches(connections);
		} catch (ExecutionException e) {
			logger.error("", e);
		}
		
		// find the vm's connection
		VgateConnection connection = null;
		VM vm = null;
		for (VgateConnection con : connections) {
			try {
				vm = VM.getByUuid(con, vmUuid);
				connection = con;
				break;
			} catch (Exception e) {
				//The vm is't in this connection, ignore this connection
				continue;
			}
		}
		if (vm == null || connection == null) { // can't get the vm from all connections
			logger.error("Can't get the vm from all connections");
			VgateTask task = new VgateTask();
			VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_VM_NOT_EXIST);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			task.setMsg(message);
			return task;
		}
		List<Record> pools = VgateUtil.getPools(connections);
		if (!checkTemplateSpec(connection, pools, templateSpec, vm)) {
			logger.error("Check the templateSpec failed!!");
			VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
			message.setErrorMsg("Check the templateSpec failed!!");
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			task.setMsg(message);
			return task;
		}
		
		
		Pool.Record poolRecord = VgateUtil.getPool(connection);
		Host master = poolRecord.master;
		String masterUUID = master.getUuid(connection);
		HostCacheEntity hostCacheEntity = null;
		try {
			hostCacheEntity = cache.getHostCacheEntity(masterUUID);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		Map<String, SR> srs = VgateUtil.matchSR(connection, masterUUID,
				templateSpec.getSystemVdiTag(), templateSpec.getUserVdiTag());
		VM tmp = vm.copy(connection, "VM_METADATA_ONLY", null);
		List<Network> netWorks = VgateUtil.getNetWorksByVifTag(connection, templateSpec.getVifTag(), masterUUID);
		if (netWorks.size() == 1) {
			VmUtil.createVIFOfTemplate(connection, tmp, netWorks.get(0));
		} else {
			logger.info("Get wrong network");
		}
		VmUtil.copyVdiOfTemplate(connection, vm, tmp, srs);
		if (!vm.getIsATemplate(connection)) {
			VgateUtil.convertVmToTemplate(connection, tmp);
		}
		String goldUUID = tmp.getUuid(connection);
		
		// insert first template
		try {
			boolean hostSRShared = VgateUtil.isHostSRShared(connection, srs);
			VgateUtil.insertTemplate(goldUUID, goldUUID, masterUUID,
					hostCacheEntity.getIp(), connection.getUsername(),
					connection.getPassword(), hostSRShared);
		} catch (SQLException e) {
			logger.error("", e);
		}
		
		// initial the task table
		for (VgateConnection con : connections) {
			Pool.Record poolRec = VgateUtil.getPool(con);
			Host m = poolRec.master;
			String poolUUID = poolRec.uuid;
			PoolCacheEntity poolCacheEntity;
			try {
				poolCacheEntity = cache.getPoolCacheEntity(poolUUID);
				if (isPoolSharedSR(poolCacheEntity, templateSpec)) {
					VgateUtil.initPublishTask(templateSpec, goldUUID, poolCacheEntity.getIP(), poolCacheEntity.getUsername(), poolCacheEntity.getPassword(), m.getUuid(con));
				} else {
					List<String> hostUUIDs = poolCacheEntity.getHostUUIDs();
					for (String hostUUID : hostUUIDs) {
						VgateUtil.initPublishTask(templateSpec, goldUUID, poolCacheEntity.getIP(), poolCacheEntity.getUsername(), poolCacheEntity.getPassword(), hostUUID);
					}
				}
				// update the host which publish the first template
				PublishTaskDAO taskDao = (PublishTaskDAO)DAOManager.getInstance().getDAO(Table.PUBLISHTASK);
				taskDao.updateTaskByHostUUID(hostCacheEntity.getHostUuid(), goldUUID, TaskStatus.SUCCESS.ordinal());
			} catch (ExecutionException e) {
				logger.error("", e);
			} catch (SQLException e) {
				logger.error("", e);
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		Worker.start = true;
		VgateTask task = new VgateTask();
		task.setResultVmUuid(goldUUID);
		task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
		return task;
	}

	private boolean isPoolSharedSR(PoolCacheEntity poolCacheEntity, TemplateSpec spec) throws ExecutionException {
		List<String> hostUUIDs = poolCacheEntity.getHostUUIDs();
		for (String hostUUID : hostUUIDs) {
			if (!isHostShareSR(hostUUID, spec)) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean isHostShareSR(String hostUUID, TemplateSpec spec) throws ExecutionException {
		HalsignCache cache = HalsignCache.getInstance();
		HostCacheEntity hostCacheEntity = cache.getHostCacheEntity(hostUUID);
		List<SREntity> sRs = hostCacheEntity.getSRs();
		for (SREntity sr : sRs) {
			Set<String> tags = sr.getTags();
			if (tags.contains(spec.getSystemVdiTag()) || tags.contains(spec.getUserVdiTag())) {
				if (!sr.isShared()) {
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public VgateTask updateTemplate(List<VgateConnection> connections,
			String goldTemplateUUID, TemplateSpec templateSpec) throws BadServerResponse,
			XenAPIException, XmlRpcException {
		HalsignCache cache = HalsignCache.getInstance();
		try {
			// initial cache
			cache.initializeCaches(connections);
		} catch (ExecutionException e) {
			logger.error("Initialize Cache failed", e);
		}
		Map<String, HostCacheEntity> hostsOfPools = null;
		try {
			hostsOfPools = cache.getHostsByPools(connections);
			logger.info("The hostOfPools size: " + hostsOfPools.size());
		} catch (ExecutionException e1) {
			logger.error("", e1);
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
			VgateMessage msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
			msg.setErrorMsg("failed to get hosts by connections");
			task.setMsg(msg);
			return task;
		}	
		
		try {
			TemplateDAO templateDao = (TemplateDAO)DAOManager.getInstance().getDAO(Table.TEMPLATE);
			List<Template> existTemplates = templateDao.findTemplatesByGoldTemplate(goldTemplateUUID);
			List<String> publishedHost = new ArrayList<String>();
			for (Template template : existTemplates) {
				publishedHost.add(template.getHost_UUID());
			}
			
			List<String> hostWithDownloading = ((PublishTaskDAO)DAOManager.getInstance().getDAO(Table.PUBLISHTASK)).getHostWithDownloadbyGoldUUID(goldTemplateUUID);
			//We should add the host which has downloading template, otherwise this host will has duplicate template.
			publishedHost.addAll(hostWithDownloading);
			
			Set<String> keySet = hostsOfPools.keySet();
			List<Template> temps = new ArrayList<Template>();
			for (Template temp : existTemplates) {
				if (keySet.contains(temp.getHost_UUID())) {
					temps.add(temp);
				}
			}
			logger.info("keySet:");
			logger.info(keySet.toString());
			logger.info("existTemplates");
			logger.info(existTemplates.toString());
			logger.info("temps");
			logger.info(temps.toString());
			if (temps.size() == 0) {
				logger.error("You can't delete all template");
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
				VgateTask task = new VgateTask();
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				task.setMsg(message);
				return task;
			} else {
				existTemplates.removeAll(temps);
				// first delete 
				for (Template template : existTemplates) {
					logger.info("Deleting template " + template.getUUID());
					// delete this template
					VgateConnection connection = null;
					try {
						String ip = template.getHost_IP();
						String username = template.getHost_UserName();
						String pwd = template.getHost_Password();
						String hostUUID = template.getHost_UUID();
						logger.info("Template host uuid: " + hostUUID);
						connection = VgateConnectionPool.getInstance().connect(ip, username, pwd, 600, 5);
						VM localTemp = VM.getByUuid(connection, template.getUUID());
						String localUUID = localTemp.getUuid(connection);
						int tasknum = ((PublishTaskDAO)DAOManager.getInstance().getDAO(Table.PUBLISHTASK)).countTaskBySource(localUUID);
						if (tasknum > 0) {
							logger.error("Delete template " + localUUID + " failed");
							VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
							VgateTask task = new VgateTask();
							task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
							task.setMsg(message);
							return task;
						}
						VmUtil.deleteVMwithVDI(connection, localTemp);
						templateDao.deleteTemplate(template.getUUID());
					} catch (Exception e) {
						logger.error("Delete template " + template.getUUID() + " failed", e);
						VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
						VgateTask task = new VgateTask();
						task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
						task.setMsg(message);
						return task;
					} finally {
						// disconnect
						VgateUtil.disconnect(connection);
					}
				}
			}
				
			// then publish
			for (VgateConnection con : connections) {
				Record poolRecord = VgateUtil.getPool(con);
				String poolUUID = poolRecord.uuid;
				PoolCacheEntity poolCached = cache.getPoolCacheEntity(poolUUID);
				if (isPoolSharedSR(poolCached, templateSpec)) {
					Host master = poolRecord.master;
					String masterUUID = master.getUuid(con);
					if (!publishedHost.contains(masterUUID)) {
						VgateUtil.initPublishTask(templateSpec, goldTemplateUUID, poolCached.getIP(), poolCached.getUsername(), poolCached.getPassword(), masterUUID);
					} else {
						// if the master has template, skip this pool
						logger.info("The master has been published local template：	" + masterUUID);
					}
				} else {
					List<String> hostUUIDs = poolCached.getHostUUIDs();
					for (String hostUUID : hostUUIDs) {
						if (!publishedHost.contains(hostUUID)) {
							VgateUtil.initPublishTask(templateSpec, goldTemplateUUID, poolCached.getIP(), poolCached.getUsername(), poolCached.getPassword(), hostUUID);
						} else {
							// if the host has template, skip this host
							logger.info("The host has been published with local template: " + hostUUID);
						}
					}
				}
			}
		} catch (ExecutionException e) {
			logger.error("", e);
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			DAOManager.getInstance().close();
		}
		
		Worker.start = true;
		
		VgateTask task = new VgateTask();
		task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
		return task;
	}
	
}
