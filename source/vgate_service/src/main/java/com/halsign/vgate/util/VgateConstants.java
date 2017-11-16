package com.halsign.vgate.util;

/**
 * 
 * @author lijuan
 *
 */
public class VgateConstants {

	public static final String TYPE_VM = "vm";
	public static final String TYPE_TEMPLATE = "template";
	
	public static final String LOCAL_SR = "local";
	public static final String NFS_SR = "nfs";
					
	public static final String PLUGIN_NAME_CLEAR_VDI = "clear-vdi"; 
	public static final String PLUGIN_FUNCTION_CLEAR_VDI_DATA = "clear_vdi_data";
	
	public static final String PLUGIN_NAME_GET_VDI_SIZE = "get-vdi-size"; 
	public static final String PLUGIN_FUNCTION_GET_VDI_SIZE = "get_vdi_size";
	
	public static final String TASK_PROCESS_TYPE_PUBLISHTEMPLATE="publishtemplate";
	public static final String TASK_PROCESS_TYPE_CRETE_VM_FROM_TEMPLATE="createVmFromTemplate";
	

	public static final int TASK_STATUS_PENDING =0;
	public static final int TASK_STATUS_STARTED =1;
	public static final int TASK_STATUS_FAILED =2;
	public static final int TASK_STATUS_SUCCEED =3;
	public static final int TASK_STATUS_RUNNING =4;
	
	/**
	 * The steps of publishing template.
	 */
	public static final String TASK_STEP_SHUTDOWN_VM ="shut down vm";
	public static final String TASK_STEP_COPY_VM ="copy vm";
	public static final String TASK_STEP_CONVERT_VM_TO_TEMPLATE ="convert vm to template";
	public static final String TASK_STEP_COPY_TEMPLATE_TO_NFSSR ="copy template to nfssr";
	public static final String TASK_STEP_ADD_TEMPLATE_TO_DB ="add template to db";
	
	public static final String TASK_STEP_GET_TEMPLATE_FROM_NFSSR = "get template from nfssr";
	public static final String TASK_STEP_CHOOSE_A_BETTER_HOST = "choose a better host";
	public static final String TASK_STEP_DOWNLOAD_TEMPLATE_TO_LOCAL ="download template to local";
	public static final String TASK_STEP_CREATE_VM_FROM_TEMPLATE ="create vm from template";
	
	public static final int OPERATE_TYPE_PUBLISH_TEMPLATE = 0;
	public static final int OPERATE_TYPE_CREATE_VM = 1;
	
	public static final int OPERATE_TYPE_DIRECTION_TEMPLATE_TO_TEMPLATE = 0;
	public static final int OPERATE_TYPE_DIRECTION_TEMPLATE_TO_VM = 1;
	
	public static final String SR_TYPE_SSD = "speedup_storage";
	
	public static long MEMORYBUFFER = 48L * 1024 * 1024;
	
	public static long DISKBUFFER = 400L * 1024 * 1024;
	
	public static long BINARY_KILO = 1024;
    public static long BINARY_MEGA = BINARY_KILO * BINARY_KILO;
    public static long BINARY_GIGA = BINARY_KILO * BINARY_KILO * BINARY_KILO;
	
	
}
