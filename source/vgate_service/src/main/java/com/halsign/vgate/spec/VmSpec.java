package com.halsign.vgate.spec;

import com.halsign.vgate.BalancePolicy;

public class VmSpec {

	private long vmMemory;
	
	private long sysDiskSize;
	
	private long dataDiskSize;
	
	private int socketCount;
	
	private int cpuCount;
	
	public long getSysDiskSize() {
		return sysDiskSize;
	}

	public void setSysDiskSize(int sysDiskSize) {
		this.sysDiskSize = sysDiskSize;
	}

	public long getDataDiskSize() {
		return dataDiskSize;
	}

	public void setDataDiskSize(int dataDiskSize) {
		this.dataDiskSize = dataDiskSize;
	}

	public int getSocketCount() {
		return socketCount;
	}

	public void setSocketCount(int socketCount) {
		this.socketCount = socketCount;
	}

	public int getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(int cpuCount) {
		this.cpuCount = cpuCount;
	}

	private BalancePolicy balancePolicy;

	public long getVmMemory() {
		return vmMemory;
	}

	public void setVmMemory(int vmMemory) {
		this.vmMemory = vmMemory;
	}

	public BalancePolicy getBalancePolicy() {
		return balancePolicy;
	}

	public void setBalancePolicy(BalancePolicy balancePolicy) {
		this.balancePolicy = balancePolicy;
	}
	
	
}
