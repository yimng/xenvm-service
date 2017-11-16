package com.halsign.vgate;

public class PublishTask {

	private String task_ID;
	private String host_UUID;
	private String host_IP;
	private String host_username;
	private String host_password;
	private TaskStatus task_status;
	private String source_UUID;
	private String gold_UUID;
	private int retry_count;
	private String template_system_vdi_tag = "";
	private String template_user_vdi_tag = "";
	private String template_vif_tag = "";
	
	public String getTemplate_system_vdi_tag() {
		return template_system_vdi_tag;
	}
	public void setTemplate_system_vdi_tag(String template_system_vdi_tag) {
		this.template_system_vdi_tag = template_system_vdi_tag;
	}
	public String getTemplate_user_vdi_tag() {
		return template_user_vdi_tag;
	}
	public void setTemplate_user_vdi_tag(String template_user_vdi_tag) {
		this.template_user_vdi_tag = template_user_vdi_tag;
	}
	public String getTask_ID() {
		return task_ID;
	}
	public String getHost_UUID() {
		return host_UUID;
	}
	public void setHost_UUID(String host_UUID) {
		this.host_UUID = host_UUID;
	}
	public void setTask_ID(String task_ID) {
		this.task_ID = task_ID;
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
	public String getGold_UUID() {
		return gold_UUID;
	}
	public void setGold_UUID(String gold_UUID) {
		this.gold_UUID = gold_UUID;
	}
	public String getSource_UUID() {
		return source_UUID;
	}
	public void setSource_UUID(String source_UUID) {
		this.source_UUID = source_UUID;
	}
	public void setHost_password(String host_password) {
		this.host_password = host_password;
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
	public String getTemplate_vif_tag() {
		return template_vif_tag;
	}
	public void setTemplate_vif_tag(String template_vif_tag) {
		this.template_vif_tag = template_vif_tag;
	}
}
