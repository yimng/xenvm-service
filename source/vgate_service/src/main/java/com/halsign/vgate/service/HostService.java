package com.halsign.vgate.service;

import com.halsign.vgate.VgateTask;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.xensource.xenapi.Host;

public interface HostService {
	
	VgateTask patchUpgradeHost(VgateConnection con, Host host, String patchFile);
	VgateTask patchRevertHost(VgateConnection con, Host host, String patchFile);
	void recoverVgateSlaves(String masterIP, String username, String ip);
}
