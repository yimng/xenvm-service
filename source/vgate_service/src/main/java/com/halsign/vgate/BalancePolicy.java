package com.halsign.vgate;

public class BalancePolicy {

	private Policy policy;
	
	private String specifiedHost;

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public String getSpecifiedHost() {
		return specifiedHost;
	}

	public void setSpecifiedHost(String specifiedHost) {
		this.specifiedHost = specifiedHost;
	}
	
	
}
