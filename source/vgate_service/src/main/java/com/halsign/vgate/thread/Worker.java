package com.halsign.vgate.thread;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.CreateVMTask;
import com.halsign.vgate.HalsignCallBack;
import com.halsign.vgate.PublishTask;
import com.halsign.vgate.TaskStatus;
import com.halsign.vgate.Template;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.DAO.CallBackParamDAO;
import com.halsign.vgate.DAO.CreateVMTaskDAO;
import com.halsign.vgate.DAO.DAOManager;
import com.halsign.vgate.DAO.PublishTaskDAO;
import com.halsign.vgate.DAO.TemplateDAO;
import com.halsign.vgate.spec.TemplateSpec;
import com.halsign.vgate.util.Table;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

public class Worker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Worker.class);
	static DAOManager daoManager = DAOManager.getInstance();
	private HalsignCallBack callback;
	private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private static ScheduledFuture<?> f;
	public static volatile boolean start = true;

	public Worker(HalsignCallBack callback) {
		this.callback = callback;
	}

	@Override
	public void run() {
		try {
			PublishTaskDAO publishDao = ((PublishTaskDAO)daoManager.getDAO(Table.PUBLISHTASK));
			CreateVMTaskDAO createDao = ((CreateVMTaskDAO)daoManager.getDAO(Table.CREATEVMTASK));
			//get all pending publish template task
			List<PublishTask> publishTasks = publishDao.query();
			for (PublishTask task : publishTasks) {
				logger.info("----------------Start Publish Template Polling -----------------");
				String ip = task.getHost_IP();
				if (!VgateUtil.isHostAlive(ip)) {
					logger.info("This host is not online: " + task.getHost_UUID());
					continue;
				}
				// find a source to download
				Template source = findSource(task.getGold_UUID(), task.getHost_UUID());
				//logger.debug("The found source is: " + source.getHost_IP());
				if (source != null) {
					// new thread to migrate or copy
					// update the task's source
					task.setSource_UUID(source.getUUID());
					// update the task status to processing
					publishDao.updateTaskSource(task.getTask_ID(), source.getUUID());
					Thread publish = new Thread(new PublishThread(callback, task, source));
					logger.info("start publish template thread");
					publish.start();
				} else {
					logger.info("Waiting for publish: " + task.getHost_UUID());
				}
			}
			//get all pending create vm task
			List<CreateVMTask> createVMTasks = createDao.query();
			for (CreateVMTask task : createVMTasks) {
				VgateConnection connect = null;
				try {
					logger.info("----------------Start Create VM Polling -----------------");
					connect = VgateConnectionPool.getInstance().connect(task.getHost_IP(), task.getHost_username(), task.getHost_password(), 600, 5);
					String hostUUID = task.getHost_UUID();
					String goldUUID = task.getGold_UUID();
					String ip = task.getHost_IP();
					Properties callBackParam = new Properties();
					Reader reader = ((CallBackParamDAO) daoManager.getDAO(Table.CALLBACKPARAM)).get(task.getCallBackId());
					callBackParam.load(reader);
					if (!VgateUtil.isHostAlive(ip)) {
						logger.error("This host is not online: " + task.getHost_UUID());
						this.callback.onVmCreateFail(connect, hostUUID, VgateMessageConstants.ERROR_CODE_HOST_NOT_ALIVE, callBackParam);
						continue;
					}
					// get local or share template to create vm
					String template = VgateUtil.findTemplate(hostUUID, goldUUID);
					if (template != null) {
						//int tasknum = publishDao.countTaskBySource(template);
						//TODO - check export status, when exporting cannot create vm and publish to other host
						//if (tasknum >= 1) {
						//	logger.info("This template is publishing, please wait for a while: " + template);
						//	continue;
						//}
						createDao.updateTaskStatus(task.getTask_ID(), TaskStatus.PROCESS.ordinal());
						logger.info("start create vm thread");
						new Thread(new CreateVMThread(callback, task, template)).start();
					} else {
						Host masterHost = VgateUtil.getMasterHostFromPool(connect);
						String masterUUID = masterHost.getUuid(connect);
						int localDowning = publishDao.countTaskDownloading(hostUUID, goldUUID);
						int sharedDowning = publishDao.countTaskDownloading(masterUUID, goldUUID);
						int publishFailed = publishDao.countTaskPublishFailed(hostUUID, goldUUID);
						// not published
						if (localDowning + sharedDowning == 0) {
							if (publishFailed > 0) {
								// we failed to create vm at last
								logger.info("Can't create VM, the host publish template failed before!!" + task.getGold_UUID());
								this.callback.onVmCreateFail(connect, hostUUID, VgateMessageConstants.ERROR_CODE_TEMPLATE_NOT_EXIST, callBackParam);
								createDao.updateTaskStatus(task.getTask_ID(), TaskStatus.FAILED.ordinal());
							} else {
								logger.info("Assign a publish task on host: " + task.getHost_UUID());
								// initial a publish task
								TemplateSpec spec = publishDao.getTemplateSpecByGoldTemplate(goldUUID);
								VgateUtil.initPublishTask(spec, goldUUID, connect.getIp(), connect.getUsername(), connect.getPassword(), hostUUID);
							}
						// publishing
						} else if (localDowning + sharedDowning > 0) {
							logger.info("Waiting to create vm: " + hostUUID);
						}
					}
				} catch (VgateException e) {
					logger.error("", e);
				} catch (XmlRpcException e) {
					logger.error("", e);
				} catch (BadServerResponse e) {
					logger.error("", e);
				} catch (XenAPIException e) {
				} catch (IOException e) {
					logger.error("", e);
				} finally {
					VgateUtil.disconnect(connect);
				}
			}
		} catch (SQLException e) {
			logger.error("", e);
		} finally {
			daoManager.close();
		}
	}
	
	private Template findSource(String goldUUID, String hostUUID) throws SQLException {
		//TODO: we should find the source from same pool first, if can't get the source from same pool
		// we get source cross pool
		List<Template> templates = ((TemplateDAO)daoManager.getDAO(Table.TEMPLATE)).findTemplatesByGoldTemplate(goldUUID);
		
		for (Template tmp :templates) {
			String ip = tmp.getHost_IP();
			if (!VgateUtil.isHostAlive(ip)) {
				logger.info("This source host is not online: " + ip);
				continue;
			}
			String UUID = tmp.getUUID();
			int tasknum = ((PublishTaskDAO)daoManager.getDAO(Table.PUBLISHTASK)).countTaskBySource(UUID);
			if (tasknum >= 1) {
				logger.info("This host already has " + tasknum + " download task: " + ip);
				continue;
			}
			return tmp;
		}
		return null;
	}
	
	public static void start(final HalsignCallBack hasignCallBack) {
		logger.info("Starting Worker...");
		// clean
		try {
			((PublishTaskDAO)daoManager.getDAO(Table.PUBLISHTASK)).updateTaskStatus();
		} catch (SQLException e) {
			logger.info("", e);
		}
		service.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				if (start) {
					start = false;
					if (f == null) {
						f = executor.scheduleAtFixedRate(new Worker(
								hasignCallBack), 0, 60, TimeUnit.SECONDS);
					} else {
						f.cancel(false);
						if (f.isCancelled()) {
							f = executor.scheduleAtFixedRate(new Worker(
									hasignCallBack), 0, 60, TimeUnit.SECONDS);
						}
					}
				}
				
			}}, 0, 5, TimeUnit.SECONDS);
	}
	
	public static void stop() {
		logger.info("Stopping Worker...");
		service.shutdown();
		executor.shutdown();
	}
}
