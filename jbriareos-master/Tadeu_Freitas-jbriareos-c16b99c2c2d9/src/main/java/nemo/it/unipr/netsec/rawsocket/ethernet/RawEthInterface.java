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

package nemo.it.unipr.netsec.rawsocket.ethernet;


import nemo.it.unipr.netsec.ipstack.ethernet.EthAddress;
import nemo.it.unipr.netsec.ipstack.ethernet.EthPacket;
import nemo.it.unipr.netsec.ipstack.net.Address;
import nemo.it.unipr.netsec.ipstack.net.NetInterface;
import nemo.it.unipr.netsec.ipstack.net.NetInterfaceListener;
import nemo.it.unipr.netsec.ipstack.net.Packet;
import nemo.it.unipr.netsec.rawsocket.RawLinkSocket;
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.SystemUtils;

import java.net.NetworkInterface;
import java.net.SocketException;


/** Ethernet interface for sending or receiving Ethernet packets through
 * one node network card.
 * <p>
 * It uses {@link nemo.it.unipr.netsec.rawsocket.RawLinkSocket} for capturing
 * and/sending raw Ethernet packets.  
 */
public class RawEthInterface extends NetInterface {

	/** Debug mode */
	public static boolean DEBUG=false;
	
	/** Prints a debug message. */
	private void debug(String str) {
		SystemUtils.log(LoggerLevel.DEBUG,getClass(),str);
	}

	
	/** Receiver buffer size */
	static final int RECV_BUFF_SIZE=1600;
	
	/** Whether the interface is running */
	protected boolean running;
	
	/** Interface name */
	String eth_name;

	/** Ethernet address */
	//EthAddress eth_addr;

	/** Link socket */
	RawLinkSocket socket;

	
	
	/** Creates a new Ethernet interface.
	 * @param eth_name the name of the physical interface 
	 * @throws SocketException */
	public RawEthInterface(String eth_name) throws SocketException {
		super(eth_name,new EthAddress(NetworkInterface.getByName(eth_name).getHardwareAddress()));
		this.eth_name=eth_name;
		//NetworkInterface network_interface=NetworkInterface.getByName(eth_name);
		//eth_addr=new EthAddress(network_interface.getHardwareAddress());
		addAddress(EthAddress.BROADCAST_ADDRESS);
		socket=new RawLinkSocket();
		start();
	}

	
	/** Creates a new Ethernet interface.
	 * @param eth_name the name of the physical interface 
	 * @param eth_addr the Ethernet address associated to this interface 
	 * @throws SocketException */
	public RawEthInterface(String eth_name, EthAddress eth_addr) throws SocketException {
		super(eth_addr);
		this.eth_name=eth_name;
		//this.eth_addr=eth_addr;
		addAddress(EthAddress.BROADCAST_ADDRESS);
		socket=new RawLinkSocket();
		start();
	}

	
	/** Starts the interface. */
	private void start() {
		running=true;
		new Thread() {
			public void run() {
				receiver();
			}
		}.start();
	}

	
	@Override
	public void addAddress(Address addr) {
		super.addAddress(addr);
		if (DEBUG) {
			StringBuffer sb=new StringBuffer();
			for (Address a : getAddresses()) sb.append(a.toString()).append(' ');
			debug("addAddress(): addresses: "+sb.toString());
		}
	}

	
	@Override
	public void send(Packet pkt, Address dest_addr) {
		EthPacket eth_pkt=(EthPacket)pkt;
		if (eth_pkt.getSourceAddress()==null) eth_pkt.setSourceAddress(getAddress());
		if (eth_pkt.getDestAddress()==null) eth_pkt.setDestAddress(dest_addr);
		eth_pkt.setOutInterface(eth_name);
		if (DEBUG) debug("send(): Ethernet packet: "+eth_pkt.toString());
		socket.send(eth_pkt);
		// promiscuous mode
		for (NetInterfaceListener li : promiscuous_listeners.toArray(new NetInterfaceListener[0])) {
			try { li.onIncomingPacket(this,pkt); } catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	/** Receives incoming packets and process them. */
	private void receiver() {
		byte[] buf=new byte[RECV_BUFF_SIZE];
		//byte[] src_addr=new byte[6];
		//byte[] src_port=new byte[0];
		while (running) {
			//int len=socket.recvfrom(buf,0,0,src_addr,src_port);
			int len=socket.recv(buf,0,0);
			EthPacket eth_pkt=EthPacket.parseEthPacket(buf,0,len);
			//eth_pkt.setInInterface(new EthAddress(src_addr));
			EthAddress dest_addr=(EthAddress)eth_pkt.getDestAddress();
			//if (DEBUG) debug("receiver(): received new packet ("+eth_pkt.getType()+") to "+dst_addr);
			// promiscuous mode
			for (NetInterfaceListener li : promiscuous_listeners) {
				try { li.onIncomingPacket(this,eth_pkt); } catch (Exception e) {
					e.printStackTrace();
				}
			}
			// non-promiscuous mode
			if (hasAddress(dest_addr)) {
				if (DEBUG) debug("receiver(): Ethernet packet: "+eth_pkt.toString());
				for (NetInterfaceListener li : listeners) {
					try { li.onIncomingPacket(this,eth_pkt); } catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (DEBUG) debug("receiver(): end");
	}

	
	@Override
	public void close() {
		running=false;
		promiscuous_listeners.clear();
		super.close();
	}

}
