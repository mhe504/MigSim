package org.cloudbus.cloudsim.network.datacenter;


/**
 * This represents a TaskStage in the execution state.
 * It is a refactoring of CloudSim's original TaskStage 
 * object to remove redundant fields.
 * 
 * @author mhe504@york.ac.uk
 *
 */
public class ExecutionTaskStage extends TaskStage{
	
    /**
     * Execution time for this stage (in seconds). 
     */
	private double time;
	
	public ExecutionTaskStage(double time) {
		setTime(time);
	}
	
	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}


}
