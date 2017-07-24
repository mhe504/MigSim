package uk.ac.york.mhe504.components;

import java.util.Map;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.AppCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;

public class CloudDBApp extends AppCloudlet {

	public static final int ID_START_VALUE = 6000;

	//Table Names and Sizes
	private Map<String, Long> tableNameRowNumMap;
	
	//Associated Costs
	private double computeInstanceHourCost;
	private double storageGbPerMonthCost;
	private double transferMillionIopsCost;
	
	
	public CloudDBApp(int type, int appID, double deadline, int numTables, int userId, Map<String, Long> tables, int vmId,
					 double hourCost, double storageCost, double iopsCost) {
		
		super(type, appID, deadline, numTables, userId, vmId);
		setExecTime(100);
		this.setNumberOfVMs(tables.size()); //Also the number of cloudlets
		
		this.tableNameRowNumMap=tables; 
		setComputeInstanceHourCost(hourCost);
		setStorageGbPerMonthCost(storageCost);
		setTransferMillionIopsCost(iopsCost);
	}

	@Override
	public void createCloudletList() {
	
		int cloutletId = ID_START_VALUE;
		for(String name : tableNameRowNumMap.keySet()){
		
			long fileSize = NetworkConstants.FILE_SIZE;
			long outputSize = NetworkConstants.OUTPUT_SIZE;
			UtilizationModel utilizationModel = new UtilizationModelFull();
			long cloudletLength = 0;
			int pesNumber = 8; //number of cores allocated
			long data = 0; //data stored

			NetworkCloudlet cl = new NetworkCloudlet(
					cloutletId,
					cloudletLength,
					pesNumber,
					fileSize,
					outputSize,
					utilizationModel,
					utilizationModel,
					utilizationModel,
					data,
					getComputeInstanceHourCost(),
					getStorageGbPerMonthCost(),
					getTransferMillionIopsCost(),
					name);
			NetworkConstants.currentCloudletId++;
			cl.setUserId(getUserId());
			cl.submittime = CloudSim.clock();
			cl.setVmId(getVmId());
		
			cloutletId++;
			getCloudletList().add(cl);

		}
	}

	public double getComputeInstanceHourCost() {
		return computeInstanceHourCost;
	}

	public void setComputeInstanceHourCost(double computeInstanceHour) {
		this.computeInstanceHourCost = computeInstanceHour;
	}

	public double getTransferMillionIopsCost() {
		return transferMillionIopsCost;
	}

	public void setTransferMillionIopsCost(double transferMillionIOPS) {
		this.transferMillionIopsCost = transferMillionIOPS;
	}

	public double getStorageGbPerMonthCost() {
		return storageGbPerMonthCost;
	}

	public void setStorageGbPerMonthCost(double storageGbPerMonth) {
		this.storageGbPerMonthCost = storageGbPerMonth;
	}
}
