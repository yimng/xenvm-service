package com.halsign.vgate.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.halsign.vgate.License;
import com.halsign.vgate.VgateException;
import com.halsign.vgate.VgateMessage;
import com.halsign.vgate.service.LicenseService;
import com.halsign.vgate.util.VgateConnectionPool;
import com.halsign.vgate.util.VgateConnectionPool.VgateConnection;
import com.halsign.vgate.util.VgateMessageConstants;
import com.halsign.vgate.util.VgateUtil;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.LicenseProcessingError;
import com.xensource.xenapi.Types.XenAPIException;

public class LicenseServiceImp implements LicenseService {
	private static final Logger logger = LoggerFactory.getLogger(LicenseServiceImp.class);

	@Override
	public VgateMessage applyLicense(VgateConnection con, String hostUuid,
			License license) {
		String mac;
		int socketNum;
		Host host;
		
		if(con == null || (!VgateUtil.isConnectionValid(con))) {
			logger.error("Connection invalid.");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_CONNECTION_INVALID);
		}
		
		if(hostUuid == null) {
			logger.error("The host UUID you supply is invalid. ");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_HOST_UUID_INVALID);
		}
		
		try {
			host = Host.getByUuid(con, hostUuid);
		} catch (XenAPIException | XmlRpcException e) {
			logger.error("The host UUID you supply is invalid. ");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_HOST_UUID_INVALID);
		}
		
		if(license == null) {
			logger.error("license invalid.");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE, 
					VgateMessageConstants.ERROR_CODE_LICENSE_INVALID);
		}
		
		mac = license.getMac();
		final String regexMac = "^([0-9a-fA-F]{2})(([\\s:-][0-9a-fA-F]{2}){5})$";
		if(mac == null || !(mac.matches(regexMac))) {
			logger.error("Mac address is invalid.");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_MAC_INVALID);
		}
		
		socketNum = license.getSocketNum();
		if(socketNum  < 1) {
			logger.error("Socket number must be one or bigger than one. Socket number you supply is " + socketNum + ".");
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_SOCKET_NUMBER_LESS_THAN_ONE);
		}
		
		/*code goes here to call web service to get contents.*/
		String encodeContent;
		try {
			byte [] content = this.callWebServiceForGenerateLicense(license);
			encodeContent = new String(Base64.encodeBase64(content));
		} catch (ClientProtocolException e) {
			String message = "License server failed";
			logger.error(message, e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_GENERATE_FAILURE);
		} catch (IOException e) {
			String message = "Failed to generate license.";
			logger.error(message, e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_GENERATE_FAILURE);
		} catch (Exception e) {
			logger.error("", e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_GENERATE_FAILURE);
		}
		
		try {
			host.licenseApply(con, encodeContent);
		} catch (BadServerResponse e) {
			logger.error("BadServerResponse", e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_APPLY_FAILURE);
		} catch (LicenseProcessingError e) {
			logger.error("LicenseProcessingError", e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_APPLY_FAILURE);
		} catch (XenAPIException e) {
			logger.error("XenAPIException", e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_APPLY_FAILURE);
		} catch (XmlRpcException e) {
			logger.error("XmlRpcException", e);
			return new VgateMessage(VgateMessageConstants.STATUS_FAILURE,
					VgateMessageConstants.ERROR_CODE_LICENSE_APPLY_FAILURE);
		}
		logger.info("apply license successfully");
		
		return new VgateMessage(VgateMessageConstants.STATUS_SUSCCESS);
	}

	private byte[] callWebServiceForGenerateLicense(License license)
			throws Exception {
		String expired_date = new SimpleDateFormat("yyyyMMdd").format(license
				.getExpireDate());
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(
				"http://www.halsign.cn/license_ali/gen_backend.php");
		//192.166.30.251 
		//www.halsign.cn
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", "ali"));
		nvps.add(new BasicNameValuePair("password", "halsign-aliws653$*@"));
		nvps.add(new BasicNameValuePair("sku_type", "3"));//hard code
		nvps.add(new BasicNameValuePair("version", "5.2.1"));//hard code
		nvps.add(new BasicNameValuePair("expired_date", expired_date));
		nvps.add(new BasicNameValuePair("mac", license.getMac().toUpperCase()));
		nvps.add(new BasicNameValuePair("product_code", "0000-0000-0000-0000-0000-0000")); //hard code
		nvps.add(new BasicNameValuePair("serial_number", "00000000-0000-0000-0000-000000000000"));//hard code
		nvps.add(new BasicNameValuePair("socket_number", license.getSocketNum() + ""));
		nvps.add(new BasicNameValuePair("company", license.getCompanyName()));

		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("httpPost set param error", e);
		}
		ResponseHandler<byte[]> responseHandler = new ResponseHandler<byte[]>() {
			@Override
			public byte[] handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				if (status >= 200 && status < 300) {
					HttpEntity entity = response.getEntity();
					return entity != null ? EntityUtils.toByteArray(entity)	: null;
				} else {
					throw new ClientProtocolException(
							"Unexpected response status: " + status);
				}
			}
		};
		
		byte [] ret = httpclient.execute(httpPost, responseHandler);
		String contentBody = new String(ret);
		if ("loginfail".equals(contentBody)) {
			throw new Exception("You should login license server to generate license");
		}
		if ("generatefail".equals(contentBody)) {
			throw new Exception("License generate failed");
		}
		return ret;
	}
	
	public static void main(String args[]) throws VgateException {
		final String hostId = "192.166.30.5";
		final String userName = "root";
		final String pwd = "aliursys";
		VgateConnection vgateConn = VgateConnectionPool.getInstance().getConnect(hostId, userName, pwd, 10, 10);
		LicenseServiceImp service = new LicenseServiceImp();
		License license = new License();
		license.setCompanyName("Halsign");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);
		
				
		license.setExpireDate(cal.getTime());
		license.setSocketNum(4);
		license.setMac("78:2b:cb:38:36:23");
		service.applyLicense(vgateConn, "b191bbd9-c362-49ac-b195-1baad1542d63", license);
	}
	
}
