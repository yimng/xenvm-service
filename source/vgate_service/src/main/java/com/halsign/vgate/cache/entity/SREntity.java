package com.halsign.vgate.cache.entity;

import java.util.Set;

public class SREntity {

	private String UUID;
	private SRTYPE type;
	private boolean shared;
	
	private Set<String> tags;
	
	public SRTYPE getType() {
		return type;
	}

	public SREntity (String UUID, SRTYPE type, boolean shared) {
		this.UUID = UUID;
		this.type = type;
		this.shared = shared;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public void setType(SRTYPE type) {
		this.type = type;
	}

	public String getUUID() {
		return UUID;
	}

	public void setUUID(String uUID) {
		UUID = uUID;
	}
	
	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public enum SRTYPE {
		LOCAL,
		EXT,
		LVMOISCSI,
		ISO,
		NFS,
		LVM,
		NETAPP,
		UDEV,
		LVMOFC,
		LVMOHBA,
		RAWHBA,
		LVMOBOND,
		UNKNOWN
	}
	
	public String toString() {
		String str = "SRUUID: " + UUID + "\n" 
				+ "SR type :" + type.toString() + "\n";
		str += "Tags:";
		for (String tag : tags) {
			str += tag + " ";
		}
		str += "\nshared: " + shared + "\n";
		return str;
	}
	
	public static SRTYPE fromString(String str) {
		switch(str) {
		case "local" : return SRTYPE.LOCAL;
		case "ext" : return SRTYPE.EXT;
		case "lvmoiscsi" : return SRTYPE.LVMOISCSI;
		case "iso" : return SRTYPE.ISO;
		case "nfs" : return SRTYPE.NFS;
		case "lvm" : return SRTYPE.LVM;
		case "netapp" : return SRTYPE.NETAPP;
		case "udev" : return SRTYPE.UDEV;
		case "lvmofc" : return SRTYPE.LVMOFC;
		case "lvmonhba" : return SRTYPE.LVMOHBA;
		case "rawhba" : return SRTYPE.RAWHBA;
		case "lvmobond" : return SRTYPE.LVMOBOND;
		default: return SRTYPE.UNKNOWN;
		}
	}
	
}
