package com.halsign.vgate.cache.entity;

public class VmCacheEntity {

	private String vmUuid;
	
	private String vmAffinity;
	private long memory;
	
	public String getVmUuid() {
		return vmUuid;
	}
	public void setVmUuid(String vmUuid) {
		this.vmUuid = vmUuid;
	}
	public String getVmAffinity() {
		return vmAffinity;
	}
	public void setVmAffinity(String vmAffinity) {
		this.vmAffinity = vmAffinity;
	}
	public long getMemory() {
		return memory;
	}
	public void setMemory(long memory) {
		this.memory = memory;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("vmuuid:" + vmUuid + "\n");
		sb.append("vmAffinity: " + vmAffinity + "\n");
		sb.append("memory: " + memory + "\n");
		return sb.toString();
	}
}
