package uk.ac.york.mhe504.util;

public class DataTimeMap {
	
	private long data;
	private double time;
	
	public DataTimeMap (long data, double time)
	{
		this.data = data;
		this.time = time;
	}	
	public long getData() {
		return data;
	}
	public void setData(long data) {
		this.data = data;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	
	public String toString(){
		return "[time="+time+"; data="+data+"]";
	}

}
