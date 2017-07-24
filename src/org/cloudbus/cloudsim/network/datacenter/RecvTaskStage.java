package org.cloudbus.cloudsim.network.datacenter;


/**
 * This represents a TaskStage in the receive state.
 * It is a refactoring of CloudSim's original TaskStage 
 * object to remove redundant fields.
 * 
 * @author mhe504@york.ac.uk
 *
 */
public class RecvTaskStage extends TaskStage {
	
	private int vmId;

	public RecvTaskStage(int vmId) {
		setVmId(vmId);
	}
	
	public int getVmId() {
		return vmId;
	}

	public void setVmId(int vmId) {
		this.vmId = vmId;
	}

}
