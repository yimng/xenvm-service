package com.halsign.vgate;

public class CreateVMTask {
	private String task_ID;
	private String gold_UUID;
	private String host_UUID;
	private String host_IP;
	private String host_username;
	private String host_password;
	private int count;
	private TaskStatus task_status;
	private int retry_count;
	private long vm_memory;
	private long sys_disk_size;
	private long data_disk_size;
	private int socket_count;
	private int cpu_count;
	private Policy policy;
	private String callBackId;
	
	public String getTask_ID() {
		return task_ID;
	}
	public void setTask_ID(String task_ID) {
		this.task_ID = task_ID;
	}
	public String getHost_UUID() {
		return host_UUID;
	}
	public void setHost_UUID(String host_UUID) {
		this.host_UUID = host_UUID;
	}
	public String getHost_IP() {
		return host_IP;
	}
	public void setHost_IP(String host_IP) {
		this.host_IP = host_IP;
	}
	public String getHost_username() {
		return host_username;
	}
	public void setHost_username(String host_username) {
		this.host_username = host_username;
	}
	public String getHost_password() {
		return host_password;
	}
	public void setHost_password(String host_password) {
		this.host_password = host_password;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public TaskStatus getTask_status() {
		return task_status;
	}
	public void setTask_status(TaskStatus task_status) {
		this.task_status = task_status;
	}
	public int getRetry_count() {
		return retry_count;
	}
	public void setRetry_count(int retry_count) {
		this.retry_count = retry_count;
	}
	public long getVm_memory() {
		return vm_memory;
	}
	public void setVm_memory(long vm_memory) {
		this.vm_memory = vm_memory;
	}
	public long getSys_disk_size() {
		return sys_disk_size;
	}
	public void setSys_disk_size(long sys_disk_size) {
		this.sys_disk_size = sys_disk_size;
	}
	public long getData_disk_size() {
		return data_disk_size;
	}
	public void setData_disk_size(long data_disk_size) {
		this.data_disk_size = data_disk_size;
	}
	public int getSocket_count() {
		return socket_count;
	}
	public void setSocket_count(int socket_count) {
		this.socket_count = socket_count;
	}
	public int getCpu_count() {
		return cpu_count;
	}
	public void setCpu_count(int cpu_count) {
		this.cpu_count = cpu_count;
	}
	public Policy getPolicy() {
		return policy;
	}
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}
	public String getGold_UUID() {
		return gold_UUID;
	}
	public void setGold_UUID(String gold_UUID) {
		this.gold_UUID = gold_UUID;
	}
	public String getCallBackId() {
		return callBackId;
	}
	public void setCallBackId(String callBackId) {
		this.callBackId = callBackId;
	}

}
