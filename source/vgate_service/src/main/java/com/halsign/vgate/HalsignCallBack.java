package com.halsign.vgate;

import java.util.Properties;

import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.VM;

public interface HalsignCallBack {

	public boolean onTemplatePublishSuccess(VgateConnection connection, String templateUuid/*模板uuid*/, String hostUuid);
	
	public boolean onTemplatePublishFail(VgateConnection connection, int errorCode/*出错错误码*/, String templateUuid/*模板uuid*/, String hostUuid);
	
	public boolean onVmCreateSuccess(VgateConnection connection, String hostUUID, Properties aliParam/*阿里自定义的参数*/, VM vm);
	
	public boolean onVmCreateFail(VgateConnection connection, String hostUUID, int errorCode/*出错错误码*/, Properties aliParam/*阿里自定义的参数*/);
}
