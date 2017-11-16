package com.halsign.vgate.service.impl;

import java.io.Reader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.CreateVMTask;
import com.halsign.vgate.Policy;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.Template;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.DAO.CallBackParamDAO;
import com.halsign.vgate.DAO.CreateVMTaskDAO;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.DAO.TemplateDAO;
import com.halsign.vgate.cache.HalsignCache;
import com.halsign.vgate.cache.entity.HostCacheEntity;
import com.halsign.vgate.cache.entity.PoolCacheEntity;
import com.halsign.vgate.cache.entity.VmCacheEntity;
import com.halsign.vgate.service.VmService;
import com.halsign.vgate.spec.VmSpec;
import com.halsign.vgate.thread.Worker;
import com.halsign.vgate.util.Table;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.halsign.vgate.util.VmUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VbdType;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

/**
 * 虚拟机的后台服务。
 * 
 * @author 红山世纪
 *
 */
public class VmServiceImpl implements VmService {
	final static Logger logger = LoggerFactory.getLogger(VmServiceImpl.class);
	
	/**
	 * 还原虚拟机到模板的初始状态。
	 * 虚拟机内容会全部回滚，异步调用过程。
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param templateUuid 模板的uuid
	 * @return  还原虚拟机的任务
	 */
	@Override
		public VgateTask revertVmToTemplateAsync(VgateConnection connection,
			String vmUuid, String templateUuid) throws VgateException {
		DAOManager daomanager = DAOManager.getInstance();
		VgateMessage msg;
		VgateTask task = new VgateTask();
		try {

			if (connection == null
					|| (!VgateUtil.isConnectionValid(connection))) {
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_CONNECTION_INVALID);
				logger.error("The connection is invalid.");
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

			VM vm;
			try {
				vm = VM.getByUuid(connection, vmUuid);
			} catch (Exception e) {
				logger.error("The VM doesn't exist. uuid = " + vmUuid);
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_VM_NOT_EXIST);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}
			if (vm.getPowerState(connection) == VmPowerState.RUNNING) {
				logger.error("The VM is running");
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_VM_IS_RUNNING);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

			Host host = VgateUtil.getHostByVM(connection, vm);
			if (!host.getMetrics(connection).getLive(connection)) {
				msg = new VgateMessage(
						VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_HOST_NOT_ALIVE);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}
			
			Set<VBD> vm_vbds = vm.getVBDs(connection);
			logger.info("The VDI size is " + vm_vbds.size());
			int count = 0;
			if (vm_vbds != null && vm_vbds.size() > 0) {
				for (VBD vbd : vm_vbds) {
					if (vbd.getEmpty(connection)
							|| vbd.getType(connection) == VbdType.CD) {
						continue;
					}
					String vdi_uuid = vbd.getVDI(connection)
							.getUuid(connection);
					logger.info("vdi uuid is " + vdi_uuid + ".");
					Map<String, String> args = new HashMap<String, String>();
					args.put("vdi_uuid", vdi_uuid);
					logger.info("call plugin clear-vdi, function : clear_vdi_data.");
					host.callPlugin(connection,
							VgateConstants.PLUGIN_NAME_CLEAR_VDI,
							VgateConstants.PLUGIN_FUNCTION_CLEAR_VDI_DATA, args);
					logger.info("vdi(" + vdi_uuid + ")has been reverted.");
					count++;
				}
			}
			if (count == 0) {
				logger.warn("No VDI reverted");
				String message = "Failed to revert vm to template due to there is no VDI.";
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			}
			
		} catch (XenAPIException e) {
			
			String message = "Failed to revert vm to template due to "
					+ e.toString();
			logger.error(message, e);
			
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} catch (XmlRpcException e) {
			String message = "Failed to revert vm to template due to "
					+ e.toString();
			logger.error(message, e);
			
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} catch (Exception e) {
			String message = "Failed to revert vm to template due to "
					+ e.toString();
			logger.error(message, e);
			
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} finally {
			daomanager.close();
		}
		msg = new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
		task.setMsg(msg);
		task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
		return task;
	}

	/**
	 * 还原虚拟机的指定虚拟磁盘到模板的初始状态。
	 * 虚拟机的某一个虚拟磁盘回滚，异步调用过程。
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param templateUuid 模板的uuid
	 * @param vdiUuid 虚拟磁盘的uuid
	 * @return  还原虚拟机的任务
	 */
	@Override
	public VgateTask revertVmDiskToTemplateAsync(VgateConnection connection,
			String vmUuid, String templateUuid, String vdiUuid)
			throws VgateException {
		DAOManager daomanager = DAOManager.getInstance();
		VgateMessage msg;
		VgateTask task = new VgateTask();
		try {
			if (connection == null
					|| (!VgateUtil.isConnectionValid(connection))) {
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_CONNECTION_INVALID);
				logger.error("[revertVmDiskToTemplateAsync()]The connection is invalid.");
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

			VM vm;
			try {
				vm = VM.getByUuid(connection, vmUuid);
			} catch (Exception e) {
				logger.error("[revertVmDiskToTemplateAsync()]The VM doesn't exist. uuid = "
						+ vmUuid);
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_VM_NOT_EXIST);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

			if (vm.getPowerState(connection) == VmPowerState.RUNNING) {
				logger.error("[revertVmDiskToTemplateAsync()]The VM is running");
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_VM_IS_RUNNING);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}
			
			try {
				VDI.getByUuid(connection, vdiUuid);
			} catch (Exception e) {
				logger.error("[revertVmDiskToTemplateAsync()]The VDI doesn't exist. uuid = "
						+ vdiUuid);
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_TEMPLATE_NOT_EXIST);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

			
			Host host = VgateUtil.getHostByVM(connection, vm);
			if (!host.getMetrics(connection).getLive(connection)) {
				msg = new VgateMessage(
						VgateMessageConstants.STATUS_FAILURE,
						VgateMessageConstants.ERROR_CODE_HOST_NOT_ALIVE);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}
			
			Set<VBD> vm_vbds = vm.getVBDs(connection);
			logger.info("The VDI size is " + vm_vbds.size());
			int count = 0;
			if (vm_vbds != null && vm_vbds.size() > 0) {
				for (VBD vbd : vm_vbds) {
					if (vbd.getEmpty(connection)
							|| vbd.getType(connection) == VbdType.CD) {
						continue;
					}
					VDI vdi = vbd.getVDI(connection);
					if (vdi.getTags(connection).contains("AliurSysVDI")) {
						continue;// we will not revert the data vdi
					}
					String uuid = vdi.getUuid(connection);
					if (!vdiUuid.equals(uuid)) {
						continue;
					}
					logger.info("vdi uuid is " + uuid + ".");
					Map<String, String> args = new HashMap<String, String>();
					args.put("vdi_uuid", uuid);
					logger.info("call plugin clear-vdi, function : clear_vdi_data.");
					host.callPlugin(connection,
							VgateConstants.PLUGIN_NAME_CLEAR_VDI,
							VgateConstants.PLUGIN_FUNCTION_CLEAR_VDI_DATA, args);
					logger.info("vdi(" + uuid + ")has been reverted.");
					count++;
				}
			}
			if (count == 0) {
				logger.warn("No VDI reverted");
				String message = "Failed to revert vm to template due to there is no VDI.";
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			}
			if (count > 1) {
				logger.warn("More than one vdis reverted");
				String message = "Failed to revert vm to template due to more than one vdis reverted, it should be one.";
				msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
				task.setMsg(msg);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			}

		} catch (XenAPIException e) {
			String message = "Failed to revert vm disk due to " + e.toString();
			logger.error(message, e);
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} catch (XmlRpcException e) {
			String message = "Failed to revert vm disk due to " + e.toString();
			logger.error(message, e);
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} catch (Exception e) {
			String message = "Failed to revert vm disk due to " + e.toString();
			logger.error(message, e);
			msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, message);
			task.setMsg(msg);
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			throw new VgateException(message, e);
		} finally {
			daomanager.close();
		}
		msg = new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
		task.setMsg(msg);
		task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);

		return task;
	}

	/**
	 * 根据模板的uuid获取对应的虚拟机列表.
	 * 
	 * @param connection 连接
	 * @param templateUuid 模板的uuid
	 * 
	 * @return 虚拟机列表
	 */
	@Override
	public List<VM> listVmByTemplateUuid(VgateConnection connection,
			String templateUuid) throws VgateException {
		return null;
	}

	@Override
	public VgateTask createVmFromTemplateAsync(
			List<VgateConnection> connections, String templateUuid,
			VmSpec vmSpec, int numberOfCreate, Properties proper)
			throws VgateException, BadServerResponse, XenAPIException, XmlRpcException {
		HalsignCache cache = HalsignCache.getInstance();
		DAOManager daoManager = DAOManager.getInstance();
		try {
			cache.initializeCaches(connections);
		} catch (ExecutionException e1) {
			logger.error("initialize cache failed", e1);
		}
		
		String callBackID = UUID.randomUUID().toString();
		try {
			Reader reader = new StringReader(convertToString(proper));
			((CallBackParamDAO) daoManager.getDAO(
					Table.CALLBACKPARAM)).insert(callBackID, reader);
		} catch (SQLException e1) {
			logger.error("store the ali vm callback param failed", e1);
		} finally {
			daoManager.close();
		}
		
		logger.info("Gold Template UUID is: " + templateUuid);
		VM template = null;
		VgateConnection connection = null;
		long vmmemory = 0;
		List<Template> templates = new ArrayList<Template>();
		try {
			templates = ((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).findTemplatesByGoldTemplate(templateUuid);
		} catch (SQLException e2) {
			logger.info("", e2);
		} finally {
			daoManager.close();
		}
		if (templates.size() == 0) {
			// can't get template
			logger.error("Please publish template first");
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			return task;
		}
		
		for (Template tmp : templates) {
			try {
				String ip = tmp.getHost_IP();
				String username = tmp.getHost_UserName();
				String pwd = tmp.getHost_Password();
				connection = VgateConnectionPool.getInstance().connect(ip, username, pwd, 600, 5);
				template = VM.getByUuid(connection, tmp.getUUID());
				break;
			} catch (Exception e) {
				continue;
			} finally {
				VgateUtil.disconnect(connection);
			}
		}
		
		if (template != null) {
			if (vmSpec.getVmMemory() != 0) {
				long staticMax = vmSpec.getVmMemory() * VgateConstants.BINARY_MEGA;
				long vmOverHead = VmUtil.getVMOverHead(connection, template, staticMax);
				logger.info("vm overhead is: " + vmOverHead + "bytes");
				vmmemory = staticMax + vmOverHead;
			} else {
				vmmemory = template.getMemoryStaticMax(connection) + template.getMemoryOverhead(connection);
			}
		} else {
			// can't get template
			logger.error("Can't find the local template, please wait for a while to try again!!");
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			return task;
		}
		
		Map<String, Integer> hostvms = new HashMap<String, Integer>();
		Map<String, HostCacheEntity> hostsOfPools = null;
		try {
			hostsOfPools = cache.getHostsByPools(connections);
			logger.info("The hostOfPools size: " + hostsOfPools.size());
		} catch (ExecutionException e) {
			logger.error("Get the hosts by the pools failed", e);
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
			VgateMessage msg = new VgateMessage(VgateMessageConstants.STATUS_FAILURE);
			msg.setErrorMsg("failed to get hosts by connections");
			task.setMsg(msg);
			return task;
		}
		// remove the offline host
		Iterator<Entry<String, HostCacheEntity>> iterator = hostsOfPools.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, HostCacheEntity> next = iterator.next();
			HostCacheEntity hostCacheEntity = next.getValue();
			String ip = hostCacheEntity.getIp();
			if (!VgateUtil.isHostAlive(ip)) {
				iterator.remove();
			}
		}
		logger.info("After offline filter, the hostOfPools size: " + hostsOfPools.size());
		
		// initial a host==>vmcount map
		for (String hostUUID : hostsOfPools.keySet()) {
			hostvms.put(hostUUID, 0);
		}
		logger.info("Start calculate the host momory and the count of vm in cache");
		calculateHostMemoryAndVmCount(hostsOfPools);
		logger.info("Stop calculate the host momory and the count of vm in cache");
		
		logger.info("vmmemory: " + vmmemory);
		logger.info("Create VM Policy: " + vmSpec.getBalancePolicy().getPolicy());
	
		while (numberOfCreate > 0) {
			HostCacheEntity host = chooseHostEntity(hostsOfPools, vmSpec, vmmemory);
			if (host == null) {
				break;
			}
			int count = hostvms.get(host.getHostUuid());
			count++;
			hostvms.put(host.getHostUuid(), count);
			numberOfCreate--;
		}
		
		if (numberOfCreate > 0) {
			logger.info("There are " + numberOfCreate + " vm left which can't be created");
			VgateTask task = new VgateTask();
			task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
			return task;
		}
		
		for (Map.Entry<String, Integer> entry : hostvms.entrySet()) {
			int count = entry.getValue();
			if (count > 0) {
				String hostUUID = entry.getKey();
				HostCacheEntity hostCached = null;
				try {
					hostCached = cache.getHostCacheEntity(hostUUID);
				} catch (ExecutionException e) {
					logger.error("", e);
				}
				String ip = hostCached.getIp();
				PoolCacheEntity poolEntity = VgateUtil.getPoolEntityOfHost(hostUUID);
				logger.info("Initial a creat vm task with " + count + " VMs");
				initCreateTask(vmSpec, templateUuid, hostUUID, poolEntity, ip, count, callBackID);
			}
		}
		Worker.start = true;
		VgateTask task = new VgateTask();
		task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
		return task;
	}

	private void calculateHostMemoryAndVmCount(Map<String, HostCacheEntity> allhost) {
		HalsignCache cache = HalsignCache.getInstance();
		Map<String, VmCacheEntity> allVmEntity = cache.getAllVmEntity();
		for (VmCacheEntity vm : allVmEntity.values()) {
			String hostUUID = vm.getVmAffinity();
			if (allhost.containsKey(hostUUID)) {
				long vmmemory = vm.getMemory();
				HostCacheEntity host = allhost.get(hostUUID);
				long hostmemory = host.getMemory();
				host.setMemory(hostmemory - vmmemory);
				host.setVmCount(host.getVmCount() + 1);
			}
		}
	}
	
	private HostCacheEntity chooseHostEntity(Map<String, HostCacheEntity> allhost, VmSpec spec, long vmmemory) {
		if (spec.getBalancePolicy().getPolicy() == Policy.LEAST_VM_COUNT) {
			List<Entry<String, HostCacheEntity>> hostlist = new ArrayList<Entry<String, HostCacheEntity>>();
			for (Entry<String, HostCacheEntity> entry : allhost.entrySet()) {
				hostlist.add(entry);
			}
			
			Collections.sort(hostlist, new Comparator<Entry<String, HostCacheEntity>>() {
				@Override
				public int compare(Entry<String, HostCacheEntity> o1,
						Entry<String, HostCacheEntity> o2) {
					return o1.getValue().getVmCount() - o2.getValue().getVmCount();
				}
			});
			
			for (Entry<String, HostCacheEntity> entry : hostlist) {
				HostCacheEntity host = entry.getValue();
				logger.info("Choosed host is: " + host.getIp());
				logger.info("Choosed host has " + host.getVmCount() + " VM");
				logger.info("Choosed host memory left " + host.getMemory());
				return entry.getValue();
			}
			return null;
		} else if (spec.getBalancePolicy().getPolicy() == Policy.LEAST_VM_COUNT_WITH_MEMORY) {
			List<Entry<String, HostCacheEntity>> hostlist = new ArrayList<Entry<String, HostCacheEntity>>();
			for (Entry<String, HostCacheEntity> entry : allhost.entrySet()) {
				hostlist.add(entry);
			}
			// sort the hosts by the the number of vms on this host
			Collections.sort(hostlist, new Comparator<Entry<String, HostCacheEntity>>() {
				@Override
				public int compare(Entry<String, HostCacheEntity> o1,
						Entry<String, HostCacheEntity> o2) {
					return o1.getValue().getVmCount() - o2.getValue().getVmCount();
				}
			});
			
			for (Entry<String, HostCacheEntity> entry : hostlist) {
				HostCacheEntity host = entry.getValue();
				if (host.getMemory() > vmmemory) {
					logger.info("Choosed host is: " + host.getIp());
					logger.info("Choosed host has " + host.getVmCount() + " VM");
					logger.info("Choosed host memory left " + host.getMemory());
					host.setMemory(host.getMemory() - vmmemory);
					host.setVmCount(host.getVmCount() + 1);
					return host;
				}
			}
			return null;
			
		} else if (spec.getBalancePolicy().getPolicy() == Policy.MAX_HOST_DENSITY) {
			String hostuuid = "";
			HostCacheEntity host = new HostCacheEntity();
			host.setMemory(0);
			host.setHostUuid("invalid uuid");
			host.setIp("0.0.0.0");
			// find the max memory host
			// TODO find better method to get max memory host
			for (Map.Entry<String, HostCacheEntity> entry : allhost.entrySet()) {
				String key = entry.getKey();
				HostCacheEntity value = entry.getValue();
				logger.info("host ip: " + value.getIp());
				logger.info("host left memory: " + value.getMemory());
				if (value.getMemory() > host.getMemory()) {
					hostuuid = key;
					host = value;
				}
			}
			logger.debug("max memory host uuid is: " + hostuuid);
			if (host.getMemory() - vmmemory < 0) {
				logger.info("The host with max memory can't create vm, which means we can't find a host to create vm");
				logger.info("this host is :" + host.getIp());
				// The max memory host can't create vm, which means we can't find a host to create vm
				return null;
			}
			host.setMemory(host.getMemory() - vmmemory);
			allhost.put(hostuuid, host);
			return host;
		} else if (spec.getBalancePolicy().getPolicy() == Policy.SPECIFIED_HOST) {
			String uuid = spec.getBalancePolicy().getSpecifiedHost();
			logger.info("Choosed host is: " + uuid);
			if (allhost.containsKey(uuid)) {
				return allhost.get(uuid);
			} else {
				logger.error("Can't choose the host in cache");
			}
		}
		return null;
	}


	private void initCreateTask(VmSpec vmSpec, String goldUUID, String hostUUID,
			PoolCacheEntity poolCacheEntity, String ip, int taskCount, String callBackId) {
		try {
			CreateVMTask task = new CreateVMTask();
			task.setTask_ID(UUID.randomUUID().toString());
			task.setGold_UUID(goldUUID);
			task.setHost_UUID(hostUUID);
			task.setHost_IP(ip);
			task.setHost_username(poolCacheEntity.getUsername());
			task.setHost_password(poolCacheEntity.getPassword());
			task.setTask_status(TaskStatus.PENDING);
			task.setCount(taskCount);
			task.setRetry_count(3);
			task.setVm_memory(vmSpec.getVmMemory());
			task.setSys_disk_size(vmSpec.getSysDiskSize());
			task.setData_disk_size(vmSpec.getDataDiskSize());
			task.setSocket_count(vmSpec.getSocketCount());
			task.setCpu_count(vmSpec.getCpuCount());
			task.setPolicy(vmSpec.getBalancePolicy().getPolicy());
			task.setCallBackId(callBackId);
			((CreateVMTaskDAO) DAOManager.getInstance().getDAO(
					Table.CREATEVMTASK)).insert(task);
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			DAOManager.getInstance().close();
		}
	}
	
	private String convertToString(Properties p) {
		StringBuilder sb = new StringBuilder();
		for (Entry<Object, Object> entry : p.entrySet()) {
			sb.append(entry.getKey().toString());
			sb.append(":");
			sb.append(entry.getValue().toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}

