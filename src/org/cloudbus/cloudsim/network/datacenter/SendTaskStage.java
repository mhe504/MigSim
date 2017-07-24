package org.cloudbus.cloudsim.network.datacenter;


/**
 * This represents a TaskStage in the send state.
 * It is a refactoring of CloudSim's original TaskStage 
 * object to remove redundant fields.
 * 
 * @author mhe504@york.ac.uk
 *
 */
public class SendTaskStage extends TaskStage{

    /**
     * The data length generated for the task (in bytes).
     */
	private long data;

	/**
	 * Target NetworkCloudlet ID (to receive the data from or send the data to)
	 */
	private int cloudletId;

    /**
     * Target VM ID (the VM the NetworkCloudlet is running on)
     */
	private int vmId;

	public SendTaskStage(long data, int vmId, int cloudletId) {
		setData(data);
		setVmId(vmId);
		setCloudletId(cloudletId);
	}
	public int getVmId() {
		return vmId;
	}

	public void setVmId(int vmId) {
		this.vmId = vmId;
	}

	public int getCloudletId() {
		return cloudletId;
	}

	public void setCloudletId(int cloudletId) {
		this.cloudletId = cloudletId;
	}

	public long getData() {
		return data;
	}

	public void setData(long data) {
		this.data = data;
	}


}
