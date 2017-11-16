package com.halsign.vgate.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.xmlrpc.XmlRpcException;

import com.halsign.vgate.BalancePolicy;
import com.halsign.vgate.HalsignCallBack;
import com.halsign.vgate.Policy;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.service.TemplateService;
import com.halsign.vgate.service.impl.TemplateServiceImpl;
import com.halsign.vgate.service.impl.VmServiceImpl;
import com.halsign.vgate.spec.TemplateSpec;
import com.halsign.vgate.spec.VmSpec;
import com.halsign.vgate.thread.Worker;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String args[]) throws VgateException, ExecutionException, BadServerResponse, XenAPIException, XmlRpcException, InterruptedException {
		String dbUser = "root";
		String dbPassword = "qwer1234";
		String dbUrl = "jdbc:mysql://192.166.30.241/vgatedb";
		MysqlDataSource mysqlDS = new MysqlDataSource();
		mysqlDS.setURL(dbUrl);
		mysqlDS.setUser(dbUser);
		mysqlDS.setPassword(dbPassword);
		DAOManager.setDataSource(mysqlDS);
		HalsignCallBack callback = new HalsignCallBack(){
			@Override
			public boolean onTemplatePublishSuccess(VgateConnection connection,
					String templateUuid, String hostUuid) {
				// TODO Auto-generated method stub
				System.out.println("onTemplatePublishSuccess " + templateUuid + " : " + hostUuid);
				return false;
			}
			@Override
			public boolean onTemplatePublishFail(VgateConnection connection,
					int errorCode, String templateUuid, String hostUuid) {
				// TODO Auto-generated method stub
				System.out.println("onTemplatePublishFail " + templateUuid + " : " + hostUuid);
				return false;
			}
			@Override
			public boolean onVmCreateSuccess(VgateConnection connection,
					String hostUUID, Properties aliParam, VM vm) {
				// TODO Auto-generated method stub
				System.out.println("onVmCreateSuccess " + aliParam + " : " + hostUUID);
				return false;
			}
			@Override
			public boolean onVmCreateFail(VgateConnection connection,
					String hostUUID, int errorCode, Properties aliParam) {
				// TODO Auto-generated method stub
				System.out.println("onVmCreateFail " + aliParam + " : " + hostUUID);
				return false;
			}};
		Worker.start(callback);
		//VgateConnection con1 = VgateConnectionPool.getInstance().connect("192.166.30.6", "root", "aliursys", 200, 5);
		VgateConnection con3 = VgateConnectionPool.getInstance().connect("192.166.30.230", "root", "aliursys", 200, 5);
		List<VgateConnection> connections = new ArrayList<VgateConnection>();
		//connections.add(con1);
		connections.add(con3);
		
		
		TemplateSpec spec = new TemplateSpec();
		spec.setSystemVdiTag("ssd");
		spec.setUserVdiTag("ssd");
		
		
		TemplateService service = new TemplateServiceImpl();
		String vmuuid = "f910e91d-4c4a-2962-5456-3651aeaa0b5a";
//		VgateTask publishTemplateAsync = service.publishTemplateAsync(connections, vmuuid, spec);
//		String templateUuid = publishTemplateAsync.getResultVmUuid();
//		System.out.println("---------------------------------------------------" + templateUuid);
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		connections.remove(con3);
//		service.updateTemplate(connections, templateUuid, spec);
		
		
		
		VmServiceImpl createservice = new VmServiceImpl();
		Properties proper = new Properties();
		proper.setProperty("test", "halsign");
		VmSpec vmSpec = new VmSpec();
		BalancePolicy policy = new BalancePolicy();
		policy.setPolicy(Policy.LEAST_VM_COUNT_WITH_MEMORY);
		vmSpec.setBalancePolicy(policy);
		createservice.createVmFromTemplateAsync(connections, "1a1fadab-047d-cfc4-431f-295a1390349a", vmSpec, 4, proper);
		
		
		
		// at some point at the end
		//executor.shutdown();
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//		    public void run() {
//		    	executor.shutdown();
//		        try {
//		        	executor.awaitTermination(5, TimeUnit.SECONDS);
//		        } catch (InterruptedException e) {
//		        }
//		    }
//		});
		
		
	}

}
