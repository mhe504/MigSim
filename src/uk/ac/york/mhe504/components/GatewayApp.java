package uk.ac.york.mhe504.components;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.AppCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;

public class GatewayApp extends AppCloudlet {
  	
	public static final double GTW_COMPUTE_INSTANCE_HOUR = 0.119; // m4.large
	public static final double GTW_STORAGE_GB_PM = 0.00; //EBS General Purpose SSD
  	public static final double GTW_TRANSFER_MILLION_IOPS = 0.00; //EC2 Internet In
  	
  	public static final int GATEWAY_CLOUDLET_ID = 2001;


	public GatewayApp(int type, int appID, double deadline, int numbervm, int userId, int vmId) {
		super(type, appID, deadline, numbervm, userId, vmId);
		setExecTime(100);
		this.setNumberOfVMs(1); //Also the number of cloudlets
	}

	@Override
	public void createCloudletList() {
		
		long fileSize = NetworkConstants.FILE_SIZE;
		long outputSize = NetworkConstants.OUTPUT_SIZE;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		
		NetworkCloudlet gtwCloudlet = new NetworkCloudlet(
				GATEWAY_CLOUDLET_ID,
				0,
				1,
				fileSize,
				outputSize,
				utilizationModel,
				utilizationModel,
				utilizationModel,
				0,
				GTW_COMPUTE_INSTANCE_HOUR,
				GTW_STORAGE_GB_PM,
				GTW_TRANSFER_MILLION_IOPS,
				"Gateway");
		
		NetworkConstants.currentCloudletId++;
		gtwCloudlet.setUserId(getUserId());
		gtwCloudlet.submittime = CloudSim.clock();
		gtwCloudlet.setVmId(getVmId());

		getCloudletList().add(gtwCloudlet);

	}

}
