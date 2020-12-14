package org.cloudbus.cloudsim.requestsgenerator;

import java.util.Random;

import org.cloudbus.cloudsim.Service;
import org.cloudbus.cloudsim.ServiceRequest;
import org.cloudbus.cloudsim.distributions.WeibullDistr;

public class BotRequestsGenerator extends RequestsGenerator {
	
	//Monday to Friday
	private static final int WEEKDAY = 0;
	
	//Saturday and Sunday
	private static final int WEEKEND = 1;
	
	//8am-5pm
	private static final int PEAK_TIME = 2;
	
	//5pm-8am
	private static final int OFF_PEAK_TIME = 3;

	protected Random distr;
	protected WeibullDistr iat;
	protected WeibullDistr daily;
	protected WeibullDistr size;
	protected double recountTime;
	protected double currentIat;
	protected double stopTime;
	
	public BotRequestsGenerator(int simulatedDays) {
		distr = new Random(System.currentTimeMillis());
		iat = new WeibullDistr(4.25,7.86);
		daily = new WeibullDistr(1.79,24.16);
		size = new WeibullDistr(1.76,2.11);
		recountTime=-1.0;
		stopTime = simulatedDays*24*60*60;
	}

	@Override
	public double delayToNextEvent(double currentTime) {
		int day = getDayOfTheWeek(currentTime);
		int time = getTimeOfTheDay(currentTime);
		
		//peak time: requests arrive a lot
		if(day==WEEKDAY && time==PEAK_TIME){
			double iats = iat.sample();
			if(iats<1) iats=1;
			return iats;
		}
		
		//few requests, given by daily cycle, in periods of 30 minutes
		if (currentTime>recountTime){
			recountTime=currentTime+30*60;
			double dailySamp=daily.sample();
			if(dailySamp<1) dailySamp=1;
			currentIat = (30*60)/dailySamp;
		}
		return currentIat;
	}

	@Override
	public ServiceRequest nextRequests(double currentTime) {
		if(currentTime>=stopTime) return new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,0);
		
		double reqs = size.sample();
		
		double lowTasks = Math.pow(2, reqs);
		double upTasks =  Math.pow(2, reqs+1) - 1;
		
		int tasks = (int)(lowTasks + distr.nextDouble()*(upTasks-lowTasks));
				
		return new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,tasks);
	}
	
	private int getDayOfTheWeek(double currentTime){
		//how many days since beginning?
		double days = currentTime/(60*60*24);
		int day = ((int)days)%7;
		
		//considering simulation started on Monday
		if (day==5||day==6) return WEEKEND;
		return WEEKDAY; 
	}
	
	private int getTimeOfTheDay(double currentTime){
		double time=currentTime%(60*60*24);
		
		//considering simulation started at midnight
		if (time<(8*60*60)) return OFF_PEAK_TIME;
		if (time<(17*60*60)) return PEAK_TIME;
		return OFF_PEAK_TIME;
	}
}
