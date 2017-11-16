package com.halsign.vgate;

import java.util.Date;

public class License {

	private Date expireDate;
	private String mac;
	private int socketNum;
	private String companyName;
	
	public Date getExpireDate() {
		return expireDate;
	}
	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}
	public String getMac() {
		return mac;
	}
	public void setMac(String mac) {
		this.mac = mac;
	}
	public int getSocketNum() {
		return socketNum;
	}
	public void setSocketNum(int socketNum) {
		this.socketNum = socketNum;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	
	public String toString() {
		
		return "<xe_license sku_type=\"XE Advanced\" "
				+ "version=\"5.2.0\" "
				+ "productcode=\"0000-0000-0000-0000-0000-0000\" "
				+ "serialnumber=\"00000000-0000-0000-0000-000000000000\" "
				+ "sockets=\"0\" "
				+ "expiry=\"1456848000\" "
				+ "human_readable_expiry=\"2016-03-02\" "
				+ "hwaddr=\"B8:AC:6F:37:CC:FE\" "
				+ "name=\"\" "
				+ "address1=\"\" "
				+ "address2=\"\" "
				+ "city=\"\" "
				+ "state=\"\" "
				+ "postalcode=\"\" "
				+ "country=\"\" "
				+ "company=\"红山世纪\"/>";
	}
}
