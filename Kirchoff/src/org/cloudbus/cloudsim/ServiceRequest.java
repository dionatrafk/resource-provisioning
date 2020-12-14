package org.cloudbus.cloudsim;

public class ServiceRequest {
	
	private int serviceType;
	private int numberOfRequests;
	
	public ServiceRequest(int serviceType, int numberOfRequests) {
		super();
		this.serviceType = serviceType;
		this.numberOfRequests = numberOfRequests;
	}

	public int getServiceType() {
		return serviceType;
	}

	public int getNumberOfRequests() {
		return numberOfRequests;
	}
}
