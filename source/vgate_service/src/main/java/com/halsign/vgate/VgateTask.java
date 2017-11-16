package com.halsign.vgate;

import java.util.Date;

/**
 * 任务
 * @author lijuan
 *
 */
public class VgateTask {
   /*
	CREATE TABLE Task
	(
		taskID               VARCHAR(36) NOT NULL Primary key,
		taskName             VARCHAR(200) NOT NULL, 
		taskType             VARCHAR(100) NULL,
		taskCurStep          VARCHAR(100) NULL, 
		vmUuid                VARCHAR(36) NULL,
		taskStatus               INT(2) null,
		taskStatusDesc           VARCHAR(20) NULL,
		taskProcess          DECIMAL,
		taskCreatedTime      TIMESTAMP NULL,
		taskFinishedTime     TIMESTAMP NULL,
		taskDesc          TEXT NULL,
		taskPriority        INT(2) NULL
	);*/
	/**
     * 唯一标识符
     */
    private String taskID;
    /**
     * 父级任务ID
     */
    private String parentTaskID;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务类型
     */
    private String taskType;
    /**
     * 任务当前步骤
     */
    private String taskCurStep;
    /**
     * 虚拟机uuid/模板uuid
     */
    private String vmUuid;
    /**
     * 任务状态
     */
    private int taskStatus;
    /**
     * 任务状态描述
     */
    private String taskStatusDesc;
    /**
     * 任务处理进度
     */
    public Double taskProgress;
    /**
     * 任务创建时间
     */
    private Date taskCreatedTime;
    /**
     * 任务完成时间
     * 注意：当任务状态处于 succeeded/failed时，任务完成时间才是有效时间。
     * 当任务处于pending状态时，此值无意义。
     */
    private Date taskFinishedTime;
    /**
     * 任务描述
     */
    private String taskDesc;
    /**
     * 任务优先级
     */
    private int taskPriority;

    private int stepIndex;
    
    private int totalSteps;
    
    private String srUuid;
    
    private String resultVmUuid;
    
    private VgateMessage msg;
    
    private String asyncTaskResult;
    
	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	public String getParentTaskID() {
		return parentTaskID;
	}

	public void setParentTaskID(String parentTaskID) {
		this.parentTaskID = parentTaskID;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getTaskCurStep() {
		return taskCurStep;
	}

	public void setTaskCurStep(String taskCurStep) {
		this.taskCurStep = taskCurStep;
	}

	public String getVmUuid() {
		return vmUuid;
	}

	public void setVmUuid(String vmUuid) {
		this.vmUuid = vmUuid;
	}

	public int getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(int taskStatus) {
		this.taskStatus = taskStatus;
	}

	public String getTaskStatusDesc() {
		return taskStatusDesc;
	}

	public void setTaskStatusDesc(String taskStatusDesc) {
		this.taskStatusDesc = taskStatusDesc;
	}

	public Double getTaskProgress() {
		return taskProgress;
	}

	public void setTaskProgress(Double taskProgress) {
		this.taskProgress = taskProgress;
	}

	public Date getTaskCreatedTime() {
		return taskCreatedTime;
	}

	public void setTaskCreatedTime(Date taskCreatedTime) {
		this.taskCreatedTime = taskCreatedTime;
	}

	public Date getTaskFinishedTime() {
		return taskFinishedTime;
	}

	public void setTaskFinishedTime(Date taskFinishedTime) {
		this.taskFinishedTime = taskFinishedTime;
	}

	public String getTaskDesc() {
		return taskDesc;
	}

	public void setTaskDesc(String taskDesc) {
		this.taskDesc = taskDesc;
	}

	public int getTaskPriority() {
		return taskPriority;
	}

	public void setTaskPriority(int taskPriority) {
		this.taskPriority = taskPriority;
	}

	public int getStepIndex() {
		return stepIndex;
	}

	public void setStepIndex(int stepIndex) {
		this.stepIndex = stepIndex;
	}

	public String getSrUuid() {
		return srUuid;
	}

	public void setSrUuid(String srUuid) {
		this.srUuid = srUuid;
	}

	public String getResultVmUuid() {
		return resultVmUuid;
	}

	public void setResultVmUuid(String resultVmUuid) {
		this.resultVmUuid = resultVmUuid;
	}

	public VgateMessage getMsg() {
		return msg;
	}

	public void setMsg(VgateMessage msg) {
		this.msg = msg;
	}

	public int getTotalSteps() {
		return totalSteps;
	}

	public void setTotalSteps(int totalSteps) {
		this.totalSteps = totalSteps;
	}

	public String getAsyncTaskResult() {
		return asyncTaskResult;
	}

	public void setAsyncTaskResult(String asyncTaskResult) {
		this.asyncTaskResult = asyncTaskResult;
	}


}
