/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network.datacenter;

import java.util.ArrayList;

/**
 * AppCloudlet class represents an application which user submit for execution within a datacenter. It
 * consist of several {@link NetworkCloudlet NetworkCloudlets}.
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
 * 
 * @todo If it is an application/cloudlet, it would extend the Cloudlet class.
 * In the case of Cloudlet class has more attributes and methods than
 * required by this class, a common interface would be created.
 * 
 * @todo The attributes have to be defined as private.
 * 
 * @todo: Surely this should be an abstract class?
 * 
 */
public abstract class AppCloudlet {

	
	//App types
    public static final int HPC = 1;
    public static final int WEB_APP = 2;
	public static final int WORKFLOW = 3;
	
	private int type;

	private int appID;

        /**
         * The list of {@link NetworkCloudlet} that this AppCloudlet represents.
         */
	private ArrayList<NetworkCloudlet> clist;

        /**
         * @todo Not sure what this if for...
         */
	private double deadline;

        /**
         * Number of VMs the AppCloudlet can use.
         */
	private int numberOfVMs;

        /**
         * Id of the AppCloudlet's owner.
         */
	private int userId;

        /**
         * @todo Not sure what this if for...
         */
	private double execTime;
	
	/**
	 * The ID of the VM this app is running on. 
	 */
	private int vmId;

	public AppCloudlet(int type, int appID, double deadline, int numbervm, int userId, int vmId) {
		super();
		setType(type);
		setAppID(appID);
		setDeadline(deadline);
		setNumberOfVMs(numbervm);
		setUserId(userId);
		setCloudletList(new ArrayList<NetworkCloudlet>());
		setVmId(vmId);
	}

	public abstract void createCloudletList();

	public double getExecTime() {
		return execTime;
	}

	public void setExecTime(double execTime) {
		this.execTime = execTime;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getNumberOfVMs() {
		return numberOfVMs;
	}

	public void setNumberOfVMs(int numberOfVMs) {
		this.numberOfVMs = numberOfVMs;
	}

	public double getDeadline() {
		return deadline;
	}

	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}

	/**
	 * Returns the list of NetworkCloudlets this app is running on.
	 */
	public ArrayList<NetworkCloudlet> getCloudletList() {
		return clist;
	}

	/**
	 * Sets the list of NetworkCloudlets this app is running on.
	 * param clist
	 */
	public void setCloudletList(ArrayList<NetworkCloudlet> clist) {
		this.clist = clist;
	}

	public int getAppID() {
		return appID;
	}

	public void setAppID(int appID) {
		this.appID = appID;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getVmId() {
		return vmId;
	}

	public void setVmId(int vmId) {
		this.vmId = vmId;
	}

}
