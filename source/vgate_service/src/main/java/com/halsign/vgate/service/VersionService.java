package com.halsign.vgate.service;

import com.halsign.vgate.VgateException;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;

public interface VersionService {

	public String getVgateServiceVersion();
	
	public String getVgateVersion(VgateConnection connection) throws VgateException;
	
	public String getSdkVersion();

	
}
