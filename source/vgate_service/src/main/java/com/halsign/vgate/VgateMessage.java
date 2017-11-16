package com.halsign.vgate;

import com.halsign.vgate.util.VgateMessageConstants;

/**
 * 任务执行成功与否的信息。
 * 
 * 
 * @author 红山世纪
 *
 */
public class VgateMessage extends VgateMessageConstants {
   /**
    * 错误代码
    */
	private int errorCode;
	/**
	 * 错误消息
	 */
	private String errorMsg;
	/**
	 * 状态
	 */
	private int status;
	
	public VgateMessage(int status, int errorCode) {
		this.errorCode = errorCode;
		this.errorMsg = (String) this.getMap().get(errorCode);
		this.status = status;
	}
	
	public VgateMessage(int status) {
		this.status = status;
	}
	
	public VgateMessage(int status, String errorMsg) {
		this.status = status;
		this.errorMsg = errorMsg;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}

	
}
