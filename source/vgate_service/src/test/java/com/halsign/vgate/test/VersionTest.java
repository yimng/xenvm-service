package com.halsign.vgate.test;

import com.halsign.vgate.VgateException;
import com.halsign.vgate.service.VersionService;
import com.halsign.vgate.service.impl.VersionServiceImpl;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;

public class VersionTest {

	public static void main(String[] args) {
		
		String hostId = "192.168.1.111";
		String userName = "root";
		String pwd = "aliursys";
		
		VersionService versionService = new VersionServiceImpl();
		String vgateServiceVersion = versionService.getVgateServiceVersion();
		System.out.println("Vgate service's version is " + vgateServiceVersion);

		try {
			VgateConnection vgateConn = VgateConnectionPool.getInstance().getConnect(hostId, userName, pwd, 10, 10);

			String vgateVersion = versionService.getVgateVersion(vgateConn);
		} catch (VgateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
