package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.analyzer.ProtocolAnalyzer;
import nemo.it.unipr.netsec.ipstack.analyzer.Sniffer;
import nemo.it.unipr.netsec.ipstack.analyzer.SnifferListener;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Address;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4AddressPrefix;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Prefix;
import nemo.it.unipr.netsec.ipstack.ip4.SocketAddress;
import nemo.it.unipr.netsec.ipstack.link.EthTunnelHub;
import nemo.it.unipr.netsec.ipstack.link.EthTunnelInterface;
import nemo.it.unipr.netsec.ipstack.net.NetInterface;
import nemo.it.unipr.netsec.ipstack.net.Packet;
import nemo.it.unipr.netsec.ipstack.routing.Route;
import nemo.it.unipr.netsec.nemo.ip.Ip4Host;
import nemo.it.unipr.netsec.nemo.ip.Ip4Router;
import nemo.it.unipr.netsec.nemo.ip.IpLink;
import nemo.it.unipr.netsec.nemo.ip.IpLinkInterface;
import nemo.it.unipr.netsec.nemo.link.PromiscuousLinkInterface;
import nemo.org.zoolu.util.SystemUtils;

import java.net.SocketException;


public class DistributedNetworkExample {

	public static void main(String[] args) throws SocketException {
		int N=16;
		if (args.length==0) {
			// run 1 hub + 16 networks on localhost
			new EthTunnelHub();
			for (int i=1; i<N; i++) {
				main(new String[]{String.valueOf(i),"127.0.0.1:"+EthTunnelHub.DEFAULT_PORT});
			}
			main(new String[]{String.valueOf(0),"127.0.0.1:"+EthTunnelHub.DEFAULT_PORT,"10.1."+(N-1)+".2"});
			return;
		}
		else {
			// run only one network
			int net_id= Integer.parseInt(args[0]); // e.g. 0, 1, 2,...
			SocketAddress tunnel_hub_addr=new SocketAddress(args[1]); // e.g. 192.168.56.40
			Ip4Address remote_addr=args.length>2? new Ip4Address(args[2]) : null; // of type 10.1.X.2, 10.1.X.2, or 172.31.0.X
			
			Ip4Prefix eth_prefix=new Ip4Prefix("10.1."+net_id+".0/24"); 
			Ip4AddressPrefix tunnnel_addr=new Ip4AddressPrefix("172.31.0."+net_id+"/24"); 
			
			IpLink link1=new IpLink(eth_prefix);
			IpLinkInterface r_eth=new IpLinkInterface(link1);
			EthTunnelInterface r_tun=new EthTunnelInterface(tunnnel_addr,tunnel_hub_addr);
			
			Ip4Router r1=new Ip4Router(new NetInterface[]{r_eth,r_tun});
			for (int i=0;i<N; i++) {
				link1.addRouter((Ip4Address)r_eth.getAddress());
				if (i!=net_id) r1.getRoutingTable().add(new Route(new Ip4Prefix("10.1."+i+".0/24"),new Ip4Address("172.31.0."+i),r_tun));
			}

			Ip4Host host1=new Ip4Host(link1);		
			
			if (remote_addr!=null) {
				// ping a remote host
				System.out.println("From "+host1.getAddress()+":");
				host1.ping(remote_addr,4, System.out);
				SystemUtils.exitAfter(8000);
			}
			else {
				// sniff packets on link1
				new Sniffer(new PromiscuousLinkInterface(link1), new SnifferListener(){
					@Override
					public void onPacket(Sniffer sniffer, NetInterface ni, Packet pkt) {
						System.out.println("Captured packet: "+ProtocolAnalyzer.exploreInner(pkt));
					}			
				});
			}
			
		}
	}

}
