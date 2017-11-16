package com.halsign.vgate.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.IpAddress;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.service.NetworkService;
import com.halsign.vgate.service.VmService;
import com.halsign.vgate.service.impl.NetworkServiceImp;
import com.halsign.vgate.service.impl.VmServiceImpl;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

/**
 * 
 * @author lijuan
 *
 */
public class ServiceTest {

	final static Logger logger = LoggerFactory.getLogger(ServiceTest.class);
	
    public static void disconnect(VgateConnection connection) throws Exception
    {
    	logger.debug("disconnect...");
        Session.logout(connection);
    }
    
    
    public static void testListVmByTemplate(VgateConnection connection, String templateUuid) throws BadServerResponse, XenAPIException, XmlRpcException, VgateException {
    	VmService vs = new VmServiceImpl();

		List<VM> vmList = vs.listVmByTemplateUuid(connection, templateUuid);
		if(vmList == null || (vmList != null && vmList.size() == 0)) {
			logger.debug("there isn't any vm created by the template");
			return;
		}
		logger.debug("The size of vm is  : "+vmList.size());
		
		for(int i = 0; i < vmList.size(); i++) {
			VM curVm = vmList.get(i);
			logger.debug("--------- " + i + ":");
			System.out.print("vm uuid=" + curVm.getUuid(connection));
			logger.debug(", vm name=" + curVm.getNameLabel(connection));
		}
    	
    }
    
    public static void testrevertVmToTemplateAsync(VgateConnection connection, String vmUuid, String templateUuid) throws VgateException {
    	VmService vs = new VmServiceImpl();
    	logger.debug("-----------------------------start test revertVmToTemplateAsync");
    	vs.revertVmToTemplateAsync(connection, vmUuid, templateUuid);
    	logger.debug("-----------------------------end test revertVmToTemplateAsync");
    }
    
    public static void testrevertDiskToTemplateAsync(VgateConnection connection, String vmUuid, String templateUuid, String vdiUuid) throws VgateException {
    	VmService vs = new VmServiceImpl();
    	logger.debug("-----------------------------start test revertVmToTemplateAsync");
    	vs.revertVmDiskToTemplateAsync(connection, vmUuid, templateUuid, vdiUuid);
    	logger.debug("-----------------------------end test revertVmToTemplateAsync");
    }
  

    public static void testSetHostName(VgateConnection connection, String uuid, String hostName) {
		NetworkService ns = new NetworkServiceImp();
		logger.debug("Try to set a new name for vm ... ");
		ns.setVmHostName(connection, uuid, hostName);
		logger.debug("operation is success , new name is " + hostName);
	}
	
	public static void testSetVmIp(VgateConnection connection, String uuid, IpAddress ip) {
		NetworkService ns = new NetworkServiceImp();
		logger.debug("new ip address is" + ip.getIpAddr());
		ns.setVmIp(connection, uuid, ip, true);
	}

	protected static int waitForTaskComplete(VgateTask task) {
        int status = 0;

        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            status = task.getTaskStatus();
        } while (status == VgateConstants.TASK_STATUS_PENDING 
                || status == VgateConstants.TASK_STATUS_RUNNING
                || status == VgateConstants.TASK_STATUS_STARTED);

        return status;
    }
    
    public static void main(String[] args) throws VgateException {

		
    	Map<String, String> a = new HashMap<String, String>();
    	a.put("a", "1");
    	a.put("b", "2");
    	List<String> b = new ArrayList<String>();
    	b.add("a");
    	b.add("b");
    	b.add("c");
    	b.removeAll(a.keySet());
    	System.out.println(b);
				
		
		
    }
}
