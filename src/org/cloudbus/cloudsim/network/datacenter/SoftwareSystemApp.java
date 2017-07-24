/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network.datacenter;

import java.util.Map;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;

public class SoftwareSystemApp extends AppCloudlet {

	private Map<String,Long> tableNameRowNumMap;
	public static final double COMPUTE_INSTANCE_HOUR = 0.0;
	public static final double STORAGE_GB_PM = 0.0;
	public static final double TRANSFER_MILLION_IOPS = 0.0;
	
	public static final int ID_START_VALUE = 0;
	
	public SoftwareSystemApp(int type, int appID, double deadline, int numTables, int userId, Map<String, Long> tables, int vmId) {
		super(type, appID, deadline, numTables, userId, vmId);
		setExecTime(100);
		this.setNumberOfVMs(tables.size()); //Also the number of cloudlets
		this.tableNameRowNumMap = tables;
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
					COMPUTE_INSTANCE_HOUR,
					STORAGE_GB_PM,
					TRANSFER_MILLION_IOPS,
					name);
			NetworkConstants.currentCloudletId++;
			cl.setUserId(getUserId());
			cl.submittime = CloudSim.clock();
			cl.setVmId(getVmId());
		
			cloutletId++;
			getCloudletList().add(cl);

		}

	}
}
