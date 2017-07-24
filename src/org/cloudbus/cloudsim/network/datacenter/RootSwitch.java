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

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

/**
 * This class allows to simulate Root switch which connects Datacenters to external network. 
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
 * @since CloudSim Toolkit 3.0
 */
public class RootSwitch extends Switch {
	
    /**
     * List of downlink switches.
     */
	private List<AggregateSwitch> downlinkswitches;

    /**
     * Map of packets sent to switches on the downlink,
     * where each key is a switch id and the corresponding
     * value is the packets sent to that switch.
     */
	public Map<Integer, List<NetworkPacket>> downlinkswitchpktlist;
	
	/**
	 * Instantiates a Root Switch specifying what other switches are connected to its downlink
	 * ports, and corresponding bandwidths.
	 * 
	 * @param name Name of the root switch
	 * @param level At which level the switch is with respect to hosts.
	 * @param dc The Datacenter where the switch is connected to
	 */
	public RootSwitch(String name, int level, NetworkDatacenter dc) {
		super(name, level, dc);
		downlinkswitchpktlist = new HashMap<Integer, List<NetworkPacket>>();
		setDownlinkswitches(new ArrayList<AggregateSwitch>());
		numport = NetworkConstants.RootSwitchPort;
	}

	protected void processpacket_up(SimEvent ev) {

		// packet coming from down level router.
		// has to send up
		// check which switch to forward to
		// add packet in the switch list

		NetworkPacket hspkt = (NetworkPacket) ev.getData();
		int recvVMid = hspkt.pkt.reciever;
		CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.Network_Event_send));
		schedule(getId(), getLatency(), CloudSimTags.Network_Event_send);

		if (level == NetworkConstants.ROOT_LEVEL) {
			// get id of edge router
			int edgeswitchid = dc.VmToSwitchid.get(recvVMid);
			// search which aggregate switch has it
			int aggSwtichid = -1;
			;
			for (AggregateSwitch sw : getDownlinkswitches()) {
				for (EdgeSwitch edge : sw.getDownlinkswitches()) {
					if (edge.getId() == edgeswitchid) {
						aggSwtichid = sw.getId();
						break;
					}
				}
			}
			if (aggSwtichid < 0) {
				Log.printLine(" No destination for this packet");
			} else {
				List<NetworkPacket> pktlist = downlinkswitchpktlist.get(aggSwtichid);
				if (pktlist == null) {
					pktlist = new ArrayList<NetworkPacket>();
					downlinkswitchpktlist.put(aggSwtichid, pktlist);
				}
				pktlist.add(hspkt);
			}
		}
	}

	public void processEvent(SimEvent ev) {
		
		switch (ev.getTag()) {
			case CloudSimTags.Network_Event_UP:
				// process the packet from down switch or host
				processpacket_up(ev);
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
	private void processpacketforward(SimEvent ev) {
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
	}

	public List<AggregateSwitch> getDownlinkswitches() {
		return downlinkswitches;
	}

	public void setDownlinkswitches(List<AggregateSwitch> downlinkswitches) {
		this.downlinkswitches = downlinkswitches;
	}

}
