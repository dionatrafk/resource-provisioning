package org.cloudbus.cloudsim.requestsgenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Service;
import org.cloudbus.cloudsim.ServiceRequest;

public class Grid5000RequestsGenerator extends RequestsGenerator {
	int currentIdx;
	int numberOfRequests;
	long workloadStartTime;
	ArrayList<GridRequest> requests;
	
	public Grid5000RequestsGenerator(String fileName,int numberOfRequests) {
		this.currentIdx=0;
		this.numberOfRequests=numberOfRequests;
		this.requests=new ArrayList<GridRequest>();
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
		        if (line==numberOfRequests) break;
		    }
		    
		    //there were not enough number of lines as required
		    if(line<numberOfRequests) numberOfRequests=line;
		    
		    reader.close(); 
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
		if(lineNum==0) workloadStartTime=submitTime;
		
		submitTime-=workloadStartTime;
		submitTime+=4;
		if (submitTime<0) submitTime=0;
		
		//int runTime = Integer.parseInt(fields[3]);
		
		int numProc = Integer.parseInt(fields[7]);
		if(numProc<=0) numProc=1;
		
		//int reqMem = Integer.parseInt(fields[9]);
		
		requests.add(new GridRequest(submitTime,new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,numProc)));
		
		return true;
	}

	@Override
	public double delayToNextEvent(double currentTime) {
		while(currentIdx<numberOfRequests&&currentTime>requests.get(currentIdx).getEventTime()) currentIdx++;
		if(currentIdx>=numberOfRequests) return -1.0;
		
		return requests.get(currentIdx+1).getEventTime()-currentTime;
	}

	@Override
	public ServiceRequest nextRequests(double currentTime) {
		while(currentIdx<numberOfRequests&&currentTime>requests.get(currentIdx).getEventTime()) currentIdx++;
		if(currentIdx>=numberOfRequests||currentTime<requests.get(currentIdx).getEventTime())
			return new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,0);
		
		return requests.get(currentIdx).getRequest();
	}
}

class GridRequest {
	double eventTime;
	ServiceRequest request;
	
	public GridRequest(double eventTime, ServiceRequest request) {
		super();
		this.eventTime = eventTime;
		this.request = request;
	}

	public double getEventTime() {
		return eventTime;
	}

	public ServiceRequest getRequest() {
		return request;
	}
}
