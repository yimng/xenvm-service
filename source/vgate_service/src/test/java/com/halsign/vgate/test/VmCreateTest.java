package com.halsign.vgate.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.halsign.vgate.BalancePolicy;
import com.halsign.vgate.Policy;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.service.impl.VmServiceImpl;
import com.halsign.vgate.spec.VmSpec;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;

public class VmCreateTest {

	private static String connectionName = "192.166.20.205";
	protected static Connection connection;
	protected static List<VgateConnection> connections = new ArrayList<VgateConnection>();
	protected static VmSpec spec = new VmSpec(); 
	
	protected static String dbUser = "";
	protected static String dbPassword = "";
	protected static String dbUrl = "";
	protected static String driverClass = "com.mysql.jdbc.Driver";
	
	protected static void connect() throws Exception
    {
        connection = new Connection(new URL("http://" + connectionName));
        System.out.println("connect");
        Session.loginWithPassword(connection, "root", "aliursys", APIVersion.latest().toString());
    }

    protected static void disconnect() throws Exception
    {
    	System.out.println("disconnect");
        Session.logout(connection);
    }

	public static void main(String[] args) throws Exception {

//		connect();

//		Set<Pool> pools = Pool.getAll(connection);
//		for (Pool pool : pools) {
//			String uuid = pool.getUuid(connection);
//			System.out.println("uuid ==================> " + uuid);
//			
//			pool.setNameLabel(connection, "test");
//			pool.setNameDescription(connection, "test 222");
//		}
		
//		Pool.join(connection, "192.166.20.205", "root", "aliursys");
		
		parseConfigurationFile(args[0]);
		parseVmSpec(args[1]);
		parseDatabaseParams(args[2]);
		VmServiceImpl service  = new VmServiceImpl();
		
		String goldTemplateUuid = args[3];
		int vmNum = Integer.parseInt(args[4]);
//		HalsignCache.getInstance().initializeCaches(connections);
		System.out.println("The gold template uuid is ==> " + goldTemplateUuid);
		
		
		
		Properties p = new Properties();
		p.setProperty("test", "halsign");
		service.createVmFromTemplateAsync(connections, goldTemplateUuid, spec, vmNum, p);
		
//		disconnect();

	}
	
	private static void parseVmSpec(String file) throws Exception {
		try {

			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);

			String str = null;

			while ((str = br.readLine()) != null) {
				String[] strs = str.split(" ");
				BalancePolicy balancePolicy = new BalancePolicy();
				
				if (Integer.parseInt(strs[0]) == 1) {
					balancePolicy.setPolicy(Policy.LEAST_VM_COUNT);
				} else if (Integer.parseInt(strs[0]) == 2) {
					balancePolicy.setPolicy(Policy.LEAST_VM_COUNT_WITH_MEMORY);
				} else if (Integer.parseInt(strs[0]) == 3) {
					balancePolicy.setPolicy(Policy.MAX_HOST_DENSITY);
				} else  {
					balancePolicy.setPolicy(Policy.SPECIFIED_HOST);
				}
				
				int po = Integer.parseInt(strs[0]);
				if ((po != 1) && (po != 2) && (po != 3)) {
					balancePolicy.setSpecifiedHost(strs[1]);
					spec.setBalancePolicy(balancePolicy);
					spec.setCpuCount(Integer.parseInt(strs[2]));
					spec.setSysDiskSize(Integer.parseInt(strs[3]));
					spec.setDataDiskSize(Integer.parseInt(strs[4]));
					spec.setVmMemory(Integer.parseInt(strs[5]));
					spec.setSocketCount(Integer.parseInt(strs[6]));
				} else {
					spec.setBalancePolicy(balancePolicy);
					spec.setCpuCount(Integer.parseInt(strs[1]));
					spec.setSysDiskSize(Integer.parseInt(strs[2]));
					spec.setDataDiskSize(Integer.parseInt(strs[3]));
					spec.setVmMemory(Integer.parseInt(strs[4]));
					spec.setSocketCount(Integer.parseInt(strs[5]));
					
				}
				
				
			}

			br.close();
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private static void parseDatabaseParams(String file) {
		try {

			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);

			String str = null;

			while ((str = br.readLine()) != null) {
				String[] strs = str.split(" ");
				dbUser = strs[0];
				dbPassword = strs[1];
				dbUrl = strs[2];
				
				MysqlDataSource mysqlDS = new MysqlDataSource();
				mysqlDS.setURL(dbUrl);
				mysqlDS.setUser(dbUser);
				mysqlDS.setPassword(dbPassword);
				DAOManager.setDataSource(mysqlDS);
			}

			br.close();
			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void parseConfigurationFile(String file) throws Exception {

		try {
			// read file content from file
			StringBuffer sb = new StringBuffer("");

//			String file1 = args[0];

			// FileReader reader = new FileReader("d://BugReport.txt");
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);

			String str = null;

			while ((str = br.readLine()) != null) {
//				sb.append(str + "/n");

				String[] strs = str.split(" ");
				System.out.println(strs[0]);
				System.out.println(strs[1]);
				System.out.println(strs[2]);
				
				VgateConnection con = VgateConnectionPool.getInstance().connect(strs[0], strs[1], strs[2], 1, 1);
				connections.add(con);
			}

			br.close();
			reader.close();

			// write string to file

			// String file2 = args[1];
			// // FileWriter writer = new FileWriter("d://BugReport2.txt");
			// FileWriter writer = new FileWriter(file2);
			//
			// BufferedWriter bw = new BufferedWriter(writer);
			// bw.write(sb.toString());
			//
			// bw.close();
			// writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	

}
