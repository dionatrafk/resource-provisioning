package org.cloudbus.cloudsim.requestsgenerator;

import java.util.Random;

import org.cloudbus.cloudsim.Service;
import org.cloudbus.cloudsim.ServiceRequest;

public class WikiRequestsGenerator extends RequestsGenerator {
	
	//Tuesday to Thursday
	private static final int WEEKDAY = 0;
	
	//Saturday and Monday
	private static final int WEEKEND = 1;
	
	private static final int SUNDAY = 2;
		
	protected double stopTime;
	
	Random random = new Random(System.currentTimeMillis());
	
	public WikiRequestsGenerator(int simulatedDays) {
		this.stopTime = simulatedDays*24*60*60;
	}

	/**
	 * Generates new requests every minute
	 */
	@Override
	public double delayToNextEvent(double currentTime) {
		if (currentTime<stopTime) return 60.0;
		return -1.0;
	}

	/**
	 * Requests generated during one minute are grouped here.
	 * Number of requests generated on each interval follows
	 * a normal distribution with mean given by the typical
	 * number of requests in the time of day/day of the week
	 */
	@Override
	public ServiceRequest nextRequests(double currentTime) {
		if (currentTime>=stopTime) return new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,0);
		
		double peak=0.0;
		double valley=0.0;
		int day=getDayOfTheWeek(currentTime);
		switch(day){
			case WEEKDAY: peak=1200; valley=500; break;
			case WEEKEND: peak=1000; valley=500; break;
			case SUNDAY: peak=900; valley=400; break;
		}
		
		//get the degree, in radians, related to the time
		double rad=Math.PI*getTimeOfTheDay(currentTime)/(60*60*24);
		
		//get the sin in the time
		double sin = Math.sin(rad);
		
		//sin is a number between 0 and 1. Change the scale
		//and translate the result, which is the mean of requests
		//for this time
		double mean = valley+sin*(peak-valley);
		
		double std=mean*0.2;
		int requests=0;
		requests = (int) (random.nextGaussian()*std+mean)/100;
		//for(int i=0;i<60;i++){
			//int req = (int) (random.nextGaussian()*std+mean);
			//requests+=req;
		//}
		//requests/=100;
				
		return new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,requests);
	}
	
	private int getDayOfTheWeek(double currentTime){
		//how many days since beginning?
		double days = currentTime/(60*60*24);
		int day = ((int)days)%7;
		
		//considering simulation started on Monday
		if (day==6) return SUNDAY;
		if (day==0||day==5) return WEEKEND;
		return WEEKDAY; 
	}
	
	//time, in seconds spent since 0:00:00 of the current day
	private double getTimeOfTheDay(double currentTime){
		return currentTime%(60*60*24);
	}

}
