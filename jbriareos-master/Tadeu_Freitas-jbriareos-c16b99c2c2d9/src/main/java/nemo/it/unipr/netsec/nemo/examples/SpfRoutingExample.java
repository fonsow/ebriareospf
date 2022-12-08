package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.ip4.Ip4Prefix;
import nemo.it.unipr.netsec.nemo.ip.Ip4Host;
import nemo.it.unipr.netsec.nemo.ip.Ip4Router;
import nemo.it.unipr.netsec.nemo.ip.IpLink;
import nemo.it.unipr.netsec.nemo.routing.ShortestPathAlgorithm;
import nemo.it.unipr.netsec.nemo.routing.ospf.OspfRouting;
import nemo.it.unipr.netsec.simulator.scheduler.VirtualClock;
import nemo.org.zoolu.util.Clock;


public class SpfRoutingExample {

	public static void main(String[] args) {
		long bit_rate=1000000; // 1Mb/s
		int n=8,m=8; // nxm network
		int c=10; // number of ping messages
		boolean virtual_time=true; // whether using virtual time
		long max_virtual_time=120000; // stops when reaching this time value, in millisecs
		
		// use virtual clock
		if (virtual_time) Clock.setDefaultClock(new VirtualClock(max_virtual_time));

		// create nx(m+1) horizontal links
		IpLink[][] h_links=new IpLink[n][m+1];
		for (int i=0; i<n; i++) for (int j=0; j<m+1; j++) h_links[i][j]=new IpLink(bit_rate,new Ip4Prefix("10."+i+"."+j+".0/24"));
		// create (n+1)xm vertical links		
		IpLink[][] v_links=new IpLink[n+1][m];
		for (int i=0; i<n+1; i++) for (int j=0; j<m; j++) v_links[i][j]=new IpLink(bit_rate,new Ip4Prefix("20."+i+"."+j+".0/24"));
		
		// create all nxm routers
		Ip4Router[][] routers=new Ip4Router[n][m];	
		for (int i=0; i<n; i++) for (int j=0; j<m; j++) {
			routers[i][j]=new Ip4Router(new IpLink[]{h_links[i][j],h_links[i][j+1],v_links[i][j],v_links[i+1][j]});
			routers[i][j].setDynamicRouting(new OspfRouting(ShortestPathAlgorithm.DIJKSTRA));
		}
		
		// create H1 and H2 connected to the first and last horizontal link respectively
		Ip4Host host1=new Ip4Host(h_links[0][0]);		
		Ip4Host host2=new Ip4Host(h_links[n-1][m]);
		
		// wait for a while, then ping H2 from H1
		Clock.getDefaultClock().sleep(5000);
		System.out.println("Host "+host1.getAddress()+":");
		host1.ping(host2.getAddress(),c, System.out);
	}

}
