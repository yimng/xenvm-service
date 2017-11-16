package com.halsign.vgate.service.impl;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.VgateException;
import com.halsign.vgate.service.VersionService;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateUtil;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

public class VersionServiceImpl implements VersionService{

	final static Logger logger = LoggerFactory.getLogger(VersionServiceImpl.class);
	public static final String VGATE_SERVICE_MAJOR_VERSION = "2";
	public static final String VGATE_SERVICE_MINOR_VERSION = "11";
	public static final String VGATE_SERVICE_MICRO_VERSION = "10";
	
	@Override
	public String getVgateServiceVersion() {
		StringBuffer versionBuffer = new StringBuffer();
		versionBuffer.append(VGATE_SERVICE_MAJOR_VERSION).append(".")
		             .append(VGATE_SERVICE_MINOR_VERSION).append(".")
		             .append(VGATE_SERVICE_MICRO_VERSION);
		
		return versionBuffer.toString();
	}

	@Override
	public String getSdkVersion() {
		
		return APIVersion.latest().toString();
	}

	@Override
	public String getVgateVersion(VgateConnection connection)
			throws VgateException {
		String vgateVersion = null;
		
		if(connection == null || (!VgateUtil.isConnectionValid(connection))) {
			logger.error("[getVgateVersion()]The connection is invalid.");
			
			return null;
		}
		
		try {
			Host host = VgateUtil.getMasterHostFromPool(connection);
			if(host != null) {
				vgateVersion = VgateUtil.getVgateVersion(connection, host);
				logger.info("The vgate version is " + vgateVersion);
			}
			
		} catch (BadServerResponse e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XenAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return vgateVersion;
	}



}
