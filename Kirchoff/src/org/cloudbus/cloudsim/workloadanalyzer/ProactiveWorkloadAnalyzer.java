package org.cloudbus.cloudsim.workloadanalyzer;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.cloudbus.cloudsim.forecaster.ForecasterRuntime;
import org.cloudbus.cloudsim.util.CyclicListDouble;
import org.cloudbus.cloudsim.util.IFeedbackable;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;


public class ProactiveWorkloadAnalyzer extends WorkloadAnalyzer implements IFeedbackable{

	public enum Confidence{
		Low80,
		Low95,
		Exact,
		Hi80,
		Hi95
	}
	
	private int predictInterval;
	private int predictDepth;
	private String trainingFilePath;
	private int numberOfTrainingDataPoints; 
	private CyclicListDouble cyclicList;
	private String predictionModel;
	//private int predictTrainPeriod;
	
	
	//#########################   Constructors   ########################
	
	public ProactiveWorkloadAnalyzer(String trainingFilePath, int predictInterval, int predictDepth, String predictionModel, String outputPath) throws FileNotFoundException, IOException
	{
		this.trainingFilePath = trainingFilePath;
		this.predictInterval = predictInterval;
		this.predictDepth = predictDepth;
		this.numberOfTrainingDataPoints = totalNumberOfTrainDatapoints(trainingFilePath);
		this.cyclicList = new CyclicListDouble(numberOfTrainingDataPoints, trainingFilePath);
		//this.outputFilePath = outputPath;
		this.predictionModel = predictionModel;
		
	}
	
	public ProactiveWorkloadAnalyzer(String trainingFilePath, int predictInterval, int predictDepth, String predictionModel) throws FileNotFoundException, IOException
	{		
		this(trainingFilePath, 
			predictInterval, 
			predictDepth, 
			predictionModel,
			CreateOutputFileName(trainingFilePath));
	}
		
	//###################################################################
	
	//############################  Getters  ############################
		
	public int GetNumberOfTrainingDataPoints()
	{
		return this.numberOfTrainingDataPoints;		
	}
	
	public int GetPredictInterval()
	{
		return this.predictInterval;
	}
	
	public int GetPredictDepth()
	{
		return this.predictDepth;
	}
		
	//###################################################################
	
		
	//######################  Public Methods   ##########################
		
	@Override
	public void AddKnownDataLoadEntry(double input)
	{
		cyclicList.Add(input);
	}
	
	public double[] GetHistory()
	{
		return cyclicList.GetData();
	}
	
	@Override
	public double getEstimatedArrivalRate(double currentTime) throws RserveException, REXPMismatchException {
		double result = 0;
		
		result = getEstimatedArrivalRate();
		
		return result;
	}

	@Override
	public double delayToNextChangeInModel(double currentTime) 
	{
		return predictInterval - (currentTime % predictInterval);
	}
	
	
	public double getEstimatedArrivalRate() throws RserveException, REXPMismatchException
	{
		return ForecasterRuntime.Forecast(this.cyclicList.GetData(), predictionModel, predictInterval);
	// make the new predictions here
		
	}
	
	//######################  Private Methods   ##########################
	
	private int totalNumberOfTrainDatapoints(String inputFile) throws FileNotFoundException, IOException
	{
		LineNumberReader reader = new LineNumberReader(new FileReader(trainingFilePath));
		reader.skip(Long.MAX_VALUE);
		int nol = reader.getLineNumber();
		reader.close();
		return nol;
		
	}
	
	private static String CreateOutputFileName(String inputStringPath) throws IOException
	{
		Path inputPath = Paths.get(inputStringPath);
		Path outputFilePath = Files.createTempFile(inputPath.getParent(), "output", ".txt");
		return outputFilePath.toString();
	}
	
	//###################################################################
	
	
	
	
	

}
