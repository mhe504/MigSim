package org.cloudbus.cloudsim.network.datacenter;


/**
 * This refactored abstract version of CloudSim's original TaskStage 
 * object. Fields which are only populated when a TaskStage is in
 * a particular state have been moved to {@link RecvTaskStage},
 * {@link SendTaskStage}, and {@link ExecutionTaskStage}. No MigSim
 * specific features existing in these objects.
 * 
 * @author mhe504@york.ac.uk
 *
 */
public abstract class TaskStage {

	private boolean ignoreInResults;
	public boolean getIgnoreInResults() {
		return ignoreInResults;
	}

	public void setIgnoreInResults(boolean ignoreInResults) {
		this.ignoreInResults = ignoreInResults;
	}

}
