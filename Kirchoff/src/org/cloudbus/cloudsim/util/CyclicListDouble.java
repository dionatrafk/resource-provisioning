package org.cloudbus.cloudsim.util;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;

public class CyclicListDouble
{

	private double[] dataArray;
	private int head;
	private boolean overloaded = false;
	private BufferedReader buffReader;
	private InputStreamReader reader;
	private boolean endOfInput = false;
	private boolean fileLoaded = false;
	private boolean closedStream = true;
	
	//initiate an empty list with a predefined capacity
	public CyclicListDouble(int size) throws InvalidParameterException
	{
		if(size < 1)
			throw new InvalidParameterException("The size of a CyclicList should be bigger that 0.");
		
		dataArray = new double[size];
		head = 0;
	}
	
	//initiate a list attached using a file as a data source.
	//the file should contain a double number in every line
	public CyclicListDouble(int size, String filePath) throws IOException, FileNotFoundException, InvalidParameterException
	{
		if(size < 1)
			throw new InvalidParameterException("The size of a CyclicList should be bigger that 0.");
		
		dataArray = new double[size];
		head = 0;
		fileLoaded = true;
		
		reader = new InputStreamReader(
				 						new FileInputStream(filePath)
									  );
		
		buffReader = new BufferedReader(reader);
		closedStream = false;
		
		int i = 0;
		String readedLine = buffReader.readLine();
		
		while(readedLine != null)
		{
			dataArray[i++] = Double.parseDouble(readedLine);
			if(i < dataArray.length)
				readedLine = buffReader.readLine();
			else
			{
				overloaded = true;
				break;
			}
		}
		
		if(readedLine == null)
		{
			endOfInput = true;
			buffReader.close();
			closedStream = true;
		}
				
	}
	
	//if the list is linked to the a file, deletes the oldest number in the list and 
	public boolean NextInputEntry()
	{
		boolean result = false;
		
		try
		{
			if(!fileLoaded)   			//The cyclicList has not been loaded from a file, so "NextInputEntry()" is not applicable in this case.
			{
				result = false;
			}
			else if (!closedStream)     //The underlying input file has already been read completely.
			{
				String readedLine = buffReader.readLine();
				if(readedLine != null)  
				{
					this.Add(Double.parseDouble(readedLine));
					result = true;
				}
				else
				{
					buffReader.close();
					closedStream = true;
					result = false;
				}
			}
			else 
			{
				result = false;
			}
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return result;
	}
	
	public void Add(double input)
	{
		if(head >= dataArray.length)
		{
			head = 0;
			overloaded = true;
		}
		
		dataArray[head++] = input;
	}
	
	
	public double[] GetData()
	{
		double[] result;
		if(!overloaded)
		{
			result = new double[head];
			for(int i = 0; i < head; i++)
			{
				result[i] = dataArray[i];
			}
		}
		else
		{
			result = new double[dataArray.length];
			//int initialPos = head - 1;
			int initialPos = head;
			if (initialPos > dataArray.length - 1)
				initialPos = 0;
			for(int i = 0; i < dataArray.length; i++)
			{
				result[i] = dataArray[initialPos++];
				if (initialPos > dataArray.length - 1)
					initialPos = 0;
			}
		}
		return result;
	}
	
	public boolean CloseStream() throws IOException
	{
		boolean result = false;
		if(!closedStream)
		{
			buffReader.close();
			closedStream = true;
			result = true;
		}
		return result;
	}
	
}
