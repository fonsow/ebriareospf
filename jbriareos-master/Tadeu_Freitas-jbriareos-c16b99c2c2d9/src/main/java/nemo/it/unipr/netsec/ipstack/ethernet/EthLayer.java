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

package nemo.it.unipr.netsec.ipstack.ethernet;


import nemo.it.unipr.netsec.ipstack.net.*;
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.SystemUtils;


/** Ethernet layer for sending or receiving Ethernet packets through an Ethernet interface.
 */
public class EthLayer extends Layer {

	/** Debug mode */
	public static boolean DEBUG=false;
	
	/** Prints a debug message. */
	private void debug(String str) {
		SystemUtils.log(LoggerLevel.DEBUG,getClass(),str);
	}
	
	/** Ethernet address */
	//EthAddress eth_addr;

	/** Ethernet interface */
	NetInterface eth_ni;

	/** This physical interface listener */
	NetInterfaceListener this_eth_listener;

	
	
	/** Creates a new Ethernet interface.
	 * @param eth_ni Ethernet interface */
	public EthLayer(NetInterface eth_ni) {
		this.eth_ni=eth_ni;
		eth_ni.addAddress(EthAddress.BROADCAST_ADDRESS);
		this_eth_listener=new NetInterfaceListener() {
			@Override
			public void onIncomingPacket(NetInterface ni, Packet pkt) {
				processIncomingPacket(pkt);
			}
		};
		eth_ni.addListener(this_eth_listener);
	}

	
	/** Gets the Ethernet interface
	 * @return the interface */
	public NetInterface getEthInterface() {
		return eth_ni;
	}

	/*@Override
	public NetInterface[] getNetInterfaces() {
		return new NetInterface[]{eth_ni};
	}*/

	@Override
	public Address getAddress() {
		return eth_ni.getAddress();
	}

	@Override
	public void send(Packet pkt) {
		EthPacket eth_pkt=(EthPacket)pkt;
		if (eth_pkt.getSourceAddress()==null) eth_pkt.setSourceAddress(eth_ni.getAddress());
		if (DEBUG) debug("send(): Ethernet packet: "+eth_pkt);
		eth_ni.send(eth_pkt,null);
	}

	
	/** Processes an incoming Ethernet packet. */
	private void processIncomingPacket(Packet pkt) {
		EthPacket eth_pkt=EthPacket.parseEthPacket(pkt.getBytes());
		EthAddress dest_addr=(EthAddress)eth_pkt.getDestAddress();
		if (eth_ni.hasAddress(dest_addr)) {
			if (DEBUG) debug("processIncomingPacket(): Ethernet packet: "+eth_pkt);
			Integer type=eth_pkt.getType();
			LayerListener listener=listeners.get(type);
			if (listener!=null) {
				try { listener.onIncomingPacket(this,eth_pkt); } catch (Exception e) {
					e.printStackTrace();
				}				
			}
		}
	}	

	
	@Override
	public void close() {
		eth_ni.removeListener(this_eth_listener);
		super.close();
	}	

}
