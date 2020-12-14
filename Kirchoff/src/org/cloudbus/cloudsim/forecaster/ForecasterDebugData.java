package org.cloudbus.cloudsim.forecaster;

public class ForecasterDebugData {
	private ForecastEntry[] payload;
	private String debuggingInfo;
	
	public ForecasterDebugData(ForecastEntry[] payload, String debuggingInfo)
	{
		this.payload = payload;
		this.debuggingInfo = debuggingInfo;
	}
	

	public ForecastEntry[] getPayload()
	{
		return this.payload;
	}
	
	public String getDebugInfo()
	{
		return this.debuggingInfo;
	}
}
