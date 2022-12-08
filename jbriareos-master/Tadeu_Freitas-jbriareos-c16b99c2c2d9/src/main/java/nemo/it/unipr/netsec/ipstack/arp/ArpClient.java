/*
 * Copyright 2018 NetSec Lab - University of Parma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package nemo.it.unipr.netsec.ipstack.arp;


import nemo.it.unipr.netsec.ipstack.ethernet.EthAddress;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Address;
import nemo.it.unipr.netsec.ipstack.net.Layer;
import nemo.it.unipr.netsec.ipstack.net.LayerListener;
import nemo.it.unipr.netsec.ipstack.net.Packet;
import nemo.org.zoolu.util.*;

import java.util.Enumeration;
import java.util.Hashtable;


/** ARP client.
 * It gets the Ethernet address of a target IPv4 address.
 */
public class ArpClient {
	
	/** Debug mode */
	public static boolean DEBUG=false;
	
	/** Prints a debug message. */
	private void debug(String str) {
		SystemUtils.log(LoggerLevel.DEBUG,getClass(),str);
	}

	
	/** Maximum number of attempts */
	public static int MAXIMUM_ATTEMPTS=3;

	/** Retransmission timeout [millisecs] */
	public static long RETRANSMISSION_TIMEOUT=3000;
	
	
	/** ARP layer */
	ArpLayer arp_layer;

	/** Local Ethernet address */
	EthAddress local_eth_addr;

	/** Local IPv4 address */
	Ip4Address local_ip_addr;

	/** Target Ethernet address */
	EthAddress target_eth_addr;

	/** Target IPv4 address */
	Ip4Address target_ip_addr;

	/** Listener for incoming ARP messages */ 
	LayerListener this_arp_listener;

	/** Retransmission timer */ 
	Timer retransmission_timer=null;

	/** ARP table */
	Hashtable<Ip4Address,ArpRecord> arp_table=null;
	
	/** ARP table timeout in milliseconds, that is the amount of time that a mapping is cached with the local ARP table */
	long arp_table_timeout;


	
	/** Creates a new ARP client.
	 * @param arp_layer the ARP layer
	 * @param local_ip_addr the local IP address
	 * @param arp_table_timeout ARP table timeout in milliseconds; if greater than 0, the ARP responses are cached in a local ARP table for this amount of time */
	public ArpClient(ArpLayer arp_layer, Ip4Address local_ip_addr, long arp_table_timeout) {
		this.arp_layer=arp_layer;
		this.local_eth_addr=(EthAddress)arp_layer.getAddress();
		this.local_ip_addr=local_ip_addr;
		this.arp_table_timeout=arp_table_timeout;
		this_arp_listener=new LayerListener() {
			@Override
			public void onIncomingPacket(Layer layer, Packet pkt) {
				processIncomingPacket(pkt);
			}
		};
		if (arp_table_timeout>0) arp_table=new Hashtable<Ip4Address,ArpRecord>();
	}

	
	/** Gets the Ethernet address of a target IP address.
	 * It sends an ARP request for the given IP address and captures the ARP response.
	 * <p>
	 * This is a blocking method. It waits for a response and returns only when a response is received or the maximum number of attempts occurred. 
	 * @param target_ip_addr the target IP address
	 * @return the requested Ethernet address, or <i>null</i> in case of failure */
	public synchronized EthAddress request(Ip4Address target_ip_addr) {
		this.target_ip_addr=target_ip_addr;
		target_eth_addr=null;
		arp_layer.addListener(new Integer(ArpPacket.ARP_REPLY),this_arp_listener);
		try {
			ArpPacket arp_pkt=new ArpPacket(local_eth_addr,EthAddress.BROADCAST_ADDRESS,ArpPacket.ARP_REQUEST,local_eth_addr,local_ip_addr,null,target_ip_addr);
			int remaining_attempts=MAXIMUM_ATTEMPTS;
			TimerListener this_timer_listener=new TimerListener() {
				@Override
				public void onTimeout(Timer t) {
					processTimeout(t);				
				}	
			};
			while (target_eth_addr==null && remaining_attempts>0) {
				if (DEBUG) debug("request(): who-has "+target_ip_addr+"? tell "+local_ip_addr);
				arp_layer.send(arp_pkt);
				retransmission_timer=Clock.getDefaultClock().newTimer(RETRANSMISSION_TIMEOUT,0,this_timer_listener);
				retransmission_timer.start(true);
				remaining_attempts--;
				// wait for the response
				synchronized (target_ip_addr) {
					if (target_eth_addr==null) try { target_ip_addr.wait(); } catch (InterruptedException e) {}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		arp_layer.removeListener(this_arp_listener);
		if (DEBUG) debug("request(): response: "+target_eth_addr);
		return target_eth_addr;
	}
	
	
	/** Gets the Ethernet address of a target IP address.
	 * It first looks into a local ARP table; if the address is not found, a ARP request is sent.
	 * @param target_ip_addr the IP address
	 * @return the requested Ethernet address, or <i>null</i> in case of failure */
	public EthAddress lookup(Ip4Address target_ip_addr) {
		if (DEBUG) debug("lookup(): "+target_ip_addr);
		EthAddress eth_addr=null;
		if (DEBUG) {
			StringBuffer sb=new StringBuffer();
			for (Enumeration<Ip4Address> i = arp_table.keys(); i.hasMoreElements(); ) sb.append(i.nextElement()).append(" ");
			debug("lookup(): ARP table: "+sb.toString());
		}
		if (arp_table!=null && arp_table.containsKey(target_ip_addr)) {
			ArpRecord record=arp_table.get(target_ip_addr);
			if ((record.getTime()+arp_table_timeout)>Clock.getDefaultClock().currentTimeMillis()) {
				eth_addr=record.getAddress();
				if (DEBUG) debug("lookup(): from ARP table: "+eth_addr);
			}
			else arp_table.remove(target_ip_addr);
		}
		if (eth_addr==null) {
			eth_addr=request(target_ip_addr);
			if (DEBUG) debug("lookup(): from network: "+eth_addr);
			if (eth_addr!=null && arp_table!=null && arp_table_timeout>0) arp_table.put(target_ip_addr,new ArpRecord(eth_addr,Clock.getDefaultClock().currentTimeMillis()));
		}
		return eth_addr;
	}

	
	/** Processes an incoming ARP packet. */
	protected void processIncomingPacket(Packet pkt) {
		try {
			//if (DEBUG) debug("processIncomingPacket(): ARP");
			ArpPacket arp_pkt=(ArpPacket)pkt;
			if (arp_pkt.getOperation()!=ArpPacket.ARP_REPLY) {
				throw new RuntimeException("It is not an ARP REPLY ("+arp_pkt.getOperation()+")");
			}
			// else
			//if (DEBUG) debug("processIncomingPacket(): ARP_REPLY");
			Ip4Address ip_addr=new Ip4Address(arp_pkt.getSenderProtocolAddress());
			//if (DEBUG) debug("processIncomingPacket(): IP address: "+ip_addr);
			if (ip_addr.equals(target_ip_addr)) {
				target_eth_addr=new EthAddress(arp_pkt.getSenderHardwareAddress());
				if (DEBUG) debug("processIncomingPacket(): "+target_ip_addr+" is-at "+target_eth_addr);
				synchronized (target_ip_addr) {
					target_ip_addr.notifyAll();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/** Processes retransmission timeout. */
	protected void processTimeout(Timer t) {
		synchronized (target_ip_addr) {
			target_ip_addr.notifyAll();
		}
	}

	
	/** Closes the ARP client. */ 
	public void close() {
		arp_layer.close();
	}	

}
