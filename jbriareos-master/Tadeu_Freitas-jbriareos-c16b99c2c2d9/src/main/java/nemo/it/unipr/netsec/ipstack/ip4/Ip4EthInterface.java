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

package nemo.it.unipr.netsec.ipstack.ip4;


import nemo.it.unipr.netsec.ipstack.arp.ArpClient;
import nemo.it.unipr.netsec.ipstack.arp.ArpLayer;
import nemo.it.unipr.netsec.ipstack.arp.ArpServer;
import nemo.it.unipr.netsec.ipstack.ethernet.EthAddress;
import nemo.it.unipr.netsec.ipstack.ethernet.EthLayer;
import nemo.it.unipr.netsec.ipstack.ethernet.EthMulticastAddress;
import nemo.it.unipr.netsec.ipstack.ethernet.EthPacket;
import nemo.it.unipr.netsec.ipstack.net.*;
import nemo.org.zoolu.util.ByteUtils;
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.SystemUtils;


/** IPv4 interface for sending and receiving IPv4 packets through an Ethernet-like layer.
 * <p>
 * Layer-two address resolution is performed through the ARP protocol.
 */
public class Ip4EthInterface extends NetInterface {
	
	/** Debug mode suppress output for SSH packet */
	public static boolean DEBUG_SUPPRESS_SSH_OUTPUT=true;
	
	/** Debug mode */
	public static boolean DEBUG=false;

	/** Prints a debug message. */
	private void debug(String str) {
		SystemUtils.log(LoggerLevel.DEBUG,getClass(),str);
	}

	/** ARP table timeout */
	public static long ARP_TABLE_TIMEOUT=20000;
	
	/** Ethernet layer */
	EthLayer eth_layer;

	/** ARP client */
	ArpClient arp_client=null;

	/** ARP server */
	ArpServer arp_server=null;

	/** This Ethernet listener */
	LayerListener this_eth_listener;
	

	
	/** Creates a new IP interface.
	 * @param eth_ni an Ethernet-like interface
	 * @param ip_addr the IP address and prefix length */
	public Ip4EthInterface(NetInterface eth_ni, Ip4AddressPrefix ip_addr) {
		this(new EthLayer(eth_ni),ip_addr);
	}

		
	/** Creates a new IP interface.
	 * @param eth_layer the Ethernet layer
	 * @param ip_addr the IP address and prefix length */
	public Ip4EthInterface(EthLayer eth_layer, Ip4AddressPrefix ip_addr) {
		super(ip_addr);
		this.eth_layer=eth_layer;
		eth_layer.getEthInterface().addAddress(new EthMulticastAddress(ip_addr));
		this_eth_listener=new LayerListener() {
			@Override
			public void onIncomingPacket(Layer layer, Packet pkt) {
				processIncomingPacket(pkt);
			}
		};
		eth_layer.addListener(new Integer(EthPacket.ETH_IP4),this_eth_listener);
		ArpLayer arp_layer=new ArpLayer(eth_layer);
		arp_client=new ArpClient(arp_layer,ip_addr,ARP_TABLE_TIMEOUT);
		arp_server=new ArpServer(arp_layer,ip_addr);
	}

	
	/** Gets the Ethernet address.
	 * @return the address */
	public EthAddress getEthAddress() {
		return (EthAddress)eth_layer.getAddress();
	}

	/*/** Gets addresses of attached networks.
	 * @return the network addresses */
	/*public Ip4Prefix[] getNetAddresses() {
		return net_addresses;
	}*/

	
	@Override
	public void send(final Packet pkt, final Address dest_addr) {
		(new Thread() {
			public void run() {
				Ip4Packet ip_pkt=(Ip4Packet)pkt;
				if (DEBUG) debug("send(): IP packet: "+ip_pkt);
				EthAddress dst_eth_addr=null;
				if (dest_addr.equals(Ip4Address.ADDR_BROADCAST)) dst_eth_addr=EthAddress.BROADCAST_ADDRESS;
				else {
					dst_eth_addr=arp_client.lookup((Ip4Address)dest_addr);
				}
				if (dst_eth_addr!=null) {
					EthPacket eth_pkt=new EthPacket(eth_layer.getAddress(),dst_eth_addr,EthPacket.ETH_IP4,ip_pkt.getBytes());
					eth_layer.send(eth_pkt);
					if (DEBUG) debug("send(): IP packet sent to "+dst_eth_addr);
					// promiscuous mode
					for (NetInterfaceListener li : promiscuous_listeners) {
						try { li.onIncomingPacket(Ip4EthInterface.this,ip_pkt); } catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				else {
					if (DEBUG) debug("send(): no Ethernet adddress found for "+dest_addr+": packet discarded");
				}
			}
		}).start();
	}

	
	/** Processes an incoming Ethernet packet. */
	private void processIncomingPacket(Packet pkt) {
		EthPacket eth_pkt=(EthPacket)pkt;
		if (eth_pkt.getType()!=EthPacket.ETH_IP4) {
			throw new RuntimeException("It is not an IPv4 packet (type=0x"+ Integer.toHexString(eth_pkt.getType())+")");
		}
		Ip4Packet ip_pkt=Ip4Packet.parseIp4Packet(eth_pkt.getPayloadBuffer(),eth_pkt.getPayloadOffset(),eth_pkt.getPayloadLength());
		if (DEBUG)
			if (!DEBUG_SUPPRESS_SSH_OUTPUT || ip_pkt.getProto()!=Ip4Packet.IPPROTO_TCP || (ByteUtils.twoBytesToInt(ip_pkt.getPayloadBuffer(),ip_pkt.getPayloadOffset())!=22 && ByteUtils.twoBytesToInt(ip_pkt.getPayloadBuffer(),ip_pkt.getPayloadOffset()+2)!=22)) 
				debug("processIncomingPacket(): IP packet: "+ip_pkt);
		// promiscuous mode
		for (NetInterfaceListener li : promiscuous_listeners) {
			try { li.onIncomingPacket(this,ip_pkt); } catch (Exception e) {
				e.printStackTrace();
			}
		}
		// non-promiscuous mode
		for (NetInterfaceListener li : listeners) {
			try { li.onIncomingPacket(this,ip_pkt); } catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	@Override
	public void close() {
		arp_client.close();
		arp_server.close();
		eth_layer.removeListener(this_eth_listener);
		super.close();
	}	

}
