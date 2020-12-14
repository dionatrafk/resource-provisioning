package org.cloudbus.cloudsim.workloadanalyzer;


public class SimpleWorkloadAnalyzer extends WorkloadAnalyzer {

	double mean;
	
	public SimpleWorkloadAnalyzer(double mean) {
		this.mean=mean;
	}

	@Override
	public double delayToNextChangeInModel(double currentTime) {
		//update every 15 minutes
		return 15.0*60.0;
	}

	@Override
	public double getEstimatedArrivalRate(double currentTime) {
		return mean;
	}

}
