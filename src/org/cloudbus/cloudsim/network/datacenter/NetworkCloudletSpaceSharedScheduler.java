/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network.datacenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;

/**
 * CloudletSchedulerSpaceShared implements a policy of scheduling performed by a virtual machine
 * to run its {@link Cloudlet Cloudlets}. 
 * It consider that there will be only one cloudlet per VM. Other cloudlets will be in a waiting list.
 * We consider that file transfer from cloudlets waiting happens before cloudlet execution. I.e.,
 * even though cloudlets must wait for CPU, data transfer happens as soon as cloudlets are
 * submitted.
 * 
 * Each VM has to have its own instance of a CloudletScheduler.
 * 
 * @author Saurabh Kumar Garg
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 3.0
 * @todo Attributes should be private
 */
public class NetworkCloudletSpaceSharedScheduler extends CloudletScheduler {
	/** The current CPUs. */
	protected int currentCpus;
	
	private int capacity;

	/** The used PEs. */
	protected int usedPes;

        /**
         * The map of packets to send, where each key is a destination VM
         * and each value is the list of packets to sent to that VM.
         */
	public Map<Integer, List<HostPacket>> pkttosend;

        /**
         * The map of packets received, where each key is a sender VM
         * and each value is the list of packets sent by that VM.
         */
	public Map<Integer, List<HostPacket>> pktrecv;
	
	private NetworkDatacenter datacenter;

	/**
	 * Creates a new CloudletSchedulerSpaceShared object. 
         * This method must be invoked before starting the actual simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	public NetworkCloudletSpaceSharedScheduler(NetworkDatacenter dc) {
		super();
		cloudletWaitingList = new ArrayList<ResCloudlet>();
		cloudletExecList = new ArrayList<ResCloudlet>();
		cloudletPausedList = new ArrayList<ResCloudlet>();
		cloudletFinishedList = new ArrayList<ResCloudlet>();
		usedPes = 0;
		currentCpus = 0;
		pkttosend = new HashMap<Integer, List<HostPacket>>();
		pktrecv = new HashMap<Integer, List<HostPacket>>();
		datacenter = dc;
	}

	@Override
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
                /*@todo Method to long. Several "extract method" refactorings may be performed.*/
		
		if (!mipsShare.equals(getCurrentMipsShare()))
		{
			// update capacity & currentCpus
			setCurrentMipsShare(mipsShare);
			int cpus = 0;

			for (Double mips : mipsShare) { // count the CPUs available to the VMM
				capacity += mips;
				if (mips > 0) {
					cpus++;
				}
			}
			currentCpus = cpus;
			capacity /= cpus; // average capacity of each cpu
		}

		for (ResCloudlet rcl : getCloudletExecList()) { // each machine in the
			// exec list has the
			// same amount of cpu

			NetworkCloudlet cl = (NetworkCloudlet) rcl.getCloudlet();
			/*TODO:
			 * Output Cost@time
			 * 
			if (CloudSim.clock() > (lastClockValue+60000))
			{
				lastClockValue = CloudSim.clock();
				Log.printLine(Math.floor(CloudSim.clock()));			
			}
			*/
			
			
			// check status
			// if execution stage
			// update the cloudlet finishtime
			// CHECK WHETHER IT IS WAITING FOR THE PACKET
			// if packet received change the status of job and updateVmProcessingupdate the time.
			//
			if ((cl.currStagenum != -1)) {
				if (cl.currStagenum == NetworkCloudlet.FINISH) {
					break;
				}
				TaskStage st = cl.stages.get(cl.currStagenum);
				if (st instanceof ExecutionTaskStage) {

					cl.timespentInStage = Math.round(CloudSim.clock() - cl.timetostartStage);
					// update the time
					if (cl.timespentInStage >= ((ExecutionTaskStage)st).getTime()) {
						changetonextstage(cl, st);
						// change the stage
					}
					else
					{
						return estimateFinishTime(currentTime, capacity);
					}
				}
				else if (st instanceof RecvTaskStage) {
					List<HostPacket> pktlist = pktrecv.get(((RecvTaskStage)st).getVmId());
					List<HostPacket> pkttoremove = new ArrayList<HostPacket>();
					if (pktlist != null) {
						Iterator<HostPacket> it = pktlist.iterator();
						HostPacket pkt = null;
						if (it.hasNext()) {
							pkt = it.next();
							// Asumption packet will not arrive in the same cycle
							if (pkt.reciever == cl.getVmId()) {
								pkt.recievetime = CloudSim.clock();
								//st.time = CloudSim.clock() - pkt.sendtime;
								changetonextstage(cl, st);
								pkttoremove.add(pkt);
							}
							cl.setData(cl.getData() + new Double(pkt.data).longValue(),CloudSim.clock());
						}
						pktlist.removeAll(pkttoremove);
						// if(pkt!=null)
						// else wait for recieving the packet
					}
				}

			} else {
				cl.currStagenum = 0;
				cl.timetostartStage = CloudSim.clock();

				if (cl.stages.get(0) instanceof ExecutionTaskStage){
					datacenter.schedule(
							datacenter.getId(),
							((ExecutionTaskStage)cl.stages.get(0)).getTime(),
							CloudSimTags.VM_DATACENTER_EVENT);
				} else {
					datacenter.schedule(
							datacenter.getId(),
							0.0001, /*TODO Possible Bug here. Maybe use 3?*/
							CloudSimTags.VM_DATACENTER_EVENT);
				}
			}

		}

		if (getCloudletExecList().size() == 0 && getCloudletWaitingList().size() == 0) { // no
			// more cloudlets in this scheduler
			setPreviousTime(currentTime);
			return 0.0;
		}

		
		List<ResCloudlet> finishedCloutlets = new ArrayList<>();
		
		for (ResCloudlet rcl : getCloudletExecList())
		{
			if (((NetworkCloudlet)rcl.getCloudlet()).currStagenum == NetworkCloudlet.FINISH)
			{
				finishedCloutlets.add(rcl);
			}
		}
		if (!finishedCloutlets.isEmpty())
		{
			finishedCloutlets.forEach(rcl -> cloudletFinish(rcl));
			getCloudletExecList().removeAll(finishedCloutlets);
			
			// for each finished cloudlet, add a new one from the waiting list
			if (!getCloudletWaitingList().isEmpty()) {
				List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
				for (int i = 0; i < finishedCloutlets.size(); i++) {
					toRemove.clear();
					for (ResCloudlet rcl : getCloudletWaitingList()) {
						if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
							rcl.setCloudletStatus(Cloudlet.INEXEC);
							for (int k = 0; k < rcl.getNumberOfPes(); k++) {
								rcl.setMachineAndPeId(0, i);
							}
							getCloudletExecList().add(rcl);
							usedPes += rcl.getNumberOfPes();
							toRemove.add(rcl);
							break;
						}
					}
					getCloudletWaitingList().removeAll(toRemove);
				}// for(cont)
			}
		}
		
		return estimateFinishTime(currentTime, capacity);
	}

	private double estimateFinishTime(double currentTime, double capacity) {
		// estimate finish time of cloudlets in the execution queue
		double nextEvent = Double.MAX_VALUE;
		for (ResCloudlet rcl : getCloudletExecList()) {
			double estimatedFinishTime = currentTime + (rcl.getRemainingCloudletLength() / (capacity * rcl.getNumberOfPes()));
			if (estimatedFinishTime - currentTime < CloudSim.getMinTimeBetweenEvents()) {
				estimatedFinishTime = currentTime + CloudSim.getMinTimeBetweenEvents();
			}
			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}
		}
		setPreviousTime(currentTime);
		return nextEvent;
	}

        /**
         * Changes a cloudlet to the next stage.
         * 
         * @todo It has to be corrected the method name case. Method too long
         * to understand what is its responsibility.*/
	private void changetonextstage(NetworkCloudlet cl, TaskStage st) {
		cl.timespentInStage = 0;
		cl.timetostartStage = CloudSim.clock();
		int currstage = cl.currStagenum;
		if (currstage >= (cl.stages.size() - 1)) {
			cl.currStagenum = NetworkCloudlet.FINISH;
		} else {
			cl.currStagenum++;
			int i = 0;
			for (i = cl.currStagenum; i < cl.stages.size(); i++) {
				if (cl.stages.get(i) instanceof SendTaskStage) {
					SendTaskStage tsk = (SendTaskStage)cl.stages.get(i);
					HostPacket pkt = new HostPacket(
							cl.getVmId(),
							tsk.getVmId(),
							tsk.getData(),
							CloudSim.clock(),
							-1,
							cl.getCloudletId(),
							tsk.getCloudletId());
					List<HostPacket> pktlist = pkttosend.get(cl.getVmId());
					if (pktlist == null) {
						pktlist = new ArrayList<HostPacket>();
					}
					pktlist.add(pkt);
					pkttosend.put(cl.getVmId(), pktlist);
					cl.setData(cl.getData() - new Double(pkt.data).longValue(),CloudSim.clock());
				} else {
					break;
				}

			}
	
			if (i == cl.stages.size()) {
				cl.currStagenum = NetworkCloudlet.FINISH;
			} else {
				cl.currStagenum = i;
				if (cl.stages.get(i) instanceof ExecutionTaskStage) {
					datacenter.schedule(
							datacenter.getId(),
							((ExecutionTaskStage)cl.stages.get(i)).getTime(),
							CloudSimTags.VM_DATACENTER_EVENT);
				}

			}
		}

	}

	@Override
	public Cloudlet cloudletCancel(int cloudletId) {
		// First, looks in the finished queue
		for (ResCloudlet rcl : getCloudletFinishedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletFinishedList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		// Then searches in the exec list
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletExecList().remove(rcl);
				if (rcl.getRemainingCloudletLength() == 0.0) {
					cloudletFinish(rcl);
				} else {
					rcl.setCloudletStatus(Cloudlet.CANCELED);
				}
				return rcl.getCloudlet();
			}
		}

		// Now, looks in the paused queue
		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				getCloudletPausedList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		// Finally, looks in the waiting list
		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				rcl.setCloudletStatus(Cloudlet.CANCELED);
				getCloudletWaitingList().remove(rcl);
				return rcl.getCloudlet();
			}
		}

		return null;

	}

	@Override
	public boolean cloudletPause(int cloudletId) {
		boolean found = false;
		int position = 0;

		// first, looks for the cloudlet in the exec list
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletExecList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0.0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		// now, look for the cloudlet in the waiting list
		position = 0;
		found = false;
		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletWaitingList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0.0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		return false;
	}

	@Override
	public void cloudletFinish(ResCloudlet rcl) {
		((NetworkCloudlet) (rcl.getCloudlet())).finishtime = CloudSim.clock();
		rcl.setCloudletStatus(Cloudlet.SUCCESS);
		rcl.finalizeCloudlet();
		getCloudletFinishedList().add(rcl);
		usedPes -= rcl.getNumberOfPes();
	}

	@Override
	public double cloudletResume(int cloudletId) {
		boolean found = false;
		int position = 0;

		// look for the cloudlet in the paused list
		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			ResCloudlet rcl = getCloudletPausedList().remove(position);

			// it can go to the exec list
			if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
				rcl.setCloudletStatus(Cloudlet.INEXEC);
				for (int i = 0; i < rcl.getNumberOfPes(); i++) {
					rcl.setMachineAndPeId(0, i);
				}

				long size = (long)rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletExecList().add(rcl);
				usedPes += rcl.getNumberOfPes();

				// calculate the expected time for cloudlet completion
				double capacity = 0.0;
				int cpus = 0;
				for (Double mips : getCurrentMipsShare()) {
					capacity += mips;
					if (mips > 0) {
						cpus++;
					}
				}
				currentCpus = cpus;
				capacity /= cpus;

				long remainingLength = (long)rcl.getRemainingCloudletLength();
				double estimatedFinishTime = CloudSim.clock()
						+ (remainingLength / (capacity * rcl.getNumberOfPes()));

				return estimatedFinishTime;
			} else {// no enough free PEs: go to the waiting queue
				rcl.setCloudletStatus(Cloudlet.QUEUED);

				long size = (long)rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletWaitingList().add(rcl);
				return 0.0;
			}

		}

		// not found in the paused list: either it is in in the queue, executing
		// or not exist
		return 0.0;

	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		// it can go to the exec list
		if ((currentCpus - usedPes) >= cloudlet.getNumberOfPes()) {
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			rcl.setCloudletStatus(Cloudlet.INEXEC);
			for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
				rcl.setMachineAndPeId(0, i);
			}

			getCloudletExecList().add(rcl);
			usedPes += cloudlet.getNumberOfPes();
		} else {// no enough free PEs: go to the waiting queue
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			rcl.setCloudletStatus(Cloudlet.QUEUED);
			getCloudletWaitingList().add(rcl);
			return 0.0;
		}

		// calculate the expected time for cloudlet completion
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : getCurrentMipsShare()) {
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}

		currentCpus = cpus;
		capacity /= cpus;

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = capacity * fileTransferTime;
		long length = cloudlet.getCloudletLength();
		length += extraSize;
		cloudlet.setCloudletLength(length);
		return cloudlet.getCloudletLength() / capacity;
	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet) {
		cloudletSubmit(cloudlet, 0);
		return 0;
	}

	@Override
	public int getCloudletStatus(int cloudletId) {
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		return -1;
	}

	@Override
	public double getTotalUtilizationOfCpu(double time) {
		double totalUtilization = 0;
		for (ResCloudlet gl : getCloudletExecList()) {
			totalUtilization += gl.getCloudlet().getUtilizationOfCpu(time);
		}
		return totalUtilization;
	}

	@Override
	public boolean isFinishedCloudlets() {
		return getCloudletFinishedList().size() > 0;
	}

	@Override
	public Cloudlet getNextFinishedCloudlet() {
		if (getCloudletFinishedList().size() > 0) {
			return getCloudletFinishedList().remove(0).getCloudlet();
		}
		return null;
	}

	@Override
	public int runningCloudlets() {
		return getCloudletExecList().size();
	}

	@Override
	public Cloudlet migrateCloudlet() {
		ResCloudlet rcl = getCloudletExecList().remove(0);
		rcl.finalizeCloudlet();
		Cloudlet cl = rcl.getCloudlet();
		usedPes -= cl.getNumberOfPes();
		return cl;
	}

	@Override
	public List<Double> getCurrentRequestedMips() {
		List<Double> mipsShare = new ArrayList<Double>();
		if (getCurrentMipsShare() != null) {
			for (Double mips : getCurrentMipsShare()) {
				mipsShare.add(mips);
			}
		}
		return mipsShare;
	}

	@Override
	public double getTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl, List<Double> mipsShare) {
                /*@todo The param rcl is not being used.*/
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : mipsShare) { // count the cpus available to the vmm
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}
		currentCpus = cpus;
		capacity /= cpus; // average capacity of each cpu
		return capacity;
	}

	@Override
	public double getTotalCurrentAllocatedMipsForCloudlet(ResCloudlet rcl, double time) {
                //@todo The method doesn't appear to be implemented in fact
		return 0.0;
	}

	@Override
	public double getTotalCurrentRequestedMipsForCloudlet(ResCloudlet rcl, double time) {
                //@todo The method doesn't appear to be implemented in fact
		return 0.0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfBw() {
                //@todo The method doesn't appear to be implemented in fact
		return 0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfRam() {
                //@todo The method doesn't appear to be implemented in fact
		return 0;
	}

}
