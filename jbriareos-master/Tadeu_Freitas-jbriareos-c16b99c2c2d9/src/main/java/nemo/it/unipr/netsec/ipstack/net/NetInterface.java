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

package nemo.it.unipr.netsec.ipstack.net;


import nemo.org.zoolu.util.Random;

import java.util.ArrayList;


/** A network interface for sending and receiving packets using an underlying protocol, a network interface, or directly a link.
 * <p>
 * A network interface may have one or more addresses.
 * <p>
 * Packets are sent out calling the method {@link #send(Packet, Address)}). This method can take as parameter the address
 * of the node that the packet has to be passed to. This address, if present, may need to be translated to the actual underlying address.
 * If and how this is achieved depends on the specific protocol and should be implemented by a proper NetInterface.
 * <p>
 * Packet are received through a {@link NetInterfaceListener} set through the method {@link #addListener(NetInterfaceListener)}. 
 */
public abstract class NetInterface {

	/** Length of the interface id */
	private static int ID_LEN=8;

	/** Interface id; if name is null, it is a randomly generated hex string */
	private String id=null;
	
	/** Interface name */
	private String name=null;
	
	/** Interface addresses */
	protected ArrayList<Address> addresses=new ArrayList<Address>();
	
	/** Interface listeners */
	protected ArrayList<NetInterfaceListener> listeners=new ArrayList<NetInterfaceListener>();

	/** Interface listeners in 'promiscuous' mode */
	protected ArrayList<NetInterfaceListener> promiscuous_listeners=new ArrayList<NetInterfaceListener>();
	

	
	/** Creates a new interface.
	 * @param name interface name */
	protected NetInterface(String name) {
		this(name,(Address)null);
	}

	
	/** Creates a new interface.
	 * @param addr interface address */
	protected NetInterface(Address addr) {
		this(null,addr);
	}

	
	/** Creates a new interface.
	 * @param name interface name
	 * @param addr interface address */
	protected NetInterface(String name, Address addr) {
		this.name=name;
		if (addr!=null) addresses.add(addr);
	}

	
	/** Creates a new interface.
	 * @param addrs interface addresses */
	protected NetInterface(Address[] addrs) {
		this(null,addrs);
	}

	/** Creates a new interface.
	 * @param name interface name
	 * @param addrs interface addresses */
	protected NetInterface(String name, Address[] addrs) {
		if (name!=null) this.name=name; else id=Random.nextHexString(ID_LEN);
		if (addrs!=null) for (Address a : addrs) addresses.add(a);
	}

	
	/** Adds a listener to this interface for receiving incoming packets targeted to this interface.
	 * @param listener interface listener to be added */
	public void addListener(NetInterfaceListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	
	/** Removes a listener.
	 * @param listener interface listener to be removed */
	public void removeListener(NetInterfaceListener listener) {
		synchronized (listeners) { 
			for (int i=0; i<listeners.size(); i++) {
				NetInterfaceListener li=listeners.get(i);
				if (li==listener) {
					listeners.remove(i);
				}
			}
		}
	}

	
	/** Gets all interface listeners.
	 * @return array of listeners */
	/*protected NetInterfaceListener[] getListeners() {
		synchronized (listeners) { 
			return listeners.toArray(new NetInterfaceListener[0]);
		}
	}*/

		
	/** Adds a listener to this interface for capturing all packets received by the physical interface independently from the destination address ('promiscuous' mode).
	 * @param listener interface listener to be added */
	public void addPromiscuousListener(NetInterfaceListener listener) {
		synchronized (promiscuous_listeners) {
			promiscuous_listeners.add(listener);
		}
	}

	
	/** Removes a listener that had been set in 'promiscuous' mode.
	 * @param listener interface listener to be removed */
	public void removePromiscuousListener(NetInterfaceListener listener) {
		synchronized (promiscuous_listeners) {
			for (int i=0; i<promiscuous_listeners.size(); i++) {
				NetInterfaceListener li=promiscuous_listeners.get(i);
				if (li==listener) {
					promiscuous_listeners.remove(i);
				}
			}	
		}
	}


	/** Gets all promiscuous listeners.
	 * @return array of listeners */
	/*protected NetInterfaceListener[] getPromiscuousListeners() {
		synchronized (promiscuous_listeners) { 
			return promiscuous_listeners.toArray(new NetInterfaceListener[0]);
		}
	}*/

	
	/** Adds an interface address.
	 * @param addr the address */
	public void addAddress(Address addr) {
		synchronized (addresses) {
			addresses.add(addr);
		}
	}

	
	/** Removes an interface address.
	 * @param addr the address */
	public void removeAddress(Address addr) {
		synchronized (addresses) {
			for (int i=0; i<addresses.size(); i++) {
				Address a=addresses.get(i);
				if (a.equals(addr)) {
					addresses.remove(a);
				}
			}
		}		
	}
	
	/** Gets interface name.
	 * @return the interface name */
	public String getName() {
		if (name!=null) return name;
		else return getId();
	}

	/** Gets the first interface address.
	 * @return the address */
	public Address getAddress() {
		synchronized (addresses) { 
			if (addresses.size()>0) return addresses.get(0);
			else return null;
		}
	}
	
	/** Gets all interface addresses.
	 * @return the addresses */
	public Address[] getAddresses() {
		synchronized (addresses) { 
			return addresses.toArray(new Address[0]);
		}
	}
	
	/** Whether a given address belongs to this interface.
	 * @param addr the address
	 * @return <i>true</i> if the address belongs to this interface */
	public boolean hasAddress(Address addr) {
		synchronized (addresses) { 
			for (Address a : addresses) {
				if (a.equals(addr)) return true;
			}
		}
		return false;
	}

	
	/** Sends a packet.
	 * @param pkt the packet to be sent
	 * @param dest_addr the address of the destination interface */
	public abstract void send(Packet pkt, Address dest_addr);	

		
	/** Closes the interface. */
	public void close() {
		listeners.clear();
	}
	
	
	/** Gets an identification string for this interface.
	 * @return the first address associated to this interface */
	protected String getId() {
		if (name!=null) return name;
			else if (addresses.size()>0) return addresses.get(0).toString();
				else return id;
	}

	
	@Override
	public String toString() {
		return getClass().getSimpleName()+'['+getId()+']';
	}

}
