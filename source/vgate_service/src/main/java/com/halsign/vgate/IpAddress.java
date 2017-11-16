package com.halsign.vgate;

import java.util.List;

/**
 * IP地址
 *
 *@author 红山世纪
 */
public class IpAddress {
	/**
	 * ip模式(static/dhcp)
	 */
	private String mode;
	/**
	 * ip地址
	 */
	private String ipAddr;
	/**
	 * ip网关
	 */
	private String gateway;
	/**
	 * ip子网掩码
	 */
	private String netmask;
	
	/**
	 * DNS服务列表
	 */
	private List<String> dns;
	
	public List<String> getDns() {
		return dns;
	}
	public void setDns(List<String> dns) {
		this.dns = dns;
	}
	
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getGateway() {
		return gateway;
	}
	public void setGateway(String gateway) {
		this.gateway = gateway;
	}
	public String getNetmask() {
		return netmask;
	}
	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}
	

}
