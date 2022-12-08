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
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.SystemUtils;


/** ARP server.
 * It responds to ARP requests for mapping IPv4 addresses to corresponding Ethernet addresses.
 */
public class ArpServer {
	
	/** Debug mode */
	public static boolean DEBUG=false;
	
	/** Prints a debug message. */
	private void debug(String str) {
		SystemUtils.log(LoggerLevel.DEBUG,getClass(),str);
	}

	
	/** ARP layer */
	ArpLayer arp_layer;

	/** Server Ethernet address */
	EthAddress eth_addr;

	/** Server IPv4 address */
	Ip4Address ip_addr;

	
	
	/** Creates a new ARP server.
	 * @param arp_layer the ARP layer
	 * @param ip_addr the IP address */
	public ArpServer(ArpLayer arp_layer, Ip4Address ip_addr) {
		this.arp_layer=arp_layer;
		this.eth_addr=(EthAddress)arp_layer.getAddress();
		this.ip_addr=ip_addr;
		LayerListener this_arp_listener=new LayerListener() {
			@Override
			public void onIncomingPacket(Layer layer, Packet pkt) {
				processIncomingPacket(pkt);
			}
		};
		arp_layer.addListener(new Integer(ArpPacket.ARP_REQUEST),this_arp_listener);
	}

	
	/** Processes an incoming ARP packet. */
	protected void processIncomingPacket(Packet pkt) {
		ArpPacket arp_pkt=(ArpPacket)pkt;
		if (arp_pkt.getOperation()!=ArpPacket.ARP_REQUEST) {
			throw new RuntimeException("It is not an ARP REQUREST ("+arp_pkt.getOperation()+")");
		}
		// else
		if (new Ip4Address(arp_pkt.getTargetProtocolAddress()).equals(ip_addr)) {
			EthAddress remote_eth_addr=new EthAddress(arp_pkt.getSenderHardwareAddress());
			Ip4Address remote_ip_addr=new Ip4Address(arp_pkt.getSenderProtocolAddress());
			if (DEBUG) debug("processIncomingPacket(): ARP_REQUEST: who-has "+ip_addr+"? tell "+remote_ip_addr);
			ArpPacket arp_reply=new ArpPacket(eth_addr,remote_eth_addr,ArpPacket.ARP_REPLY,eth_addr,ip_addr,remote_eth_addr,remote_ip_addr);
			if (DEBUG) debug("processIncomingPacket(): "+ip_addr+" is-at "+eth_addr);
			arp_layer.send(arp_reply);			
		}			
	}

	
	/** Closes the ARP server. */ 
	public void close() {
		arp_layer.close();
	}	

}
