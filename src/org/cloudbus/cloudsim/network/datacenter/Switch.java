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
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * Represents a Network Switch.
 * @todo attributes should be private
 */
public abstract class Switch extends SimEntity {

	/** The switch id */
	public int id;

        /**
         * The level (layer) of the switch in the network topology.
         */
	public int level;


	/**
         * The switch type: edge switch or aggregation switch.
         * @todo should be an enum
         */
	int type;

        /**
         * Bandwitdh of uplink.
         */
	private double uplinkbandwidth;

        /**
         * Bandwitdh of downlink.
         */
	private double downlinkbandwidth;

        /**
         * The latency of the network where the switch is connected to.
		 * 			AND / OR
		 * The time the switch spends to process a received packet.
         * This time is considered constant no matter how many packets 
         * the switch have to process.
         */
	private double latency;

	public double numport;

        /**
         * The datacenter where the switch is connected to.
         * @todo It doesn't appear to be used
         */
	public NetworkDatacenter dc;

        /**
         * List of  received packets.
         */
	public ArrayList<NetworkPacket> pktlist = new ArrayList<NetworkPacket>();

        /**
         * A map of VMs connected to this switch.
         * @todo The list doesn't appear to be updated (VMs added to it) anywhere. 
         */
	public Map<Integer, NetworkVm> Vmlist = new HashMap<Integer, NetworkVm>();

	public Switch(String name, int level, NetworkDatacenter dc) {
		super(name);
		this.level = level;
		this.dc = dc;
	}

	@Override
	public void startEntity() {
		Log.printConcatLine(getName(), " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

        /**
	 * Process non-default received events that aren't processed by
         * the {@link #processEvent(org.cloudbus.cloudsim.core.SimEvent)} method.
         * This method should be overridden by subclasses in other to process
         * new defined events.
         *  
         */
	protected void processOtherEvent(SimEvent ev) {

	}


	@Override
	public void shutdownEntity() {
		Log.printConcatLine(getName(), " is shutting down...");
	}

	public double getUplinkbandwidth() {
		return uplinkbandwidth;
	}

	public void setUplinkbandwidth(double uplinkbandwidth) {
		this.uplinkbandwidth = uplinkbandwidth;
	}

	public double getDownlinkbandwidth() {
		return downlinkbandwidth;
	}

	public void setDownlinkbandwidth(double downlinkbandwidth) {
		this.downlinkbandwidth = downlinkbandwidth;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}

}
