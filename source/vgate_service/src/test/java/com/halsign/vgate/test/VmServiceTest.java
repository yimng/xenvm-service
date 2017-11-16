package com.halsign.vgate.test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import com.halsign.vgate.VgateException;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

/**
 * 
 * @author lijuan
 *
 */
public class VmServiceTest {

	private static String connectionName = "192.168.1.111";
	protected static Connection connection;
	
	protected static void connect() throws Exception
    {
        connection = new Connection(new URL("http://" + connectionName));
        System.out.println("connect");
        Session.loginWithPassword(connection, "root", "qwer1234", APIVersion.latest().toString());
    }

    protected static void disconnect() throws Exception
    {
    	System.out.println("disconnect");
        Session.logout(connection);
    }
	
    //platform (MRW): timeoffset: -1; acpi: 1; apic: true; pae: true; vga: std; videoram: 8; nx: true; viridian: false; device_id: 0001; cores-per-socket: 1

    
    public static void updatedPlatformForVm(String vmUuid) throws BadServerResponse, XenAPIException, XmlRpcException {
    	VM vm = VM.getByUuid(connection, vmUuid);
    	if(vm != null) {
    		Map<String, String> platformMap = vm.getPlatform(connection);
        	if(platformMap == null) {
        		platformMap = new HashMap<String, String>();
        	}
        
        	String vgaValue = platformMap.get("vga");
        	if(vgaValue == null) {
        		vm.addToPlatform(connection, "vga", "std");
        	} else {
        		if(!"std".equals(vgaValue)) {
        			platformMap.put("vga", "std");
        			vm.setPlatform(connection, platformMap);
        		}
        	}
      
        	String videoramValue = platformMap.get("videoram");
        	if(videoramValue == null) {
        		vm.addToPlatform(connection, "videoram", "8");
        	} else {
        		if(!"8".equals(videoramValue)) {
        			platformMap.put("videoram", "8");
        			vm.setPlatform(connection, platformMap);
        		}
        	}
    	}
    	
    }


    public static void main(String[] args) throws VgateException, Exception {
    	VmServiceTest.connect();
    	
		String vmUuid = "bb795190-b0e9-359e-7057-84365fa223d5";
		/*VM vm = VM.getByUuid(connection, vmUuid);
		if(vm == null) return;*/
		
		//To set the number of VCPU
		//vm.setVCPUsMax(connection, 1l);
		//vm.setVCPUsAtStartup(connection, 1l);
		
		//To set the max memory
		//vm.setMemoryStaticMax(connection, 268435456l);
		
		updatedPlatformForVm(vmUuid);
		
		
		VmServiceTest.disconnect();
    }
}
