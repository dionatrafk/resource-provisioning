package org.cloudbus.cloudsim.workloadanalyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.distributions.ExponentialDistr;

public class Grid5000WorkloadAnalyzer extends WorkloadAnalyzer {
	int startFrom;
	int validLines;
	long previousTime;
	double sumOfinterarrival;
	double sumOfSqrIntearrival;
	double sumOfReqs;
	double sumOfSqrReqs;
	double meanInterarrival;
	double stddevInterarrival;
	double meanRequests;
	double stddevReqs;
	
	ExponentialDistr reqGenerator;
	ExponentialDistr intearrivalGenerator;
	
	public Grid5000WorkloadAnalyzer(String fileName,int startFrom) {
		this.startFrom = startFrom;
		this.previousTime=0;
		this.validLines=0;
		this.sumOfinterarrival=0.0;
		this.sumOfSqrIntearrival=0.0;
		this.sumOfReqs=0.0;
		this.sumOfSqrReqs=0.0;
		this.meanInterarrival=0.0;
		this.stddevInterarrival=0.0;
		this.meanRequests=0.0;
		this.stddevReqs=0.0;

		processWorkload(fileName);
	}
	
	private void processWorkload(String fileName) {
		BufferedReader reader = null;

		try {
        	FileInputStream file = new FileInputStream(fileName);
		    InputStreamReader input = new InputStreamReader(file);
		    reader = new BufferedReader(input);
		    int line=0;
		    while (reader.ready()){
		    	boolean validLine=parseLine(reader.readLine(),line);
		    	if (validLine) line++;
		    }	    
		    reader.close();
		    
		    generateStatistics();		    
        } catch (Exception e) {
        	Log.printLine("Error parsing trace file");
        	e.printStackTrace();
		} finally {
		    if (reader != null) {
		    	try {
		    		reader.close();
		        } catch (IOException ioe) {
		        	Log.printLine("Error parsing trace file");
		        	ioe.printStackTrace();
		        }
		    }
		}
    }
	
	private boolean parseLine(String line, int lineNum) {
		if (line.startsWith(";") || line.startsWith("#")) {
			return false;
		}
		
		String[] fields = line.trim().split("\\s+");
		//empty line
		if(fields.length<=1) return false;
				
		for (int i=0; i<fields.length; i++) {
			fields[i] = fields[i].trim();
		}

		long submitTime = Long.parseLong(fields[1]);
		
		//this line is valid, but belongs to workload generation
		if (lineNum<this.startFrom) {
			this.previousTime=submitTime;			
			return true;
		}
		this.validLines++;
		
		double interarrival=submitTime-previousTime;
		
		this.sumOfinterarrival+=interarrival;
		this.sumOfSqrIntearrival+=(interarrival*interarrival);
		
		//int runTime = Integer.parseInt(fields[3]);
		
		int numProc = Integer.parseInt(fields[7]);
		if(numProc<=0) numProc=1;
		this.sumOfReqs+=numProc;
		this.sumOfSqrReqs+=(numProc*numProc);
		
		//int reqMem = Integer.parseInt(fields[9]);
				
		this.previousTime=submitTime;
		return true;
	}
	
	private void generateStatistics(){
		meanInterarrival=this.sumOfinterarrival/this.validLines;
		double meanOfSqrIntearrival=this.sumOfSqrIntearrival/this.validLines;
		stddevInterarrival = Math.sqrt(meanOfSqrIntearrival-meanInterarrival*meanInterarrival);
		meanRequests=this.sumOfReqs/this.validLines;
		double meanOfSqrRequests=this.sumOfSqrReqs/this.validLines;
		stddevReqs = Math.sqrt(meanOfSqrRequests-meanRequests*meanRequests);
		this.intearrivalGenerator=new ExponentialDistr(meanInterarrival);
		this.reqGenerator=new ExponentialDistr(meanRequests);
	}

	@Override
	public double delayToNextChangeInModel(double currentTime) {
		double delay =  intearrivalGenerator.sample();
		if (delay<60) return 60*60;
		return delay;
	}

	@Override
	public double getEstimatedArrivalRate(double currentTime) {
		double reqs = reqGenerator.sample();
		double delay =  intearrivalGenerator.sample();
		if (delay<60) delay=60*60;
		if(reqs<0) reqs=0;
		return reqs/delay;
	}
}
