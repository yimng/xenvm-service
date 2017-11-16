package com.halsign.vgate.cache.entity;

import java.util.ArrayList;
import java.util.List;

public class HostCacheEntity {

	private String hostUuid;
	private List<SREntity> SRs = new ArrayList<SREntity>();
	private String ip;
	private long memory;
	private int vmCount;
	
	public List<SREntity> getSRs() {
		return SRs;
	}
	public void addSRs(SREntity sr) {
		this.SRs.add(sr);
	}
	public long getMemory() {
		return memory;
	}
	public void setMemory(long memory) {
		this.memory = memory;
	}
	public String getHostUuid() {
		return hostUuid;
	}
	public void setHostUuid(String hostUuid) {
		this.hostUuid = hostUuid;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("hostUUID: " + this.hostUuid + "\n");
		sb.append("ip: " + ip);
		sb.append("\n");
		sb.append("memory: " + memory);
		sb.append("\n");
		
		for (SREntity sr : SRs) {
			sb.append(sr.toString());
		}
		return sb.toString();
	}
	public int getVmCount() {
		return vmCount;
	}
	public void setVmCount(int vmCount) {
		this.vmCount = vmCount;
	}
	
}