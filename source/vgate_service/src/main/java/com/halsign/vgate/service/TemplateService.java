/**
 * 
 */
package com.halsign.vgate.service;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

import com.halsign.vgate.Template;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.spec.TemplateSpec;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

/**
 * 模板的后台服务。
 * 
 * @author 红山世纪
 *
 */
public interface TemplateService {

	  /**
	   * 发布模板。
	   * 根据虚拟机转化为模板，这是异步调用。
	   * 
	   * @param connection 连接
	   * @param vmUuid 虚拟机的uuid
	   * 
	   * @return 异步创建模板的任务
	   */
       VgateTask publishTemplateAsync(List<VgateConnection> connections, String vmUuid, TemplateSpec templateSpec) throws BadServerResponse, XenAPIException, XmlRpcException;
       
       /**
 	   * 获取所有模板.
 	   * 先检查nfs存储，当nfs不存在时会尝试修复，修复失败会抛异常。
 	   * 
 	   * @param connection 连接
 	   * @exception VgateException vgate异常
 	   * 
 	   * @return 所有模板的集合
 	   */
       List<Template> listTemplate(VgateConnection connection) throws VgateException;
       
       /**
 	   * 删除指定模板.
 	   * 
 	   * @param connection 连接
 	   * @param templateUuid 模板的uuid
 	   * 
 	   * @return 操作是否成功的信息
 	   */
       VgateMessage deleteTemplate(String templateUuid) throws VgateException;
       
       /**
  	   * 插入模板.
  	   * 
  	   * @param connection 连接
  	   * @param templateUuid 模板的uuid
  	   * @param srUUID 	SR的uuid
  	   * 
  	   * @return 操作是否成功的信息
  	   */
       VgateMessage insertTemplate(VgateConnection conn, String tmpUUID, String srUUID) throws VgateException;
       
       VgateTask updateTemplate(List<VgateConnection> connections, String templateUuid, TemplateSpec templateSpec) throws BadServerResponse, XenAPIException, XmlRpcException;
       
}
