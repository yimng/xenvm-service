package com.halsign.vgate.spec;

public class TemplateSpec {

	
	//The tag of which SR we expect for system vdi of the template
	private String systemVdiTag = "";
	
	//The tag of which SR we expect for user vdi of the template. If template does not
	// has user vdi, set this value to null.
	private String userVdiTag = ""; 
	
	//The tag of which vif we expect for this template
	private String vifTag = "";

	public String getSystemVdiTag() {
		return systemVdiTag;
	}

	public void setSystemVdiTag(String systemVdiTag) {
		this.systemVdiTag = systemVdiTag;
	}

	public String getUserVdiTag() {
		return userVdiTag;
	}

	public void setUserVdiTag(String userVdiTag) {
		this.userVdiTag = userVdiTag;
	}

	public String getVifTag() {
		return vifTag;
	}

	public void setVifTag(String vifTag) {
		this.vifTag = vifTag;
	}
	
	
}
