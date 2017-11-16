package com.halsign.vgate.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.IpAddress;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.service.NetworkService;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VifOperations;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VIF.Record;
import com.xensource.xenapi.VM;


/**
 * 
 * 网络的后台服务.
 * 
 * @author 红山世纪
 *
 */

public class NetworkServiceImp implements NetworkService {
	static Logger logger = LoggerFactory.getLogger(NetworkServiceImp.class);
	
	/**
	 * 设置主机名称.
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param hostName 想要给虚拟机设置的主机名称
	 * 
	 * @return 设置是否成功的信息
	 */
	@Override
	public VgateMessage setVmHostName(VgateConnection connection, String vmUuid, String hostName) {
		//TODO check Connection
		logger.info("set VM hostname ---------------> start");
		if(connection == null || (!VgateUtil.isConnectionValid(connection))) {
			logger.info("CONNECT INVALID");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
					VgateMessageConstants.ERROR_CODE_CONNECTION_INVALID);
		}
		
		VM vm = null;
		try {
			logger.info("VM UUID -----------------------> "+vmUuid);
			vm = VM.getByUuid(connection, vmUuid);
			
			String valid_host_regex = "^(?!\\d)[a-zA-Z0-9-]{1,15}$";
			if(hostName != null && hostName.matches(valid_host_regex)) {
				logger.info("new host name -----------------> "+hostName);
				Map<String, String> xenstoreData = new HashMap<String, String>();
				
				xenstoreData.putAll(vm.getXenstoreData(connection));
				xenstoreData.put("vm-data/hostname", hostName);
				
				logger.info("UPDATE THE HOSTNAME OF VM");
				vm.setXenstoreData(connection, xenstoreData);
				
			} else {
				logger.info("UPDATE HOSTNAME OF VM FAILURE");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_CODE_VM_HOSTNAME_UPDATE_FAILURE);
			}
			
		} catch (XenAPIException | XmlRpcException e) {
			String message = "Failed to update the hostname of VM due to "+e.toString();
			logger.error(message);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_VM_GET_FAILURE);
		} 
		
		logger.info("set VM hostname ---------------> sussecc");
		
		return new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
		
	}

	/**
	 * 设置ip地址.
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param ip 想要给虚拟机设置的ip地址
	 * 
	 * @return 设置是否成功的信息
	 */
	@Override
	public VgateMessage setVmIp(VgateConnection connection, String vmUuid, IpAddress ip, boolean match) {
		logger.info("set VM IP ---------------> start");
		if(connection == null || (!VgateUtil.isConnectionValid(connection))) {
			logger.info("CONNECT INVALID");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
					VgateMessageConstants.ERROR_CODE_CONNECTION_INVALID);
		}
		
		VM vm = null;
		try {
			logger.info("VM UUID -----------------> "+vmUuid);
			vm = VM.getByUuid(connection, vmUuid);
			
			if(ip == null) {
				logger.info("------------------> IP IS NULL");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_CODE_VM_IP_UPDATE_FAILURE);
			}
			
			
			if ("static".equalsIgnoreCase(ip.getMode()) && match) {
				String ipAddr = ip.getIpAddr();
				if (ipAddr != null) {
					String reg = "\\d{1,3}\\.\\d{1,3}\\.(\\d{1,3})\\.\\d{1,3}";
					Pattern p = Pattern.compile(reg);
					Matcher matcher = p.matcher(ipAddr);
					if (matcher.find()) {
						String network = matcher.group(1);
						Set<Network> all = Network.getAll(connection);
						for (Network nt : all) {
							if (nt.getTags(connection).contains("vlan_" + network)) {
								String device = deleteVMVif(connection, vm);
								createVMVif(connection, vm, nt, device);
							}
						}
					}
				}
			}
			
			
			
			logger.info("------------------> get the vifs of VM");
			Set<VIF> vifs = vm.getVIFs(connection);
			
			if(vifs == null || vifs.size() == 0) {
				logger.info("------------------> GET VIFS FAILURE ");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_CODE_VIFS_GET_FAILURE);
			}
			
			String ip_regex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
			
			if(ip.getIpAddr() != null && ip.getIpAddr().matches(ip_regex)) {
				VIF[] vifsArray = vifs.toArray(new VIF[vifs.size()]);
				 
				String macAddr = vifsArray[0].getMAC(connection).toUpperCase().replaceAll(":", "_");
				Map<String, String> xenstoreData = new HashMap<String, String>();
				
				xenstoreData.putAll(vm.getXenstoreData(connection));
				
				logger.debug("------------------> set new ip address || "+ip.getIpAddr());
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/ip/name", "IPAddress");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/ip/type", "multi_sz");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/ip/data/0", ip.getIpAddr());
				
				logger.debug("------------------> set new dhcp       || 0");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dhcp/name", "EnableDhcp");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dhcp/type", "dword");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dhcp/data", "0");
				
				logger.debug("------------------> set new subnetmask || "+ip.getNetmask());
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/mask/name", "SubnetMask");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/mask/type", "multi_sz");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/mask/data/0", ip.getNetmask());
				
				logger.debug("------------------> set new gateway    || "+ip.getGateway());
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/gate/name", "DefaultGateway");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/gate/type", "multi_sz");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/gate/data/0", ip.getGateway());
				
				List<String> dns = ip.getDns();
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dns/name", "NameServer");
				xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dns/type", "string");
				for(int i = 0; i < dns.size(); i++) {
					if(i == 0) {
						logger.debug("------------------> set new nameserver || "+dns.get(i));
						xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dns/data", dns.get(i));
					} else {
						logger.debug("------------------> add new nameserver || "+dns.get(i));
						xenstoreData.put("vm-data/vif/"+macAddr+"/tcpip/dns/data", 
								xenstoreData.get("vm-data/vif/"+macAddr+"/tcpip/dns/data")+","+dns.get(i));
					}
				}
				
				logger.info("UPDATE IP FOR VM");
				vm.setXenstoreData(connection, xenstoreData);
				
			} else {
				logger.info("------------------> ILLEGAL IPADDRESS OR IPADDRESS IS NULL");
				return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
						VgateMessageConstants.ERROR_CODE_VM_IP_UPDATE_FAILURE);
			}
			
		} catch (XenAPIException | XmlRpcException e) {
			String message = "Failed to get the VM due to "+e.toString();
			logger.error(message);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
					VgateMessageConstants.ERROR_CODE_VM_GET_FAILURE);
		}
		
		logger.info("set VM IP ---------------> success");
		
		return new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);	
		
	}
	
	private String deleteVMVif(VgateConnection con, VM vm) throws BadServerResponse, XenAPIException, XmlRpcException {
		Set<VIF> viFs = vm.getVIFs(con);
		String device = "0";
		for (VIF vif : viFs) {
			device = vif.getDevice(con);
			Record rec = vif.getRecord(con);
			if(rec != null && rec.currentlyAttached && rec.allowedOperations.contains(VifOperations.UNPLUG)){
				vif.unplug(con);
			}
			vif.destroy(con);
		}
		return device;
	}

	private VIF createVMVif(VgateConnection con, VM vm, Network network, String device) throws BadServerResponse, XenAPIException, XmlRpcException {
		VIF.Record record = new VIF.Record();
		record.VM = vm;
		record.network = network;
		record.device = device;
		record.MTU = 1500L;
		record.lockingMode = com.xensource.xenapi.Types.VifLockingMode.NETWORK_DEFAULT;
		VIF vif = VIF.create(con, record);
		Record rec = vif.getRecord(con);
		if (rec != null && !rec.currentlyAttached && rec.allowedOperations.contains(VifOperations.PLUG)) {
			vif.plug(con);
		}
		return vif;
	}
}
