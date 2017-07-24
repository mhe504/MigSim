package uk.ac.york.mhe504.components;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.AppCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;

public class MiddlewareApp extends AppCloudlet {
	
	//USD
	public static final double MW_COMPUTE_INSTANCE_HOUR = 0.175; //dms.c4.xlarge (Single AZ)
	public static final double MW_STORAGE_GB_PM = 0.00; //EBS General Purpose SSD
  	public static final double MW_TRANSFER_MILLION_IOPS = 0.00; //EC2 Internet In

  	
  	public static final int MIDDLEWARE_CLOUDLET_ID = 2000;
	
	public MiddlewareApp(int type, int appID, double deadline, int numbervm, int userId, int vmId) {
		super(type, appID, deadline, numbervm, userId, vmId);
		setExecTime(100);
		this.setNumberOfVMs(1); //Also the number of cloudlets
	}

	@Override
	public void createCloudletList() {
		
		long fileSize = NetworkConstants.FILE_SIZE;
		long outputSize = NetworkConstants.OUTPUT_SIZE;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		NetworkCloudlet mwCloudlet = new NetworkCloudlet(
				MIDDLEWARE_CLOUDLET_ID,
				0,
				1,
				fileSize,
				outputSize,
				utilizationModel,
				utilizationModel,
				utilizationModel,
				0,
				MW_COMPUTE_INSTANCE_HOUR,
				MW_STORAGE_GB_PM,
				MW_TRANSFER_MILLION_IOPS,
				"Middleware");
		NetworkConstants.currentCloudletId++;
		mwCloudlet.setUserId(getUserId());
		mwCloudlet.submittime = CloudSim.clock();
		mwCloudlet.setVmId(getVmId());

		getCloudletList().add(mwCloudlet);

		
	}
	
}