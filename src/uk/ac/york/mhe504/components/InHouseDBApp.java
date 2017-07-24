package uk.ac.york.mhe504.components;

import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.AppCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;

public class InHouseDBApp extends AppCloudlet {

	private Map<String, Long> tableNameRowNumMap;
	private int averageRowSizeBytes;
	//USD
	public static final double COMPUTE_INSTANCE_HOUR = 0.0;
	public static final double STORAGE_GB_PM = 0.0;
	public static final double TRANSFER_MILLION_IOPS = 0.0;
	public static final double AVG_IOPS = 4;
	
	public static final int ID_START_VALUE = 4000;
	
	public InHouseDBApp(int type, int appID, double deadline, int numbervm, int userId, Map<String, Long> tables, int averageRowSizeBytes, int vmId) {
		super(type, appID, deadline, numbervm, userId, vmId);
		setExecTime(100);
		this.setNumberOfVMs(tables.size()); //Also the number of cloudlets
		tableNameRowNumMap=tables;
	}

	@Override
	public void createCloudletList() {
		int cloutletId = ID_START_VALUE;
		
		for(Entry<String, Long> entry : tableNameRowNumMap.entrySet()){
			Long numRows = entry.getValue();
			String name = entry.getKey();
			
			long data = numRows * averageRowSizeBytes;
			long fileSize = NetworkConstants.FILE_SIZE;
			long outputSize = NetworkConstants.OUTPUT_SIZE;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			NetworkCloudlet clc = new NetworkCloudlet(
					cloutletId,
					0,
					1,
					fileSize,
					outputSize,
					utilizationModel,
					utilizationModel,
					utilizationModel,
					data,
					COMPUTE_INSTANCE_HOUR,
					STORAGE_GB_PM,
					TRANSFER_MILLION_IOPS,
					name);
			NetworkConstants.currentCloudletId++;
			clc.setUserId(getUserId());
			clc.submittime = CloudSim.clock();
			clc.setVmId(getVmId());
			getCloudletList().add(clc);
			cloutletId++;
		}
	}
}
