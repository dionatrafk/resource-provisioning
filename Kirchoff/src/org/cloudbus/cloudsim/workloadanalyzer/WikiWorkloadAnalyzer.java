package org.cloudbus.cloudsim.workloadanalyzer;


/**
 * Implements the daily and weekly behaviour highlighted by Urdaneta et al.
 * Basically, there is big traffic from Tuesday to Friday, medium traffic
 * Saturday and Monday, and low traffic on Sunday. In every day, there is
 * a peak in requests at noon and a valley at midnight.
 * 
 * Requests/second are "packed": scaled down, but processing time from
 * grouped request compensates it.
 * @author rodrigo
 *
 */
public class WikiWorkloadAnalyzer extends WorkloadAnalyzer {

	//Tuesday to Thursday
	private static final int WEEKDAY = 0;
	
	//Saturday and Monday
	private static final int WEEKEND = 1;
	
	private static final int SUNDAY = 2;
			
	//2:00am-7:00am
	private static final int UPPING_PEAK_TIME_1 = 3;
	
	//7:00am-11:30am
	private static final int UPPING_PEAK_TIME_2 = 4;
	
	//11:30am-12:30pm
	private static final int PEAK_TIME = 5;
	
	//12:30pm-4pm
	private static final int LOWING_PEAK_TIME_1 = 6;
	
	//4pm-8pm
	private static final int LOWING_PEAK_TIME_2 = 7;
	
	//8pm-2:00am
	private static final int OFF_PEAK_TIME = 8;
		
	public WikiWorkloadAnalyzer() {
	}

	@Override
	public double delayToNextChangeInModel(double currentTime) {
		int days= (int) currentTime/(60*60*24);
		double time=currentTime%(60*60*24);

		int toHighing1 = 2*60*60; //2:00
		int toHighing2 = 7*60*60; //7:00
		int toPeak = 11*60*60+60*30; //11:30
		int toLowing1 = 12*60*60+30*60; //12:30
		int toLowing2 = 16*60*60; //16:00
		int toLow = 20*60*60; //20:00
		
		if(time<toHighing1) return days*60*60*24+toHighing1-currentTime;
		if(time<toHighing2) return days*60*60*24+toHighing2-currentTime;
		if(time<toPeak) return days*60*60*24+toPeak-currentTime;
		if(time<toLowing1) return days*60*60*24+toLowing1-currentTime;
		if(time<toLowing2) return days*60*60*24+toLowing2-currentTime;		
		if(time<toLow) return days*60*60*24+toLow-currentTime;

		return  (days+1)*60*60*24+toHighing1-currentTime;
	}

	@Override
	public double getEstimatedArrivalRate(double currentTime) {
		int day = getDayOfTheWeek(currentTime); 
		int time = getTimeOfTheDay(currentTime);
		double fac=60;
		switch (day){
		case WEEKDAY: switch (time){
						case PEAK_TIME: return 12.0/fac;
						case OFF_PEAK_TIME: return 5.0/fac;
						case UPPING_PEAK_TIME_1: return 8.5/fac;
						case UPPING_PEAK_TIME_2: return 10.0/fac;
						case LOWING_PEAK_TIME_1: return 8.0/fac;
						case LOWING_PEAK_TIME_2: return 7.0/fac;	
						}
		case WEEKEND: switch (time){
						case PEAK_TIME: return 10.0/fac;
						case OFF_PEAK_TIME: return 4.5/fac;
						case UPPING_PEAK_TIME_1: return 7.5/fac;
						case UPPING_PEAK_TIME_2: return 9.0/fac;
						case LOWING_PEAK_TIME_1: return 9.0/fac;
						case LOWING_PEAK_TIME_2: return 6.5/fac;
						}
		case SUNDAY: switch (time){
						case PEAK_TIME: return 9.0/fac;
						case OFF_PEAK_TIME: return 4.0/fac;
						case UPPING_PEAK_TIME_1: return 7.5/fac;
						case UPPING_PEAK_TIME_2: return 8.5/fac;
						case LOWING_PEAK_TIME_1: return 7.0/fac;
						case LOWING_PEAK_TIME_2: return 6.0/fac;
						}
		default: return 0.0;
		}
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
	
	private int getTimeOfTheDay(double currentTime){
		double time=currentTime%(60*60*24);
		
		//considering simulation started at midnight
		if (time<(2*60*60)) return OFF_PEAK_TIME;
		if (time<(7*60*60)) return UPPING_PEAK_TIME_1;
		if (time<(11*60*60+30*60)) return UPPING_PEAK_TIME_2;
		if (time<(12*60*60+30*60)) return PEAK_TIME;
		if (time<(16*60*60)) return LOWING_PEAK_TIME_1;
		if (time<(20*60*60)) return LOWING_PEAK_TIME_2;
		return OFF_PEAK_TIME;
	}
}
