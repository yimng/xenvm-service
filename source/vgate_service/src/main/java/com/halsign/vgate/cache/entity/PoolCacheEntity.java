package com.halsign.vgate.cache.entity;

import java.util.ArrayList;
import java.util.List;

public class PoolCacheEntity {
	
	private String poolUUID;
	private String IP;
	private String username;
	private String password;
	List<String> hostUUIDs = new ArrayList<String>();
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	
	public String getPoolUUID() {
		return poolUUID;
	}
	public void setPoolUUID(String poolUUID) {
		this.poolUUID = poolUUID;
	}

	public List<String> getHostUUIDs() {
		return hostUUIDs;
	}
	public void addHostUUID(String hostUUID) {
		hostUUIDs.add(hostUUID);
	}
	public String getIP() {
		return IP;
	}
	public void setIP(String iP) {
		IP = iP;
	}
	
	public String toString() {
		return this.poolUUID + " " + this.IP + " " + this.username + " " + this.hostUUIDs + "\n";
	}
}
