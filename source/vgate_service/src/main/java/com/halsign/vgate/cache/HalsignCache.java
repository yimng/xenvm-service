package com.halsign.vgate.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.halsign.vgate.cache.entity.HostCacheEntity;
import com.halsign.vgate.cache.entity.PoolCacheEntity;
import com.halsign.vgate.cache.entity.SREntity;
import com.halsign.vgate.cache.entity.SREntity.SRTYPE;
import com.halsign.vgate.cache.entity.VmCacheEntity;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.halsign.vgate.util.VgateUtil;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Host.Record;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMMetrics;

public class HalsignCache {
	private static final Logger logger = LoggerFactory
			.getLogger(HalsignCache.class);
	private static final HalsignCache INSTANCE = new HalsignCache();

	private final LoadingCache<String, VmCacheEntity> vmCache;
	private final LoadingCache<String, HostCacheEntity> hostCache;
	private final LoadingCache<String, PoolCacheEntity> poolCache;
	private Map<String, VgateConnection> poolCons = new HashMap<String, VgateConnection>();
	
	private HalsignCache() {
		
		this.vmCache = CacheBuilder.newBuilder().build(
				new CacheLoader<String, VmCacheEntity>() {
					@Override
					public VmCacheEntity load(String vmUUID) throws Exception {
						return loadVM(vmUUID);
					}
				});

		this.hostCache = CacheBuilder.newBuilder().build(
				new CacheLoader<String, HostCacheEntity>() {
					@Override
					public HostCacheEntity load(String HostUUID) throws Exception {
						return loadHost(HostUUID);
					}
				});
		
		this.poolCache = CacheBuilder.newBuilder().build(
				new CacheLoader<String, PoolCacheEntity>() {
					@Override
					public PoolCacheEntity load(String poolUUID) throws Exception {
						return loadPool(poolUUID);
					}
				}
				);
	}

	public static HalsignCache getInstance() {
		return INSTANCE;
	}

	public Map<String, PoolCacheEntity> getAllPoolEntity() {
		return poolCache.asMap();
	}
	
	public void refreshPoolCache(String poolUUID) {
		poolCache.refresh(poolUUID);
	}
	public long poolCacheSize () {
		return poolCache.size();
	}
	
	public PoolCacheEntity getPoolCacheEntity(String poolUUID) throws ExecutionException {
		return poolCache.get(poolUUID);
	}
	
	public Map<String, HostCacheEntity> getAllHostEntity() {
		return hostCache.asMap();
	}
	
	public void refreshHostCache(String hostUUID) {
		hostCache.refresh(hostUUID);
	}
	
	public long hostCacheSize() {
		return hostCache.size();
	}
	
	public long vmCacheSize() {
		return vmCache.size();
	}
	
	public void refreshVmCache(String vmUUID) {
		vmCache.refresh(vmUUID);
	}
	
	public void removeVm(String vmUUID) {
		vmCache.invalidate(vmUUID);
	}
	
	public Map<String, VmCacheEntity> getAllVmEntity() {
		return vmCache.asMap();
	}
	
	public HostCacheEntity getHostCacheEntity(String hostUUID) throws ExecutionException {
		return hostCache.get(hostUUID);
	}
	
	public VmCacheEntity getVmCacheEntity(String VmUUID) throws ExecutionException {
		return vmCache.get(VmUUID);
	}
	
	private VmCacheEntity loadVM(String vmUUID)
			throws BadServerResponse, XenAPIException,
			XmlRpcException {
		VgateConnection connection = null;
		for (VgateConnection con : poolCons.values()) {
			try {
				VM.getByUuid(con, vmUUID);
				connection = con;
				break;
			} catch (Exception e) {
				continue;
			}
		}
		Map<Host, Host.Record> hostRecords = Host.getAllRecords(connection);
		Map<VM, VM.Record> vmRecords = VM.getAllRecords(connection);
		Map<VBD, VBD.Record> vbdRecords = VBD.getAllRecords(connection);
		Map<VDI, VDI.Record> vdiRecords = VDI.getAllRecords(connection);
		Map<SR, SR.Record> srRecords = SR.getAllRecords(connection);
		Map<PBD, PBD.Record> pbdRecords = PBD.getAllRecords(connection);
		VM vm = VM.getByUuid(connection, vmUUID);
		VM.Record vmRecord = vmRecords.get(vm);
		
		VmCacheEntity entity = new VmCacheEntity();
		entity.setVmUuid(vmRecord.uuid);
		if (vmRecord.affinity == null || vmRecord.affinity.isNull() || hostRecords.get(vmRecord.affinity) == null) {
			if (vmRecord.residentOn == null || vmRecord.residentOn.isNull()) {
				for (VBD vbd : vmRecord.VBDs) {
					VBD.Record vbdRecord = vbdRecords.get(vbd);
					VDI vdi = vbdRecord.VDI;
					if (vdi.isNull()) {
						continue;
					}
					VDI.Record vdiRecord = vdiRecords.get(vbdRecord.VDI);
					if ((vdiRecord == null || vdiRecord.missing || !vdiRecord.managed)) {
						continue;
					}
					SR sr = vdiRecord.SR;
					if (sr.isNull()) {
						continue;
					}
					SR.Record srRecord = srRecords.get(sr);
					if (srRecord.shared || VgateUtil.isSpeedUp(srRecord)
							|| srRecord.PBDs.size() != 1) {
						continue;
					}
					Iterator<PBD> it = srRecord.PBDs.iterator();
					if (it.hasNext()) {
						PBD pbd = it.next();
						PBD.Record pbdRecord = pbdRecords.get(pbd);
						String hostUUID = hostRecords.get(pbdRecord.host).uuid;
						entity.setVmAffinity(hostUUID);
					}
				}
			} else {
				String hostUUID = hostRecords.get(vmRecord.residentOn).uuid;
				entity.setVmAffinity(hostUUID);
			}
		} else {
			String hostUUID = hostRecords.get(vmRecord.affinity).uuid;
			entity.setVmAffinity(hostUUID);
		}
		entity.setMemory(vmRecord.memoryStaticMax + vmRecord.memoryOverhead);
		
		return entity;
	}
	private HostCacheEntity loadHost(String HostUUID) throws BadServerResponse,
			XenAPIException, XmlRpcException {
		VgateConnection connection = null;
		for (VgateConnection con : poolCons.values()) {
			try {
				Host.getByUuid(con, HostUUID);
				connection = con;
				break;
			} catch (Exception e) {
				continue;
			}
		}
		Host host = Host.getByUuid(connection, HostUUID);
		Map<Host, Host.Record> hostRecords = Host.getAllRecords(connection);
		Map<SR, SR.Record> srRecords = SR.getAllRecords(connection);
		Map<PBD, PBD.Record> pbdRecords = PBD.getAllRecords(connection);

		Host.Record hostRecord = hostRecords.get(host);
		HostCacheEntity entity = new HostCacheEntity();
		entity.setHostUuid(hostRecord.uuid);
		entity.setIp(hostRecord.address);
		entity.setMemory(getHostMemWithoutRunningVM(connection, hostRecord));

		
		for (Map.Entry<SR, SR.Record> entry : srRecords.entrySet()) {
			com.xensource.xenapi.SR.Record srRec = entry.getValue();
			if (VgateUtil.isSpeedUp(srRec)) {
				continue;
			}
			String type = srRec.type;
			if (!isValidSr(type)) {
				continue;
			}
			
			for (PBD pbd : srRec.PBDs) {
				PBD.Record pbdRecord = pbdRecords.get(pbd);
				if (pbdRecord.host.equals(host)) {
					Set<String> tags = new HashSet<String>();
					tags.addAll(srRec.tags);
					SREntity srEntity = new SREntity(srRec.uuid, SREntity.fromString(type), srRec.shared);
					srEntity.setTags(tags);
					entity.addSRs(srEntity);
				}
			}
		}
		
		return entity;
	}

	private PoolCacheEntity loadPool(String poolUUID) throws BadServerResponse, XenAPIException, XmlRpcException {
		VgateConnection connection = null;
		for (VgateConnection con : poolCons.values()) {
			try {
				Pool.getByUuid(con, poolUUID);
				connection = con;
				break;
			} catch (Exception e) {
				continue;
			}
		}
		PoolCacheEntity poolEntity = new PoolCacheEntity();
		poolEntity.setPoolUUID(poolUUID);
		poolEntity.setIP(connection.getIp());
		poolEntity.setUsername(connection.getUsername());
		poolEntity.setPassword(connection.getPassword());
		Set<Host> hosts = Host.getAll(connection);
		Map<Host, Record> hostRecs = Host.getAllRecords(connection);
		for (Host host : hosts) {
			Record hostRec = hostRecs.get(host);
			poolEntity.addHostUUID(hostRec.uuid);
		}
		
		return poolEntity;
	}
	
	private long getHostMemWithoutRunningVM(Connection con, Host.Record host)
			throws BadServerResponse, XenAPIException, XmlRpcException {

		long xenmem = host.metrics.getMemoryTotal(con) - host.memoryOverhead
				- VgateConstants.MEMORYBUFFER;
		for (VM vm : host.residentVMs) {
			if (vm.getIsControlDomain(con)) {
				xenmem = xenmem - vm.getMemoryOverhead(con);
				VMMetrics metrics = vm.getMetrics(con);
				if (!metrics.isNull()) {
					xenmem = xenmem - metrics.getMemoryActual(con);
				}
			}
		}
		return xenmem;
	}
	
	public void initializeCaches(List<VgateConnection> connections) throws ExecutionException
			 {
		long start = System.currentTimeMillis();
		this.poolCache.invalidateAll();
		this.hostCache.invalidateAll();
		this.vmCache.invalidateAll();
		for (VgateConnection connection : connections) {
			com.xensource.xenapi.Pool.Record poolRec;
			try {
				poolRec = VgateUtil.getPool(connection);
			} catch (XenAPIException | XmlRpcException e) {
				logger.info("!!!!!!!!!!! intit cache failed!!!!!!!!!!!!!!!!", e);
				continue;
			}
			if (!poolCons.containsKey(poolRec.uuid)) {
				poolCons.put(poolRec.uuid, connection);
			}
			
			try {
				String username = connection.getUsername();
				String password = connection.getPassword();
				String ip = connection.getIp();
				logger.info("[HalsignCache.initializeCaches()]=============loading pool caches...");
				Set<Pool> pools = Pool.getAll(connection);
				Map<Host, String> hostTempMap = new HashMap<Host, String>();

				Map<Host, Host.Record> hostRecords = Host.getAllRecords(connection);
				Map<SR, SR.Record> srRecords = SR.getAllRecords(connection);
				Map<PBD, PBD.Record> pbdRecords = PBD.getAllRecords(connection);
				Map<VDI, VDI.Record> vdiRecords = VDI.getAllRecords(connection);
				Map<VBD, VBD.Record> vbdRecords = VBD.getAllRecords(connection);
				Map<VM, VM.Record> vmRecords = VM.getAllRecords(connection);
				
				for (Pool pool : pools) {
					PoolCacheEntity poolEntity = new PoolCacheEntity();
					String poolUUID = pool.getUuid(connection);
					poolEntity.setPoolUUID(poolUUID);
					poolEntity.setIP(ip);
					poolEntity.setUsername(username);
					poolEntity.setPassword(password);

					logger.info("[HalsignCache.initializeCaches()]=============loading host caches...");

					for (Map.Entry<Host, Host.Record> entry : hostRecords.entrySet()) {
						Host key = entry.getKey();
						com.xensource.xenapi.Host.Record value = entry.getValue();

						HostCacheEntity hostEntity = new HostCacheEntity();
						hostEntity.setHostUuid(value.uuid);
						hostEntity.setIp(value.address);
						hostEntity.setMemory(getHostMemWithoutRunningVM(connection, value));

						hostCache.put(value.uuid, hostEntity);
						hostTempMap.put(key, value.uuid);
						poolEntity.addHostUUID(value.uuid);
					}

					for (Map.Entry<SR, SR.Record> entry : srRecords.entrySet()) {
						com.xensource.xenapi.SR.Record srRec = entry.getValue();
						if (VgateUtil.isSpeedUp(srRec)) {
							continue;
						}
						String type = srRec.type;
						if (!isValidSr(type)) {
							continue;
						}
						for (PBD pbd : srRec.PBDs) {
							
							PBD.Record pbdRecord = pbdRecords.get(pbd);
							Set<String> tags = new HashSet<String>();
							tags.addAll(srRec.tags);
							SREntity srEntity = new SREntity(srRec.uuid, SREntity.fromString(type), srRec.shared);
							srEntity.setTags(tags);
							hostCache.get(hostTempMap.get(pbdRecord.host)).addSRs(srEntity);
						}
					}
					poolCache.put(poolUUID, poolEntity);
				}
				logger.info("[HalsignCache.initializeCaches()]=============loading vm caches...");
				for (Map.Entry<VM, VM.Record> vEntry : vmRecords.entrySet()) {
					com.xensource.xenapi.VM.Record value = vEntry.getValue();

					if (value.isASnapshot || value.isATemplate
							|| value.isControlDomain
							|| value.isSnapshotFromVmpp) {
						continue;
					}

					VmCacheEntity entity = new VmCacheEntity();
					entity.setVmUuid(value.uuid);

					if (value.affinity == null || value.affinity.isNull()
							|| hostTempMap.get(value.affinity) == null) {
						if (value.residentOn == null
								|| value.residentOn.isNull()) {
							for (VBD vbd : value.VBDs) {
								VBD.Record vbdRecord = vbdRecords.get(vbd);
								VDI vdi = vbdRecord.VDI;
								if (vdi.isNull()) {
									continue;
								}
								VDI.Record vdiRecord = vdiRecords
										.get(vbdRecord.VDI);
								if ((vdiRecord == null || vdiRecord.missing || !vdiRecord.managed)) {
									continue;
								}
								SR sr = vdiRecord.SR;
								if (sr.isNull()) {
									continue;
								}
								SR.Record srRecord = srRecords.get(sr);
								if (srRecord.shared
										|| VgateUtil.isSpeedUp(srRecord)
										|| srRecord.PBDs.size() != 1) {
									continue;
								}
								Iterator<PBD> it = srRecord.PBDs.iterator();
								if (it.hasNext()) {
									PBD pbd = it.next();
									PBD.Record pbdRecord = pbdRecords.get(pbd);
									String hostUuid = hostTempMap
											.get(pbdRecord.host);
									entity.setVmAffinity(hostUuid);
								}
							}
						} else {
							String hostUuid = hostTempMap.get(value.residentOn);
							entity.setVmAffinity(hostUuid);
						}
					} else {
						String hostUuid = hostTempMap.get(value.affinity);
						entity.setVmAffinity(hostUuid);
					}
					entity.setMemory(value.memoryStaticMax
							+ value.memoryOverhead);
					vmCache.put(value.uuid, entity);
				}
			} catch (BadServerResponse e) {
				String message = "[HalsignCache.initializeCaches()]=============Failed to load cache due to "
						+ e.toString();
				logger.error(message, e);
				vmCache.invalidateAll();
				hostCache.invalidateAll();
			} catch (XenAPIException e) {
				String message = "[HalsignCache.initializeCaches()]=============Failed to load cache due to "
						+ e.toString();
				logger.error(message, e);
				vmCache.invalidateAll();
				hostCache.invalidateAll();
			} catch (XmlRpcException e) {
				String message = "[HalsignCache.initializeCaches()]=============Failed to load cache due to "
						+ e.toString();
				logger.error(message, e);
				vmCache.invalidateAll();
				hostCache.invalidateAll();
			} finally {
			}
		}
		long stop = System.currentTimeMillis();
		for (PoolCacheEntity pool : this.poolCache.asMap().values()) {
			logger.info("-----------------------------pool");
			logger.info(pool.toString());
		}
		for (HostCacheEntity host : this.hostCache.asMap().values()) {
			logger.info("-----------------------------host");
			logger.info(host.toString());
		}
//		for (VmCacheEntity vm : this.vmCache.asMap().values()) {
//			logger.info("-----------------------------vm");
//			logger.info(vm.toString());
//		}
		logger.info("Cache initialize time is " + (stop - start));
	}

	private boolean isValidSr(String type) {
		return SRTYPE.EXT.toString().equalsIgnoreCase(type) ||
				SRTYPE.LVMOHBA.toString().equalsIgnoreCase(type)||
				SRTYPE.NFS.toString().equalsIgnoreCase(type)||
				SRTYPE.LVMOISCSI.toString().equalsIgnoreCase(type)||
				SRTYPE.LVM.toString().equalsIgnoreCase(type)||
				SRTYPE.LVMOBOND.toString().equalsIgnoreCase(type)||
				SRTYPE.LVMOFC.toString().equalsIgnoreCase(type)||
				SRTYPE.RAWHBA.toString().equalsIgnoreCase(type);
	}

	public Map<String, HostCacheEntity> getHostsByPools(List<VgateConnection> cons) throws ExecutionException {
		Map<String, HostCacheEntity> hosts = new HashMap<String, HostCacheEntity>();
		for (VgateConnection con : cons) {
			com.xensource.xenapi.Pool.Record pool;
			try {
				pool = VgateUtil.getPool(con);
			} catch (XenAPIException | XmlRpcException e) {
				logger.info("getHostsByPools failed", e);
				continue;
			}
			PoolCacheEntity poolCacheEntity = this.getPoolCacheEntity(pool.uuid);
			for (String hostUUID: poolCacheEntity.getHostUUIDs()) {
				hosts.put(hostUUID, this.getHostCacheEntity(hostUUID));
			}
		}

		return hosts;
	}
}
