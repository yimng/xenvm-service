/**
 * 
 */
package com.halsign.vgate.service;

import java.util.List;
import java.util.Properties;

import org.apache.xmlrpc.XmlRpcException;

import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.spec.VmSpec;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

/**
 * 虚拟机的后台服务。
 * 
 * @author 红山世纪
 *
 */
public interface VmService {
	/**
	 * 根据模板创建虚拟机。
	 * 异步调用过程.
	 * 
	 * @param connection 连接
	 * @param templateUuid 模板的uuid
	 * @return 根据模板创建虚拟机的任务
	 */
	VgateTask createVmFromTemplateAsync(List<VgateConnection> connections, String templateUuid, VmSpec vmSpec, int numberOfCreate, Properties proper) throws VgateException, BadServerResponse, XenAPIException, XmlRpcException;
	
	/**
	 * 还原虚拟机到模板的初始状态。
	 * 虚拟机内容会全部回滚，异步调用过程。
	 * 
	 * @param connection 连接
	 * @param vmUuid 虚拟机的uuid
	 * @param templateUuid 模板的uuid
	 * @return  还原虚拟机的任务
	 */
	VgateTask revertVmToTemplateAsync(VgateConnection connection, String vmUuid, String templateUuid) throws VgateException;
	
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
	VgateTask revertVmDiskToTemplateAsync(VgateConnection connection, String vmUuid, String templateUuid, String vdiUuid) throws VgateException;

	/**
	 * 根据模板的uuid获取对应的虚拟机列表.
	 * 
	 * @param connection 连接
	 * @param templateUuid 模板的uuid
	 * 
	 * @return 虚拟机列表
	 */
	List<VM> listVmByTemplateUuid(VgateConnection connection, String templateUuid) throws VgateException;
	
}
