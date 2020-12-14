package org.cloudbus.cloudsim;

/**
 * This class contains a description of a service requested
 * by a user. It consists of an id and a class of service.
 * it is expected that services belonging to different classes
 * will have different requirements for execution in the data
 * center. This diference should be handled in the data center. 
 * @author rodrigo
 *
 */
public class Service {

	public static final int SERVICE_CLASS_DEFAULT = 0;
	public static final int SERVICE_CLASS_SMALL = 1;
	public static final int SERVICE_CLASS_BIG = 2;

	
	private int id;
	private int serviceClass;
	
	public Service(int id, int serviceClass) {
		this.id = id;
		this.serviceClass = serviceClass;
	}

	public int getId() {
		return id;
	}

	public int getServiceClass() {
		return serviceClass;
	}	
}
