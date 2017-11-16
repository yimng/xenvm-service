package com.halsign.vgate;


public class Template {
	
	String UUID;
	String Gold_UUID;
	String Host_UUID;
	String Host_IP;
	String Host_UserName;
	String Host_Password;
	boolean isShared;
	
	public String getUUID() {
		return UUID;
	}
	public void setUUID(String uUID) {
		UUID = uUID;
	}
	public String getGold_UUID() {
		return Gold_UUID;
	}
	public void setGold_UUID(String gold_UUID) {
		Gold_UUID = gold_UUID;
	}
	public String getHost_UUID() {
		return Host_UUID;
	}
	public void setHost_UUID(String host_UUID) {
		Host_UUID = host_UUID;
	}
	public String getHost_IP() {
		return Host_IP;
	}
	public void setHost_IP(String host_IP) {
		Host_IP = host_IP;
	}
	public String getHost_UserName() {
		return Host_UserName;
	}
	public void setHost_UserName(String host_UserName) {
		Host_UserName = host_UserName;
	}
	public String getHost_Password() {
		return Host_Password;
	}
	public void setHost_Password(String host_Password) {
		Host_Password = host_Password;
	}
	public boolean isShared() {
		return isShared;
	}
	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("UUID: " + this.UUID + "\n");
		sb.append("Gold UUID: " + this.Gold_UUID + "\n");
		sb.append("Host UUID: " + this.Host_UUID + "\n");
		return sb.toString();
	}
}
