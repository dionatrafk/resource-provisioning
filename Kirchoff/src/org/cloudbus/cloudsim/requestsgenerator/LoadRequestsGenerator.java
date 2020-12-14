package org.cloudbus.cloudsim.requestsgenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Service;
import org.cloudbus.cloudsim.ServiceRequest;
import org.cloudbus.cloudsim.core.CloudSim;

public class LoadRequestsGenerator extends RequestsGenerator {
	int currentIdx;
	int numberOfRequests;
	int simulationDays;
	int initialDay;
	long cumulativeTime;
	ArrayList<LoadRequest> requests;
	
	public LoadRequestsGenerator(String fileName,int simulationDays) {
		this.currentIdx=0;
		this.simulationDays=simulationDays;//days of simulation
		this.requests=new ArrayList<LoadRequest>();
		processWorkload(fileName);
	}

	private void processWorkload(String fileName) {
		BufferedReader reader = null;

		try {
        	FileInputStream file = new FileInputStream(fileName);
            
		    InputStreamReader input = new InputStreamReader(file);
		    reader = new BufferedReader(input);
		    
		    this.initialDay = 0;
		    boolean validLine = false;
		    String line = "";
		    while ((line = reader.readLine())!= null){
		    	//if (reader.readLine()== null) break;
		    	validLine = parseLine(line);
		    	if (validLine == false) break; 
		       	
		    }
		    numberOfRequests = requests.size();
		    		    
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
		
	private boolean parseLine(String line) {
		String[] fields = line.trim().split(",");
		//empty line
		if(fields.length<=1) return false;

		for (int i=0; i<fields.length; i++) {
			fields[i] = fields[i].trim();
		}
		
		int currentDay = Integer.parseInt(fields[0].substring(0, 2));
		if(this.initialDay == 0) this.initialDay = currentDay;
		
		if (currentDay > this.initialDay) {
			if ((currentDay - this.initialDay) ==  this.simulationDays) return false;
		}
		//considering requests are made in minutes	
		cumulativeTime+= (long) 60.0;
				
		int numProc = Integer.parseInt(fields[1]);
		if(numProc<=0) numProc=1;
				
		requests.add(new LoadRequest(cumulativeTime,new ServiceRequest(Service.SERVICE_CLASS_DEFAULT,numProc)));
		//Log.printLine(line);
		return true;
	}

	@Override
	public double delayToNextEvent(double currentTime) {
		while(currentIdx<numberOfRequests&&currentTime>requests.get(currentIdx).getEventTime()) currentIdx++;
		if((currentIdx+1)>=numberOfRequests) return -1.0;
		
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

class LoadRequest {
	double eventTime;
	ServiceRequest request;
	
	public LoadRequest(double eventTime, ServiceRequest request) {
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
