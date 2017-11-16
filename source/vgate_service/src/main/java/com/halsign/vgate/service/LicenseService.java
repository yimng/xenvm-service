package com.halsign.vgate.service;

import com.halsign.vgate.License;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;

public interface LicenseService {

	/*
	 * Apply License to the Host. 
	 * If expireDate is Null in License, will create a license that will never expire.
	 * 
	 * @param con VgateConnection
	 * @param host Host
	 * @param license License
	 * @return VgateMessage
	 * 
	 */
	public VgateMessage applyLicense(VgateConnection con, String hostUuid, License license);
}
