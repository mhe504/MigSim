/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network.datacenter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import uk.ac.york.mhe504.util.DataTimeMap;

/**
 * NetworkCloudlet class extends Cloudlet to support simulation of complex applications. Each such
 * a network Cloudlet represents a task of the application. Each task consists of several stages.
 * 
 * <br/>Please refer to following publication for more details:<br/>
 * <ul>
 * <li><a href="http://dx.doi.org/10.1109/UCC.2011.24">Saurabh Kumar Garg and Rajkumar Buyya, NetworkCloudSim: Modelling Parallel Applications in Cloud
 * Simulations, Proceedings of the 4th IEEE/ACM International Conference on Utility and Cloud
 * Computing (UCC 2011, IEEE CS Press, USA), Melbourne, Australia, December 5-7, 2011.</a>
 * </ul>
 * 
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 1.0
 * @todo Attributes should be private
 * @todo The different cloudlet classes should have a class hierarchy, by means
 * of a super class and/or interface.
 */
public class NetworkCloudlet extends Cloudlet implements Comparable<Object> {
	
	public static int FINISH = -2;
	
        /** Time when cloudlet will be submitted. */
	public double submittime; 

        /** Time when cloudlet finishes execution. */
	public double finishtime; 

        /** Current stage of cloudlet execution. */
	public int currStagenum; 

        /** Star time of the current stage. 
         */
	public double timetostartStage;

        /** Time spent in the current stage. 
         */
	public double timespentInStage; 


        /** All stages which cloudlet execution. */
	public ArrayList<TaskStage> stages; 
	
	private long data = 0; //The data this NetworkCloudlet is storing
	
	private long dataSentRecieved = 0;

	private int appId;
	
	private double computeCost, storageCost, ioCost;
	
	private String name;
	
	private List<DataTimeMap> dataTimeMap;

	public NetworkCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, long data,
			double computeCost, double storageCost, double ioCost, String name) {
		super(
				cloudletId,
				cloudletLength,
				pesNumber,
				cloudletFileSize,
				cloudletOutputSize,
				utilizationModelCpu,
				utilizationModelRam,
				utilizationModelBw);
		currStagenum = -1;
		this.data = data;
		stages = new ArrayList<TaskStage>();
		setComputeCost(computeCost);
		setStorageCost(storageCost);
		setIoCost(ioCost);
		setName(name);
		setDataTimeMap(new LinkedList<>());
	}

	private void setComputeCost(double computeCost) {
		this.computeCost= computeCost; 
		
	}

	@Override
	public int compareTo(Object arg0) {
		return 0;
	}

	public double getSubmittime() {
		return submittime;
	}

	public long getData() {
		return data;
	}

	public void setData(long newValue, double time) {
		
		if (newValue >= 0)
		{
			dataSentRecieved += difference(newValue,data);
			data = newValue;
			getDataTimeMap().add(new DataTimeMap(newValue, time));
		}

	}
	
    public double getRequestedCPUTime() {
		
		if (getStatus() != Cloudlet.CREATED || 
			getStatus() != Cloudlet.READY ||
			getStatus() != Cloudlet.QUEUED) {
			
			double totalTime=0.0;
			for (TaskStage ts : stages)
				if (ts instanceof ExecutionTaskStage && 
					ts.getIgnoreInResults() == false)
				{
					totalTime = totalTime + ((ExecutionTaskStage)ts).getTime();
				}
					
			return totalTime;
		}
        return 0.0;
    }
	
	public int getRequestedComputeHours()
	{
		return (int)(getRequestedCPUTime()/ 3600);
	}

	private long difference(long newValue, long data2) {
		
		if (newValue > data)
			return newValue - data;
		else if (newValue < data)
			return data - newValue;
		else
			return 0;
	}

	public int getAppId() {
		return appId;
	}

	public void setAppId(int appId) {
		this.appId = appId;
	}

	
	public double calculateComputeCost()
	{
		double hours = getActualCPUTime() / 3600;
		int computeHours = (int)hours;
		return computeHours * getComputeCost();
	}
	
	public double calculateStorageCost()
	{
		double estimatedGB = getData()/1000000.0;
		int egb = (int) Math.ceil(estimatedGB);
		return egb * getStorageCost();
	}
	
	public double calcualateIOCost()
	{
		double millionIOPS = dataSentRecieved/1000000.0;
		int mIOPS = (int) Math.ceil(millionIOPS);
		return mIOPS*getIoCost();
	}
	
	public double getDataSentRecieved()
	{
		return dataSentRecieved;
	}

	public double getComputeCost() {
		return computeCost;
	}

	public double getStorageCost() {
		return storageCost;
	}

	public void setStorageCost(double storageCost) {
		this.storageCost = storageCost;
	}

	public double getIoCost() {
		return ioCost;
	}

	public void setIoCost(double ioCost) {
		this.ioCost = ioCost;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DataTimeMap> getDataTimeMap() {
		return dataTimeMap;
	}

	public void setDataTimeMap(List<DataTimeMap> dataTimeMap) {
		this.dataTimeMap = dataTimeMap;
	}
	
}
