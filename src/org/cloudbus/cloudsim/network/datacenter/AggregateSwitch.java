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
import java.util.Map.Entry;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

/**
 * This class represents an Aggregate Switch in a Datacenter network. 
 * It interacts with other switches in order to exchange packets.
 * 
 * <br/>Please refer to following publication for more details:<br/>
 * <ul>
 * <li><a href="http://dx.doi.org/10.1109/UCC.2011.24">Saurabh Kumar Garg and Rajkumar Buyya, NetworkCloudSim: Modelling Parallel Applications in Cloud
 * Simulations, Proceedings of the 4th IEEE/ACM International Conference on Utility and Cloud
 * Computing (UCC 2011, IEEE CS Press, USA), Melbourne, Australia, December 5-7, 2011.</a>
 * </ul>
 *  
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 1.0
 */
public class AggregateSwitch extends Switch {
	
    /**
     * List of uplink switches.
     */
	private List<RootSwitch> uplinkswitches;

    /**
     * List of downlink switches.
     */
	private List<EdgeSwitch> downlinkswitches;
	
    /**
     * Map of packets sent to switches on the downlink,
     * where each key is a switch id and the corresponding
     * value is the packets sent to that switch.
     */
	public Map<Integer, List<NetworkPacket>> downlinkswitchpktlist;
	
    /**
     * Map of packets sent to switches on the uplink,
     * where each key is a switch id and the corresponding
     * value is the packets sent to that switch.
     */
	public Map<Integer, List<NetworkPacket>> uplinkswitchpktlist;

	/**
	 * Instantiates a Aggregate Switch specifying the switches that are connected to its
	 * downlink and uplink ports and corresponding bandwidths.
	 * 
	 * @param name Name of the switch
	 * @param level At which level the switch is with respect to hosts.
	 * @param dc The Datacenter where the switch is connected to
	 */
	public AggregateSwitch(String name, int level, NetworkDatacenter dc) {
		super(name, level, dc);
		downlinkswitchpktlist = new HashMap<Integer, List<NetworkPacket>>();
		uplinkswitchpktlist = new HashMap<Integer, List<NetworkPacket>>();
		numport = NetworkConstants.AggSwitchPort;
		setUplinkswitches(new ArrayList<RootSwitch>());
		setDownlinkswitches(new ArrayList<EdgeSwitch>());
	}

	private void processpacket_down(SimEvent ev) {
		// packet coming from up level router.
		// has to send downward
		// check which switch to forward to
		// add packet in the switch list
		// add packet in the host list
		NetworkPacket hspkt = (NetworkPacket) ev.getData();
		int recvVMid = hspkt.pkt.reciever;
		CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.Network_Event_send));
		schedule(getId(), getLatency(), CloudSimTags.Network_Event_send);

		if (level == NetworkConstants.Agg_LEVEL) {
			// packet is coming from root so need to be sent to edgelevel swich
			// find the id for edgelevel switch
			int switchid = dc.VmToSwitchid.get(recvVMid);
			List<NetworkPacket> pktlist = downlinkswitchpktlist.get(switchid);
			if (pktlist == null) {
				pktlist = new ArrayList<NetworkPacket>();
				downlinkswitchpktlist.put(switchid, pktlist);
			}
			pktlist.add(hspkt);
			return;
		}

	}
	
	/**
	 * Sends a packet to switches connected through a uplink port.
	 * 
	 * @param ev Event/packet to process
	 */
	protected void processpacket_up(SimEvent ev) {
		// packet coming from down level router.
		// has to be sent up.
		// check which switch to forward to
		// add packet in the switch list
		//
		// int src=ev.getSource();
		NetworkPacket hspkt = (NetworkPacket) ev.getData();
		int recvVMid = hspkt.pkt.reciever;
		CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.Network_Event_send));
		schedule(getId(), getLatency(), CloudSimTags.Network_Event_send);

		if (level == NetworkConstants.Agg_LEVEL) {
			// packet is coming from edge level router so need to be sent to
			// either root or another edge level swich
			// find the id for edgelevel switch
			int switchid = dc.VmToSwitchid.get(recvVMid);
			boolean flagtoswtich = false;
			for (Switch sw : getDownlinkswitches()) {
				if (switchid == sw.getId()) {
					flagtoswtich = true;
				}
			}
			if (flagtoswtich) {
				List<NetworkPacket> pktlist = downlinkswitchpktlist.get(switchid);
				if (pktlist == null) {
					pktlist = new ArrayList<NetworkPacket>();
					downlinkswitchpktlist.put(switchid, pktlist);
				}
				pktlist.add(hspkt);
			} else// send to up
			{
				RootSwitch sw = getUplinkswitches().get(0);
				List<NetworkPacket> pktlist = uplinkswitchpktlist.get(sw.getId());
				if (pktlist == null) {
					pktlist = new ArrayList<NetworkPacket>();
					uplinkswitchpktlist.put(sw.getId(), pktlist);
				}
				pktlist.add(hspkt);
			}
		}
	}

	@Override
	public void processEvent(SimEvent ev) {
		
		switch (ev.getTag()) {
			case CloudSimTags.Network_Event_UP:
				// process the packet from down switch or host
				processpacket_up(ev);
				break;
			case CloudSimTags.Network_Event_DOWN:
				// process the packet from uplink
				processpacket_down(ev);
				break;
			case CloudSimTags.Network_Event_send:
				processpacketforward(ev);
				break;
			default:
				// other unknown tags are processed by this method
				processOtherEvent(ev);
				break;
		}
	}
	
	/**
	 * Sends a packet to hosts connected to the switch
	 * 
	 * @param ev Event/packet to process
	 */
	protected void processpacketforward(SimEvent ev) {
		// search for the host and packets..send to them
		if (downlinkswitchpktlist != null) {
			for (Entry<Integer, List<NetworkPacket>> es : downlinkswitchpktlist.entrySet()) {
				int tosend = es.getKey();
				List<NetworkPacket> hspktlist = es.getValue();
				if (!hspktlist.isEmpty()) {
					double avband = getDownlinkbandwidth() / hspktlist.size();
					Iterator<NetworkPacket> it = hspktlist.iterator();
					while (it.hasNext()) {
						NetworkPacket hspkt = it.next();
						double delay = 1000 * hspkt.pkt.data / avband;

						this.send(tosend, delay, CloudSimTags.Network_Event_DOWN, hspkt);
					}
					hspktlist.clear();
				}
			}
		}
		if (uplinkswitchpktlist != null) {
			for (Entry<Integer, List<NetworkPacket>> es : uplinkswitchpktlist.entrySet()) {
				int tosend = es.getKey();
				List<NetworkPacket> hspktlist = es.getValue();
				if (!hspktlist.isEmpty()) {
					double avband = getUplinkbandwidth() / hspktlist.size();
					Iterator<NetworkPacket> it = hspktlist.iterator();
					while (it.hasNext()) {
						NetworkPacket hspkt = it.next();
						double delay = 1000 * hspkt.pkt.data / avband;

						this.send(tosend, delay, CloudSimTags.Network_Event_UP, hspkt);
					}
					hspktlist.clear();
				}
			}
		}

	}

	public List<EdgeSwitch> getDownlinkswitches() {
		return downlinkswitches;
	}

	public void setDownlinkswitches(List<EdgeSwitch> downlinkswitches) {
		this.downlinkswitches = downlinkswitches;
	}

	public List<RootSwitch> getUplinkswitches() {
		return uplinkswitches;
	}

	public void setUplinkswitches(List<RootSwitch> uplinkswitches) {
		this.uplinkswitches = uplinkswitches;
	}

}
