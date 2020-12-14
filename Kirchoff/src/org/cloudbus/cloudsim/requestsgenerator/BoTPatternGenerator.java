package org.cloudbus.cloudsim.requestsgenerator;

import org.cloudbus.cloudsim.ServiceRequest;

public class BoTPatternGenerator {

	public static void main(String[] args){
		
		BotRequestsGenerator generator = new BotRequestsGenerator(8);
		long currentTime = 0;
		
		do {
			ServiceRequest req = generator.nextRequests(currentTime);
			long totalRequests = req.getNumberOfRequests();

			System.out.println(currentTime+"\t"+totalRequests);
			currentTime+=generator.delayToNextEvent(currentTime);
		} while (currentTime<8*24*60*60);
	}
	
}
