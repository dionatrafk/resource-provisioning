package org.cloudbus.cloudsim.forecaster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ForecasterRuntime {
	public static  String predModel = "";
	public static boolean firstPrediction = true;
	
	public static double Forecast(double[] inputTimeSeries, String predictionModel, int predictionInterval){
		
		predModel = predictionModel;
		//when is the first prediction on the simulation
		if ((firstPrediction == true) && (!predModel.equals("ARIMA"))){
			resetModel();
			firstPrediction = false;
		}
		
		String command ="";
		Double load = 0.0;
	
		double lastValue = 0.0;
		String timeSeries = "";
		for (int i = 0; i<inputTimeSeries.length-1;i++) {
			timeSeries = timeSeries + inputTimeSeries[i]+',';
			lastValue = inputTimeSeries[i+1];
		}
		//replace my file path for your absolute file path
		switch(predictionModel) {
		  case "ARIMA":
			command = "/usr/bin/python /home/dion/eclipse-workspace/Kirchoff/bin/org/cloudbus/cloudsim/forecaster/arima_cloudsim.py "+timeSeries.substring(0, timeSeries.length()-1) +','+lastValue+ ' '+ predictionInterval ;
		    break;
		  case "MLP":
			command = "/usr/bin/python /home/dion/eclipse-workspace/Kirchoff/bin/org/cloudbus/cloudsim/forecaster/mlp_cloudsim_new.py "+timeSeries.substring(0, timeSeries.length()-1) +' '+lastValue+ ' ' + predictionInterval;	
			break;
		  case "GRU":
			command = "/usr/bin/python /home/dion/eclipse-workspace/Kirchoff/bin/org/cloudbus/cloudsim/forecaster/gru_cloudsim_new.py "+timeSeries.substring(0, timeSeries.length()-1) +' '+lastValue+ ' ' + predictionInterval;
			break;	    
		  default:
		    // code block
		}
		
		System.out.println(command);	  
		
		try {
			load = Double.parseDouble(ExecuteCommand(command));	
		}catch (Exception e) {
			load = lastValue;
		}
			
		return load;
		
		  
	}
	
	
	public static String resetModel(){
		
		String result="";
		String command = "";
		if (!predModel.equals("ARIMA")) {
		//replace my file path for your absolute file path
		  switch(predModel) {
		   case "MLP":
		  	  command = "rm /home/dion/eclipse-workspace/Kirchoff/bin/org/cloudbus/cloudsim/forecaster/mlp_cloudsim_new.h5";			
			  break;
		   case "GRU":
			  command = "rm /home/dion/eclipse-workspace/Kirchoff/bin/org/cloudbus/cloudsim/forecaster/gru_cloudsim_new.h5";		
			  break;	    
		   default:
		    // code block
		  }
		
		  result = ExecuteCommand(command);	
		}

		return result;
				
	}
	
	public static String ExecuteCommand(String command){
		String result ="";
		try {
				//System.out.println(command);
				Process process = Runtime.getRuntime().exec(command);
		
				StringBuilder output = new StringBuilder();
	
				BufferedReader reader = new BufferedReader( new InputStreamReader(process.getInputStream()));
	
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line + "\n");
				}
	
				int exitVal = process.waitFor();
				String str = output.toString();
				//System.out.println(output);
				if (exitVal == 0) {
					//System.out.println("Success!");
					//System.out.println(output);]String str = output.toString();
					//str = str.substring(str.indexOf('['+2),str.indexOf(']'));
					result = str;
					//System.exit(0);
				} else {
					System.out.println("Abnormal Execution");
				}
	
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
				
		return result.trim();
	}
	
}


