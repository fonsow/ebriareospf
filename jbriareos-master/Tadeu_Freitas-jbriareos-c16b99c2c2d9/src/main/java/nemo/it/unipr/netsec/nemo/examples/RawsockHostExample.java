package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.ethernet.EthLayer;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Address;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4AddressPrefix;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4EthInterface;
import nemo.it.unipr.netsec.nemo.ip.Ip4Host;
import nemo.it.unipr.netsec.rawsocket.ethernet.RawEthInterface;

import java.net.SocketException;


public class RawsockHostExample {

	public static void main(String[] args) throws SocketException {
		String nic_name=args[0]; // e.g. "eth0"
		Ip4AddressPrefix ip_addr_prefix=new Ip4AddressPrefix(args[1]); // e.g. "192.168.56.33/24"
		Ip4Address default_router=args.length>2? new Ip4Address(args[2]) : null; // e.g. "192.168.56.1" 

		RawEthInterface eth_interface=new RawEthInterface(nic_name);
		Ip4EthInterface ni=new Ip4EthInterface(new EthLayer(eth_interface),ip_addr_prefix);
		new Ip4Host(ni,default_router);
	}

}
