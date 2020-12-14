package org.cloudbus.cloudsim.workloadanalyzer;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * This class is responsible for loading and processing
 * the cloud workload, in order to generate load estimation.
 * Implementations of this class can use different methods
 * for acquiring the workloads metrics and processing them.
 * @author rodrigo
 *
 */
public abstract class WorkloadAnalyzer {	
	public abstract double getEstimatedArrivalRate(double currentTime) throws RserveException, REXPMismatchException;	
	public abstract double delayToNextChangeInModel(double currentTime);	
}
