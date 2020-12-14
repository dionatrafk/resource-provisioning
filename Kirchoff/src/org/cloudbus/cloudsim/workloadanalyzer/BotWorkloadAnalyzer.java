package org.cloudbus.cloudsim.workloadanalyzer;

public class BotWorkloadAnalyzer extends WorkloadAnalyzer {

	//Monday to Friday
	private static final int WEEKDAY = 0;
	
	//Saturday and Sunday
	private static final int WEEKEND = 1;
				
	//8am-5pm
	private static final int PEAK_TIME = 2;
	
	//5pm-8am
	private static final int OFF_PEAK_TIME = 3;
		
	@Override
	public double delayToNextChangeInModel(double currentTime) {
		//because this model is less stable, recalculate every 30 minutes
		return 30*60;
	}

	@Override
	public double getEstimatedArrivalRate(double currentTime) {
		int day = getDayOfTheWeek(currentTime); 
		int time = getTimeOfTheDay(currentTime);
		
		//arrival rate is daily*size
		//daily = W(1.79,24.16)
		//size = W(1.76,2.11)
		//iat = W(4.25,7.86)
		
		double sizeMode=weibullMode(1.76,2.11);

		if (day==WEEKDAY&&time==PEAK_TIME){
			double iatMode=weibullMode(4.25,7.86);
			double reqsInPeak=1/iatMode;
			//1.2: best tuning for peak time
			return 1.2*reqsInPeak*sizeMode;
		}

		//2.6: best tuning for daily mode
		double dailyMode=2.6*weibullMode(1.79,24.16);
		return (dailyMode*sizeMode)/(60*30);
	}
	
	private int getDayOfTheWeek(double currentTime){
		//how many days since beginning?
		double days = currentTime/(60*60*24);
		int day = ((int)days)%7;
		
		//considering that simulation started on Monday
		if (day==5||day==6) return WEEKEND;
		return WEEKDAY; 
	}
	
	private int getTimeOfTheDay(double currentTime){
		double time=currentTime%(60*60*24);
		
		//considering that simulation started at midnight
		if (time<(7*60*60+30*60)) return OFF_PEAK_TIME;
		if (time<(17*60*60)) return PEAK_TIME;
		return OFF_PEAK_TIME;
	}

	private double weibullMode(double alpha, double beta){
		if (alpha<1.0) return 0.0;
		
		return beta*Math.pow(1-(1.0/alpha),1.0/alpha);
	}
}
