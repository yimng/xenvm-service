package com.halsign.vgate.util;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.PublishTask;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.Template;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.DAO.PublishTaskDAO;
import com.halsign.vgate.DAO.TemplateDAO;
import com.halsign.vgate.cache.HalsignCache;
import com.halsign.vgate.cache.entity.PoolCacheEntity;
import com.halsign.vgate.cache.entity.SREntity.SRTYPE;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Pool.Record;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VbdType;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;


public class VgateUtil {
	private static final Logger logger = LoggerFactory.getLogger(VgateUtil.class);

	public static boolean isSrSsdType(VgateConnection connection, SR sr) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		boolean flag = false;
		if(sr != null) {
			Map<String, String> map = sr.getOtherConfig(connection);
			if(map != null && map.size() > 0) {
				String isSsd = map.get(VgateConstants.SR_TYPE_SSD);
				if("true".equalsIgnoreCase(isSsd)) {
					flag = true;
				}
			}
		}
		
		return flag;
	}
	
	
	public static Host getHostByVM(VgateConnection con, VM vm) throws BadServerResponse, XenAPIException,
			XmlRpcException {
		if (vm.getPowerState(con).equals(Types.VmPowerState.RUNNING)) {
			return vm.getResidentOn(con);
		}
		Host storageHost = getVMStorageHost(con, vm);
		if (storageHost == null) {
			return getMasterHostFromPool(con);
		}

		return storageHost;
	}

	public static  Host getVMStorageHost(VgateConnection con, VM vm) throws BadServerResponse,
			XenAPIException, XmlRpcException {
		for (VBD vbd : vm.getVBDs(con)) {
			VDI vdi = vbd.getVDI(con);
			if (vdi.isNull() || !(!vdi.getMissing(con) && vdi.getManaged(con))) {
				continue;
			}
			SR sr = vdi.getSR(con);
			if (sr.isNull()) {
				continue;
			}
			if (sr.getShared(con) || isSrSsdType(con, sr) || sr.getPBDs(con).size() != 1) {
				continue;
			}
			PBD pbd = (PBD) sr.getPBDs(con).toArray()[0];
			if (pbd.isNull()) {
				continue;
			}
			return pbd.getHost(con);
		}
		return null;
	}

	public static void convertVmToTemplate(VgateConnection connection, VM vm) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		if(vm != null) {
			vm.setIsATemplate(connection, true);
			Map<String, String> otherConfigMap = vm.getOtherConfig(connection);
			if(otherConfigMap != null && otherConfigMap.size() > 0) {
				String instantValue = otherConfigMap.get("instant");
				if(instantValue == null) {
					vm.addToOtherConfig(connection, "instant", "true");
				} else if (!"true".equalsIgnoreCase(instantValue)) {
					otherConfigMap.put("instant", "true");
					vm.setOtherConfig(connection, otherConfigMap);
				}
			}
		}
	}

    
    public static boolean isValidVbd(VgateConnection connection, VBD vbd) 
    		throws BadServerResponse, XenAPIException, XmlRpcException {
    	assert vbd != null;
    	boolean flag = false;
    	boolean isEmpty = vbd.getEmpty(connection);
    	VbdType type = vbd.getType(connection);
    	if(!isEmpty && (Types.VbdType.CD != type)) {
    		flag = true;
    	}
    	return flag;
    }

	public static Host getMasterHostFromPool(VgateConnection connection) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		Host host = null;
		Record pool = getPool(connection);
		if(pool != null) {
			host = pool.master;
		}
		return host;
	}
	
	
	public static boolean isConnectionValid(VgateConnection con) {
		String ref = con.getSessionReference();
		if (ref == null) {
			return false;
		}
		
		return true;
	}
	
    public static void disconnect(VgateConnection connection) 
    {
    	logger.debug("disconnect...");
        if (connection != null) {
            try{
                Session.logout(connection);
            } catch (Exception e ) {
            }
            connection.dispose();
            connection = null;
        }
    }
    
    public static String getVgateVersion(VgateConnection connection, Host host) throws BadServerResponse, XenAPIException, XmlRpcException {
    	String vgateVersion = null;
    	if(connection != null && host != null) {
    		Map<String, String> softwareVersionMap = host.getSoftwareVersion(connection);
    		if(softwareVersionMap != null) {
    			String productVersion = softwareVersionMap.get("product_version");
    			String release_number = softwareVersionMap.get("release_number");
    			vgateVersion = productVersion + "-" + release_number;
    		}
    		
    	}
    	
    	return vgateVersion;
    }
    
    public static boolean isSpeedUp(SR.Record srRecord) {
		boolean flag = false;
		if(srRecord != null) {
			Map<String, String> map = srRecord.otherConfig;
			if(map != null && map.size() > 0) {
				String isSsd = map.get(VgateConstants.SR_TYPE_SSD);
				if("true".equalsIgnoreCase(isSsd)) {
					flag = true;
				}
			}
		}
		
		return flag;
	}
    
    public static Map<String, SR> matchSR(VgateConnection con, String hostUUID, String systemTag, String userTag) throws BadServerResponse, XenAPIException, XmlRpcException {
    	Map<String, SR> srs = new HashMap<String, SR>();
    	List<SR> sRsOfHost = getSRsOfHost(con, hostUUID);
    	logger.info("SRsOfHost size: " + sRsOfHost.size());
    	for (SR sr : sRsOfHost) {
    		if(sr.getTags(con).contains(systemTag)) {
    			srs.put("sys", sr);
    		}
    		if(sr.getTags(con).contains(userTag)) {
    			srs.put("usr", sr);
    		}
    	}
    	logger.info("mathed SR" + srs);
		return srs;
    }
    
    public static List<SR> getSRsOfHost(VgateConnection con, String hostUUID) throws BadServerResponse, XenAPIException, XmlRpcException {
    	logger.info("getSRsOfHost start");
    	List<SR> ret = new ArrayList<SR>();
    	Set<SR> srs = SR.getAll(con);
    	
    	for (SR sr : srs) {
    		com.xensource.xenapi.SR.Record srRec = sr.getRecord(con);
			if (VgateUtil.isSpeedUp(srRec)) {
				continue;
			}
			String type = srRec.type;
			if (!(SRTYPE.EXT.toString().equalsIgnoreCase(type) ||
					SRTYPE.LVMOHBA.toString().equalsIgnoreCase(type)||
					SRTYPE.NFS.toString().equalsIgnoreCase(type)||
					SRTYPE.LVMOISCSI.toString().equalsIgnoreCase(type)||
					SRTYPE.LVM.toString().equalsIgnoreCase(type)||
					SRTYPE.LVMOBOND.toString().equalsIgnoreCase(type)||
					SRTYPE.LVMOFC.toString().equalsIgnoreCase(type)||
					SRTYPE.RAWHBA.toString().equalsIgnoreCase(type))) {
				continue;
			}
			for (PBD pbd : srRec.PBDs) {
				PBD.Record pbdRecord = pbd.getRecord(con);
				String uuid = pbdRecord.host.getUuid(con);
				if (uuid.equals(hostUUID)) {
					ret.add(sr);
					break;
				}
			}
		}
    	logger.info("getSRsOfHost stop");
    	return ret;
    }
    
	public static Record getPool(VgateConnection connection)
			throws BadServerResponse, XenAPIException, XmlRpcException {
		Map<Pool, com.xensource.xenapi.Pool.Record> poolRecords = Pool
				.getAllRecords(connection);
		for (com.xensource.xenapi.Pool.Record pool : poolRecords.values()) {
			return pool;
		}
		return null;
	}
	
	public static boolean isSRHasEnoughSpace(VgateConnection connection, VM template, SR sr) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		boolean isHasEnoughSpace = true;
		if(template != null && sr != null) {

			long templateSpaceSize = getTemplateSpace(connection, template);
			long srFreeSize = sr.getPhysicalSize(connection) - sr.getPhysicalUtilisation(connection);
			
			logger.debug("temlate space size -----------------> " + templateSpaceSize);
			logger.debug("SR free space size -----------------> " + srFreeSize);
			
			if(srFreeSize < templateSpaceSize + VgateConstants.DISKBUFFER) {
				logger.debug("This SR has not enough space.");
				return !isHasEnoughSpace;
			} 
			logger.debug("This SR has enough space.");
		}
		return isHasEnoughSpace;
	}
	
	public static long getTemplateSpace(VgateConnection connection, VM template) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		long templateSpace = 0l;
		if(template != null) {
			List<VDI> vdiList = getVdiListByVM(connection, template);
			if(vdiList != null && vdiList.size() > 0) {
				for(VDI vdi : vdiList) {
					long pVdiSize = 0l;
					List<VDI> parenrtVdi = getParentVdi(connection, vdi);
					if(parenrtVdi != null && parenrtVdi.size() != 0) {
						for(VDI pVdi : parenrtVdi) {
							pVdiSize += getVDISize(connection, template, pVdi);
						}
					}
					templateSpace += (getVDISize(connection, template, vdi) + pVdiSize);
				}
			}
		}
		logger.info("template size ---------- " + templateSpace);
		return templateSpace;
	}
	
	public static List<VDI> getVdiListByVM(VgateConnection connection, VM vm) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		List<VDI> vdiList = null;
		if(vm != null) {
			Set<VBD> vbdList = vm.getVBDs(connection);
			if(vbdList != null && vbdList.size() > 0) {
				vdiList = new ArrayList<VDI>();
				Iterator<VBD> vbdIterator = vbdList.iterator();
				while(vbdIterator.hasNext()) {
					VBD vbd = (VBD)vbdIterator.next();
					if(isValidVbd(connection, vbd)) {
						VDI vdi = vbd.getVDI(connection);
						//System.out.println("getVdiListByVM----------->"+vdi.getUuid(connection));
						if(!vdi.isNull()) {
							vdiList.add(vdi);
						}
					}
				}
				
			}
		}
		
		return vdiList;
	}
	
	public static List<VDI> getParentVdi(VgateConnection connection, VDI vdi) 
			throws BadServerResponse, XenAPIException, XmlRpcException {
		//System.out.println("-------------------------->"+vdi.getUuid(connection));
		List<VDI> parentVdi = new ArrayList<VDI>();
		String pVdiUuid = vdi.getSmConfig(connection).get("vhd-parent");
		VDI pVdi = null;
		try{
			pVdi = VDI.getByUuid(connection, pVdiUuid);
		} catch(Exception e) {
		}
		//System.out.println("getParentVdi------outer------------------>"+pVdi.getUuid(connection));
		while(pVdi != null && !pVdi.isNull()) {
			parentVdi.add(pVdi);
			try{
				pVdiUuid = pVdi.getSmConfig(connection).get("vhd-parent");
				pVdi = VDI.getByUuid(connection, pVdiUuid);
				//System.out.println("getParentVdi------inner--------------->"+pVdi.getUuid(connection));
			} catch(Exception e) {
				pVdi = null;
			}
		}
		
		return parentVdi;
	}
	
	public static long getVDISize(VgateConnection con, VM vm, VDI vdi) {
		Host host;
		try {
			host = VgateUtil.getHostByVM(con, vm);
			Map<String, String> args = new HashMap<String, String>();
			args.put("vdi_uuid", vdi.getUuid(con));
			String size = host.callPlugin(con,
					VgateConstants.PLUGIN_NAME_GET_VDI_SIZE,
					VgateConstants.PLUGIN_FUNCTION_GET_VDI_SIZE, args);
			return Long.parseLong(size);
		} catch (BadServerResponse e) {
			logger.error("getVDISize", e);
		} catch (XenAPIException e) {
			logger.error("getVDISize", e);
		} catch (XmlRpcException e) {
			logger.error("getVDISize", e);
		}
		return 0;
	}
	
	public static String findTemplate(String hostUuid, String goldUUID) {
		String templateUUID = null;
		DAOManager daoManager = DAOManager.getInstance();
		try {
			templateUUID = ((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).findTemplateByHostUUID(hostUuid, goldUUID);
		} catch (SQLException e) {
			logger.error("Find local template failed", e);
		}
		if (templateUUID == null) {
			try {
				List<Template> findTemplatesByGoldTemplate = ((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).findTemplatesByGoldTemplate(goldUUID);
				for (Template template : findTemplatesByGoldTemplate) {
					if (template.isShared()) {
						return template.getUUID();
					}
				}
			} catch (SQLException e) {
				logger.error("Find shared template failed", e);
			}
		}
		
		return templateUUID;
	}
	
	public static boolean isHostAlive(String ip) {
		boolean result = true;
		try {
			// if the host is live, we will ping the host
			InetAddress inet = InetAddress.getByName(ip);
			boolean reachable;
			if (isWindows()) {
				Process p1 = java.lang.Runtime.getRuntime().exec("ping -n 1 " + ip);
				int returnVal;
				try {
					returnVal = p1.waitFor();
					reachable = (returnVal==0);
				} catch (InterruptedException e) {
					reachable = false;
				}
			} else {
				reachable = inet.isReachable(1000);
			}
			if (!reachable) { // This host is not online
				result = false;
			} else {
				result = true;
			}
		} catch (IOException e) {
			result = false;
		}
		return result;
	}
	
	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
	}
	
	public static List<Record> getPools(List<VgateConnection> connections) throws BadServerResponse, XenAPIException, XmlRpcException {
		List<Record> pools = new ArrayList<Record>();
		for (VgateConnection con : connections) {
			Record pool = getPool(con);
			pools.add(pool);
		}
		return pools;
	}
	
	public static void initPublishTask(
			com.halsign.vgate.spec.TemplateSpec templateSpec, String goldUUID,
			String ip, String username, String password, String hostUUID) throws SQLException {
		PublishTask task = new PublishTask();
		task.setTask_ID(UUID.randomUUID().toString());
		task.setHost_UUID(hostUUID);
		task.setHost_IP(ip);
		task.setHost_username(username);
		task.setHost_password(password);
		task.setTask_status(TaskStatus.PENDING);
		task.setGold_UUID(goldUUID);
		task.setSource_UUID("");
		task.setRetry_count(3);
		task.setTemplate_system_vdi_tag(templateSpec.getSystemVdiTag());
		task.setTemplate_user_vdi_tag(templateSpec.getUserVdiTag());
		task.setTemplate_vif_tag(templateSpec.getVifTag());
		((PublishTaskDAO)DAOManager.getInstance().getDAO(Table.PUBLISHTASK)).insert(task);
	}
	
	public static PoolCacheEntity getPoolEntityOfHost(String hostUUID) {
		HalsignCache cache = HalsignCache.getInstance();
		Map<String, PoolCacheEntity> allPoolEntity = cache.getAllPoolEntity();
		for (PoolCacheEntity pool : allPoolEntity.values()) {
			if (pool.getHostUUIDs().contains(hostUUID)) {
				return pool;
			}
		}
		return null;
	}
	
	public static int getDeviceId(VgateConnection con, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		int i = 0;
		while (true) {
			boolean found = false;
			Set<VIF> viFs = vm.getVIFs(con);
			for(VIF vif : viFs) {
				if (vif != null) {
					if (vif.getDevice(con).equals(i)) {
						found = true;
						break;
					}
				} else {
					found = true;
				}
			}
			if (!found) {
				break;
			}
			i++;
		}
		
		return i;
	}
	
	public static List<Network> getNetWorksByVifTag(VgateConnection con, String vifTag, String hostUUID) throws BadServerResponse, XenAPIException, XmlRpcException {
		List<Network> netWorks = new ArrayList<Network>();
		Host host = Host.getByUuid(con, hostUUID);
		Set<PIF> piFs = host.getPIFs(con);
		for (PIF pif : piFs) {
			Network network = pif.getNetwork(con);
			Set<String> tags = network.getTags(con);
			if (tags.contains(vifTag)) {
				netWorks.add(network);
			}
		}
		return netWorks;
	}
	
	public static int getCoresPerSocket(VgateConnection con, Host host) throws BadServerResponse, XenAPIException, XmlRpcException {
		Map<String, String> cpuInfo = host.getCpuInfo(con);
		if (cpuInfo == null || !cpuInfo.containsKey("cpu_count") || !cpuInfo.containsKey("socket_count")) {
			return 0;
		}
		return Integer.parseInt(cpuInfo.get("cpu_count")) / Integer.parseInt(cpuInfo.get("socket_count"));
	}

	public static void insertTemplate(String uuid, String gold_tmp_uuid,
			String host_uuid, String host_ip, String username, String password, boolean shared)
			throws SQLException {
		Template template = new Template();
		template.setUUID(uuid);
		template.setGold_UUID(gold_tmp_uuid);
		template.setHost_UUID(host_uuid);
		template.setHost_IP(host_ip);
		template.setHost_UserName(username);
		template.setHost_Password(password);
		template.setShared(shared);
		((TemplateDAO) DAOManager.getInstance().getDAO(Table.TEMPLATE))
				.insertTemplate(template);
	}
	
	public static boolean isHostSRShared(VgateConnection con, Map<String, SR> srs) throws BadServerResponse, XenAPIException, XmlRpcException {
		for (Map.Entry<String, SR> entry : srs.entrySet()) {
			SR sr = entry.getValue();
			if (!sr.getShared(con)) {
				return false;
			}
		}
		return true;
	}
}
