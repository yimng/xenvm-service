package com.halsign.vgate.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.VgateTask;
import com.halsign.vgate.service.HostService;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateConstants;
import com.halsign.vgate.util.VgateMessageConstants;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostMetrics;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Types.XenAPIException;

public class HostServiceImpl implements HostService {

	private static final Logger logger = LoggerFactory
			.getLogger(HostServiceImpl.class);

	@Override
	public VgateTask patchUpgradeHost(VgateConnection con, Host host,
			String patchFile) {
		String toDirectory = "/tmp";
		VgateTask task = new VgateTask();
		try {
			String ip = host.getAddress(con);
			String user = con.getUsername();
			String pass = con.getPassword();
			scp(patchFile, toDirectory, ip, user, pass);
			Map<String, String> args = new HashMap<String, String>();
			args.put("md5", MD5CheckSum(patchFile));
			args.put("filename", FilenameUtils.getBaseName(patchFile));
			args.put("toDirectory", toDirectory);
			String ret = host.callPlugin(con, "halsign_host_upgrade.py",
					"main", args);
			logger.debug("Host patch upgrade plugin halsign_host_upgrad.py return=====>>>>"
					+ ret);
			if ("true".equals(ret.toLowerCase())) {
				task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
				return task;
			} else if ("1".equals(ret)) {
				//need reboot server
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_NEED_REBOOT);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("10".equals(ret)) {
				//pre-install fail
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_NEED_REBOOT);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
				
			} else if ("11".equals(ret)) {
				//failed at update-pkg
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_UPDATEPACKAGE);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("12".equals(ret)) {
				//failed at remove-pkg
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_REMOVEPACKAGE);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("13".equals(ret)) {
				//failed at post-install
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_POST_INSTALL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1001".equals(ret)) {
				//1001 md5 check failure
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_MD5_CHECKFAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1002".equals(ret)) {
				//1002 tar faile
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_TAR_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1003".equals(ret)) {
				//1003 filename is empty
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_FILENAME_EMPTY_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1004".equals(ret)) {
				//1004 toDirectory is empty
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_TODIR_EMPTY_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1005".equals(ret)) {
				//1005 Installation file 'install_patch'  No found
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_INTALL_SHELL_NOT_FOUDN_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1006".equals(ret)) {
				//1006 Installation package  No found
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_INTALL_PACH_NOT_FOUND_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

		} catch (XenAPIException | XmlRpcException e) {
			logger.error("patchUpgradeHost", e);
		}
		return task;
	}

	@Override
	public VgateTask patchRevertHost(VgateConnection con, Host host,
			String patchFile) {
		String toDirectory = "/tmp";
		VgateTask task = new VgateTask();
		try {
			Map<String, String> args = new HashMap<String, String>();
			args.put("filename", FilenameUtils.getBaseName(patchFile));
			args.put("toDirectory", toDirectory);
			String ret = host.callPlugin(con, "halsign_host_upgrade.py",
					"revert", args);
			logger.debug("Host patch upgrade plugin halsign_host_upgrad.py return=====>>>>"
					+ ret);
			if ("true".equals(ret.toLowerCase())) {
				task.setTaskStatus(VgateConstants.TASK_STATUS_SUCCEED);
				return task;
			} else if ("1".equals(ret)) {
				//need reboot server
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_NEED_REBOOT);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("10".equals(ret)) {
				//pre-install fail
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_NEED_REBOOT);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
				
			} else if ("11".equals(ret)) {
				//failed at update-pkg
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_UPDATEPACKAGE);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("12".equals(ret)) {
				//failed at remove-pkg
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_REMOVEPACKAGE);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("13".equals(ret)) {
				//failed at post-install
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_POST_INSTALL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1001".equals(ret)) {
				//1001 md5 check failure
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_MD5_CHECKFAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1002".equals(ret)) {
				//1002 tar faile
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_TAR_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1003".equals(ret)) {
				//1003 filename is empty
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_FILENAME_EMPTY_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1004".equals(ret)) {
				//1004 toDirectory is empty
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_TODIR_EMPTY_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1005".equals(ret)) {
				//1005 Installation file 'install_patch'  No found
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_INTALL_SHELL_NOT_FOUDN_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			} else if ("1006".equals(ret)) {
				//1006 Installation package  No found
				VgateMessage message = new VgateMessage(VgateMessageConstants.STATUS_FAILURE, VgateMessageConstants.ERROR_CODE_UPGRADE_INTALL_PACH_NOT_FOUND_FAIL);
				task.setMsg(message);
				task.setTaskStatus(VgateConstants.TASK_STATUS_FAILED);
				return task;
			}

		} catch (XenAPIException | XmlRpcException e) {
			logger.error("patchUpgradeHost", e);
		}
		return task;
	}	

	/**
	private void exec(String command, String ip, String username,
			String password) {
		JSch jsch = new JSch();
		try {
			Session session = jsch.getSession(username, ip, 22);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(password);
			session.connect();

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					logger.debug(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					logger.debug("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			session.disconnect();
		} catch (JSchException | IOException e) {
			logger.error("exec command error", e);
		}

	}
	**/
	
	private void scp(String src, String dest, String ip, String username,
			String password) {
		FileInputStream fis = null;
		try {

			JSch jsch = new JSch();
			Session session = jsch.getSession(username, ip, 22);

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(password);
			session.connect();

			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + dest;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				return;
			}

			File _lfile = new File(src);

			if (ptimestamp) {
				command = "T " + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					return;
				}
			}

			// send "C0644 filesize filename", where filename should not include
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			command += new File(src).getName();
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				return;
			}

			// send a content of src
			fis = new FileInputStream(src);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
			}
			out.close();

			channel.disconnect();
			session.disconnect();

		} catch (Exception e) {
			logger.error("scp file error", e);
		}
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				logger.info("-------------------" + sb.toString());
			}
			if (b == 2) { // fatal error
				logger.info("-------------------" + sb.toString());
			}
		}
		return b;
	}

	private String MD5CheckSum(String file) {
		MessageDigest md = null;
		FileInputStream fis = null;
		try {
			md = MessageDigest.getInstance("MD5");
			fis = new FileInputStream(file);
			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			byte[] mdbytes = md.digest();

			// convert the byte to hex format method 1
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
						.substring(1));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			logger.error("MD5CheckSum error", e);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
		return "";
	}

	@Override
	public void recoverVgateSlaves(String masterIP, String username, String password) {
			try {
				Connection con = new Connection(new URL("http://" + masterIP));
				com.xensource.xenapi.Session.loginWithPassword(con, username, password);
				Set<Host> hosts = Host.getAll(con);
				boolean recover = false;
				for (Host h : hosts) {
					boolean reachable = false;
					HostMetrics metrics = h.getMetrics(con);
					InetAddress inet = InetAddress.getByName(h.getAddress(con));
					if (isWindows()) {
						Process p1 = java.lang.Runtime.getRuntime().exec("ping -n 1 " + h.getAddress(con));
						int returnVal;
						try {
							returnVal = p1.waitFor();
							reachable = (returnVal==0);
						} catch (InterruptedException e) {
							reachable = false;
						}
					} else {
						reachable = inet.isReachable(1000);
					}
					if (reachable && !metrics.getLive(con)) {
						recover = true;
						break;
					}
				}
				if (recover) {
					Pool.recoverSlaves(con);
				}
				com.xensource.xenapi.Session.logout(con);	
			} catch (XmlRpcException | IOException e) {
				logger.error("", e);
			}
	}
	private boolean isWindows(){
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
	}
}
