/*
 * <p>Copyright ®红山公司。</p>
 * 类名: com.halsign.vgate.service.NetworkService
 * 创建人: 李娟    
 * 创建时间: 2015年7月24日
 */
package com.halsign.vgate.service;

import com.halsign.vgate.IpAddress;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;



/**
 * 
 * 网络的后台服务.
 * 
 * @author 红山世纪
 *
 */
public interface NetworkService {

	/**
	 * 设置主机名称.
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param hostName 想要给虚拟机设置的主机名称
	 * 
	 * @return 设置是否成功的信息
	 */

	VgateMessage setVmHostName(VgateConnection connection, String vmUuid, String hostName);
	
	/**
	 * 设置ip地址.
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param ip 想要给虚拟机设置的ip地址
	 * 
	 * @return 设置是否成功的信息
	 */
	VgateMessage setVmIp(VgateConnection connection, String vmUuid, IpAddress ip, boolean match);
	
	
}
