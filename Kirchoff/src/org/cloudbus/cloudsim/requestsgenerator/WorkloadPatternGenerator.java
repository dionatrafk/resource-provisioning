package org.cloudbus.cloudsim.requestsgenerator;

import org.cloudbus.cloudsim.ServiceRequest;

public class WorkloadPatternGenerator {

	public static void main(String[] args){
		
		WikiRequestsGenerator generator = new WikiRequestsGenerator(28);
		long currentTime = 0;
		
		do {
			//accumulate requests in 10 min intervals
			long totalRequests = 0;
			for (int i=0;i<10;i++){
				ServiceRequest req = generator.nextRequests(currentTime+(60*i));
				totalRequests += req.getNumberOfRequests();
			}
			System.out.println(currentTime+"\t"+totalRequests);
			currentTime+=600;
		}while (currentTime<28*24*60*60);
	}
	
}
