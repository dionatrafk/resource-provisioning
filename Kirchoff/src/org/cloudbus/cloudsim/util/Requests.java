package org.cloudbus.cloudsim.util;

public class Requests {
	private double currentTime;
	private long accepted;
	private long rejected;
	
	public Requests(double currentTime, long accepted, long rejected) {
		super();
		this.currentTime = currentTime;
		this.accepted = accepted;
		this.rejected = rejected;
	}
	public double getCurrentTime() {
		return currentTime;
	}
	public void setCurrentTime(double currentTime) {
		this.currentTime = currentTime;
	}
	public long getAccepted() {
		return accepted;
	}
	public void setAccepted(int accepted) {
		this.accepted = accepted;
	}
	public long getRejected() {
		return rejected;
	}
	public void setRejected(int rejected) {
		this.rejected = rejected;
	}

}
