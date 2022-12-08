package nemo.it.unipr.netsec.tuntap;


import nemo.it.unipr.netsec.ipstack.ethernet.EthAddress;
import nemo.it.unipr.netsec.ipstack.ethernet.EthLayer;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4AddressPrefix;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4EthInterface;

import java.io.IOException;


/** TAP interface for sending or receiving Ethernet packets.
 */
public class Ip4TapInterface extends Ip4EthInterface {
	
	static EthAddress ETH_ADDR=new EthAddress("11:22:33:44:55:66");

	
	/** Creates a new interface.
	 * @param name name of the interface (e.g. "tap0"); if <i>null</i>, a new interface is added
	 * @param ip_addr_prefix the IP address and prefix length 
	 * @throws IOException */
	public Ip4TapInterface(String name, Ip4AddressPrefix ip_addr_prefix) throws IOException {
		super(new EthLayer(new TapInterface(name,ETH_ADDR)),ip_addr_prefix);
	}

}
