package com.halsign.vgate.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VbdType;
import com.xensource.xenapi.Types.VdiOperations;
import com.xensource.xenapi.Types.VdiType;
import com.xensource.xenapi.Types.VifOperations;
import com.xensource.xenapi.Types.VmOperations;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VIF.Record;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

public class VmUtil {
	private static final Logger logger = LoggerFactory.getLogger(VmUtil.class);
	
	
	public static void copyVdiOfTemplate(VgateConnection connection,
			VM nfsTemplate, VM destTemplate, Map<String, SR> srs)
			throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VBD> vbdSet = nfsTemplate.getVBDs(connection);
		Iterator<VBD> vbdIterator = vbdSet.iterator();
		while (vbdIterator.hasNext()) {
			VBD currentVBD = (VBD) vbdIterator.next();
			if (VgateUtil.isValidVbd(connection, currentVBD)) {
				VDI vdi = currentVBD.getVDI(connection);
				if (vdi.getType(connection) == VdiType.USER) {
					SR sr = srs.get("usr") != null ? srs.get("usr") : srs
							.get("sys");
					VDI resultVdi = vdi.copy(connection, sr);

					VBD.Record vbdrecord = new VBD.Record();
					vbdrecord.VM = destTemplate;
					vbdrecord.VDI = resultVdi;
					vbdrecord.userdevice = currentVBD.getUserdevice(connection);
					vbdrecord.mode = Types.VbdMode.RW;
					vbdrecord.type = Types.VbdType.DISK;
					vbdrecord.empty = false;

					VBD.create(connection, vbdrecord);

				} else if (vdi.getType(connection) == VdiType.SYSTEM) {
					SR sr = srs.get("sys") != null ? srs.get("sys") : srs
							.get("usr");
					VDI resultVdi = vdi.copy(connection, sr);

					VBD.Record vbdrecord = new VBD.Record();
					vbdrecord.VM = destTemplate;
					vbdrecord.VDI = resultVdi;
					vbdrecord.userdevice = currentVBD.getUserdevice(connection);
					vbdrecord.mode = Types.VbdMode.RW;
					vbdrecord.type = Types.VbdType.DISK;
					vbdrecord.empty = false;

					VBD.create(connection, vbdrecord);

				}
			}
			VbdType type = currentVBD.getType(connection);
			if (Types.VbdType.CD == type) {
				logger.info("start create CD");
				VBD.Record vbdrecord = new VBD.Record();

				vbdrecord.VM = destTemplate;
				vbdrecord.VDI = null;
				vbdrecord.userdevice = "3";
				vbdrecord.mode = Types.VbdMode.RO;
				vbdrecord.type = Types.VbdType.CD;
				vbdrecord.empty = true;
				VBD.create(connection, vbdrecord);
			}
		}
	}
	
	public static void createVIFOfTemplate(VgateConnection connection, VM vm,
			Network netWork) throws BadServerResponse, XenAPIException,
			XmlRpcException {
		Set<VIF> viFs = vm.getVIFs(connection);
		for (VIF vif : viFs) {
			vif.destroy(connection);
		}
		VIF.Record vifRecord = new VIF.Record();
		vifRecord.network = netWork;
		vifRecord.MAC = "";
		vifRecord.device = VgateUtil.getDeviceId(connection, vm) + "";
		vifRecord.VM = vm;
		VIF vif = VIF.create(connection, vifRecord);
		Record rec = vif.getRecord(connection);
		if (rec != null && !rec.currentlyAttached && rec.allowedOperations.contains(VifOperations.PLUG)) {
			vif.plug(connection);
		}
	}
	
	public static void resizeVdis(VgateConnection connection, VM vm, long systemVDISize, long dataVDISize) throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VBD> vbDs = vm.getVBDs(connection);
		Iterator<VBD> vbdIterator = vbDs.iterator();
		while (vbdIterator.hasNext()) {
			VBD currentVBD = (VBD) vbdIterator.next();
			if (VgateUtil.isValidVbd(connection, currentVBD)) {
				VDI vdi = currentVBD.getVDI(connection);
				Long virtualSize = vdi.getVirtualSize(connection);
				if (vdi.getType(connection) == VdiType.USER) {
					if (dataVDISize > virtualSize) {
						if (vdi.getAllowedOperations(connection).contains(VdiOperations.RESIZE)) {
							vdi.resize(connection, dataVDISize);
						} else {
							vdi.resizeOnline(connection, dataVDISize);
						}
					}
				}
				if (vdi.getType(connection) == VdiType.SYSTEM) {
					if (systemVDISize > virtualSize) {
						if (vdi.getAllowedOperations(connection).contains(VdiOperations.RESIZE)) {
							vdi.resize(connection, systemVDISize);
						} else {
							vdi.resizeOnline(connection, systemVDISize);
						}
					}
				}
			}
		}
	}
	
	public static void changeMemory(VgateConnection connection, VM vm, long memory) throws BadServerResponse, XenAPIException, XmlRpcException {
		Long origStaticMax = vm.getMemoryStaticMax(connection);
		logger.info("origStaticMax: " + origStaticMax + " bytes");
		logger.info("change vm memory to: " + memory + " bytes");
		VmPowerState powerState = vm.getPowerState(connection);
		boolean staticChanged = origStaticMax != memory;
		boolean needReboot = staticChanged && powerState != Types.VmPowerState.HALTED;
		logger.info("staticchanged: " + staticChanged);
		logger.info("needReboot: " + needReboot);
		Host vmHome = null;
		if (needReboot) {
			vmHome = getVmHome(connection, vm);
			if (vm.getAllowedOperations(connection).contains(VmOperations.CLEAN_SHUTDOWN)) {
				vm.cleanShutdown(connection);
			} else {
				vm.hardShutdown(connection);
			}
		}
		try {
			if (staticChanged) {
				vm.setMemoryLimits(connection, memory, memory, memory, memory);
			} else {
				vm.setMemoryDynamicRange(connection, memory, memory);
			}
		} finally {
			if (needReboot) {
				vm.startOn(connection, vmHome, false, false);
			}
		}
	}
	
	public static Host getVmHome(VgateConnection connection, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		Boolean isATemplate = vm.getIsATemplate(connection);
		if (isATemplate) {
			return null;
		}
		VmPowerState powerState = vm.getPowerState(connection);
		if (powerState == Types.VmPowerState.RUNNING) {
			return vm.getResidentOn(connection);
		}
		Host vmStorageHost = VgateUtil.getVMStorageHost(connection, vm);
		if (vmStorageHost != null) {
			return vmStorageHost;
		}
		Host affinity = vm.getAffinity(connection);
		
		if (affinity != null && affinity.getMetrics(connection) == null ? false
				: affinity.getMetrics(connection).getLive(connection)) {
			return affinity;
		}
		
		return null;
	}
	
	public static void setVCPU(VgateConnection connection, VM vm, int cpuCount, int socketCount) throws BadServerResponse, XenAPIException, XmlRpcException {
		int maxCoresPerSocket = getMaxCoresPerSocket(connection, vm);
		Long vcpUsAtStartup = vm.getVCPUsAtStartup(connection);
		boolean checkVCPUConfig = checkVCPUConfig(cpuCount, socketCount, maxCoresPerSocket);
		if (checkVCPUConfig) {
			logger.info("Set VCPU start...");
			if (vcpUsAtStartup > cpuCount) {
				vm.setVCPUsAtStartup(connection, (long)cpuCount);
				vm.setVCPUsMax(connection, (long)cpuCount);
			} else {
				vm.setVCPUsMax(connection, (long)cpuCount);
				vm.setVCPUsAtStartup(connection, (long)cpuCount);
			}
		} else {
			//TODO
			logger.error("set cpu count failed");
		}
	}
	
	public static boolean checkVCPUConfig(int cpuCount, int socketCount, int maxCoresPerSocket) {
		return socketCount > 0 && socketCount < cpuCount
				&& cpuCount % socketCount == 0
				&& cpuCount / socketCount < maxCoresPerSocket;
	}
	
	public static int getMaxCoresPerSocket(VgateConnection connection, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		Host vmHome = getVmHome(connection, vm);
		if (vmHome != null) {
			return VgateUtil.getCoresPerSocket(connection, vmHome);
		}
		int maxCoresPerSocket = 0;
		Set<Host> hosts = Host.getAll(connection);
		for (Host host : hosts) {
			if (VgateUtil.getCoresPerSocket(connection, host) > maxCoresPerSocket) {
				maxCoresPerSocket = VgateUtil.getCoresPerSocket(connection, host);
			}
		}
		return maxCoresPerSocket;
	}
	
	public static long getVMOverHead(VgateConnection con, VM vm, long staticMax)
			throws BadServerResponse, XenAPIException, XmlRpcException {
		Long vcpUsMax = vm.getVCPUsMax(con);
		Double multiplier = vm.getHVMShadowMultiplier(con);

		double ceil = Math.ceil(staticMax / VgateConstants.BINARY_MEGA + 128 * vcpUsMax);
		double shadow = Math.max(1, (ceil / 128) * multiplier);
		if (isHVM(con, vm)) {
			return (long) (2 + shadow) * VgateConstants.BINARY_MEGA;
		} else {
			return (long) (1 + shadow) * VgateConstants.BINARY_MEGA;
		}

	}
	
	public static boolean isHVM(VgateConnection con, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		return !vm.getIsControlDomain(con) && !"".equals(vm.getHVMBootPolicy(con));
	}
	
	public static void deleteVMwithVDI(VgateConnection con, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VBD> vbDs = vm.getVBDs(con);
		List<VDI> vdiList = new ArrayList<VDI>();
		for (VBD vbd : vbDs) {
			if (vbd.getEmpty(con) || vbd.getType(con) == com.xensource.xenapi.Types.VbdType.CD) {
				continue;
			}
			VDI vdi = vbd.getVDI(con);
			if (!vdi.isNull()) {
				vdiList.add(vdi);
			}
		}
		VDI suspendVDI = vm.getSuspendVDI(con);
		if (!suspendVDI.isNull()) {
			vdiList.add(suspendVDI);
		}
		for (VDI vdi : vdiList) {
			logger.info("delete vdi");
			vdi.destroy(con);
		}
		logger.info("delete vm");
		vm.destroy(con);
	}
 }
