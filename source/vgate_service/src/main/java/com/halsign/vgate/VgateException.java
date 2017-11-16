package com.halsign.vgate;
/**
 *异常处理
 *
 * @author 红山世纪
 *
 */
public class VgateException extends Exception {
	
	
	/**
	 * 错误代码
	 */
	private int errorCode;
	/**
	 * 错误消息
	 */
	private String errorMsg;
	
	/**
	 * 异常处理
	 */
	private Throwable throwable;
	
	public VgateException(int errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
	
	public VgateException(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public VgateException(String errorMsg, Throwable throwable) {
		this.errorMsg = errorMsg;
		this.throwable = throwable;
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

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
	
}
