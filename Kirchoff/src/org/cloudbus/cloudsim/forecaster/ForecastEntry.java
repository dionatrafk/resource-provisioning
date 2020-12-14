package org.cloudbus.cloudsim.forecaster;


public class ForecastEntry {
	private int index;
	private double forecastedValue;
	private double lo_80; 
	private double hi_80;
	private double lo_95;
	private double hi_95;
	
	public ForecastEntry(int index, double forecasted, double lo_80, double hi_80, double lo_95, double hi_95)
	{
		this.index = index;
		this.forecastedValue = forecasted;
		
		this.lo_80 = lo_80;
		this.hi_80 = hi_80;
		
		this.lo_95 = lo_95;
		this.hi_95 = hi_95;
	}

	public int getIndex() {
		return index;
	}
	
	public double getForecastedValue()
	{
		return forecastedValue;
	}
	
	public double getLo_80()
	{
		return lo_80;
	}
	
	public double getHi_80()
	{
		return hi_80;
	}
	
	public double getLo_95()
	{
		return lo_95;
	}
	
	public double getHi_95()
	{
		return hi_95;
	}
	
	public String toString()
	{
		String result = null;
		
		result = String.valueOf(forecastedValue) + "\t" + 
					String.valueOf(lo_80) + "\t" + 
					String.valueOf(hi_80) + "\t" +
					String.valueOf(lo_95) + "\t" + 
					String.valueOf(hi_95);
		
		return result;
	}
	
	public String toString(boolean includeIndex)
	{
		String result = null;
		
		result = String.valueOf(index) + "\t" +
					String.valueOf(forecastedValue) + "\t" + 
					String.valueOf(lo_80) + "\t" + 
					String.valueOf(hi_80) + "\t" +
					String.valueOf(lo_95) + "\t" + 
					String.valueOf(hi_95);
		
		return result;
	}
}
