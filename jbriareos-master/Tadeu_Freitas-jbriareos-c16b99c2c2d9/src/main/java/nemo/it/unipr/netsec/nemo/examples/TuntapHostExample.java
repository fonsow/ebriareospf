package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.ip4.Ip4Address;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4AddressPrefix;
import nemo.it.unipr.netsec.ipstack.net.NetInterface;
import nemo.it.unipr.netsec.nemo.ip.Ip4Host;
import nemo.it.unipr.netsec.tuntap.Ip4TuntapInterface;

import java.io.IOException;


/** Simple host attached to a TUN interface.
 *  It can optionally run a HTTP server.
 */
public class TuntapHostExample {

	public static void main(String[] args) throws IOException {
		String tuntap_interface=args[0]; // e.g. tun0
		Ip4AddressPrefix ipaddr_prefix=new Ip4AddressPrefix(args[1]); // e.g. "172.0.18.2/24"
		Ip4Address default_router=new Ip4Address(args[2]); // e.g. "172.0.18.1"
		
		NetInterface ni=new Ip4TuntapInterface(tuntap_interface,ipaddr_prefix);
		Ip4Host host=new Ip4Host(ni,default_router);
		host.startHttpServer();
	}

}
