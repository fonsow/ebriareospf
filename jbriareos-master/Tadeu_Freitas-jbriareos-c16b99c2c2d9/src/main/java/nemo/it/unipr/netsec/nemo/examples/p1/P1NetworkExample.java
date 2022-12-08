package nemo.it.unipr.netsec.nemo.examples.p1;


import nemo.it.unipr.netsec.ipstack.link.Link;
import nemo.it.unipr.netsec.ipstack.link.LinkInterface;
import nemo.it.unipr.netsec.ipstack.net.Node;
import nemo.it.unipr.netsec.nemo.link.DataLink;
import nemo.it.unipr.netsec.nemo.link.DataLinkInterface;
import nemo.it.unipr.netsec.simulator.scheduler.VirtualClock;
import nemo.org.zoolu.util.Clock;
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.LoggerWriter;
import nemo.org.zoolu.util.SystemUtils;


public class P1NetworkExample {

	public static void main(String[] args) {
		long bit_rate=1000000; // 1Mb/s
		int n=100;// nxm network
		int m=n;
		boolean virtual_time=false; // whether using virtual time
		boolean verbose=false;
		
		// use virtual clock
		if (virtual_time) Clock.setDefaultClock(new VirtualClock());
		
		// verbose mode
		if (verbose) {
			SystemUtils.setDefaultLogger(new LoggerWriter(System.out,LoggerLevel.DEBUG));
			Node.DEBUG=true;
			LinkInterface.DEBUG=true;
			DataLinkInterface.DEBUG=true;
			Link.DEBUG=true;
		}

		// create nx(m+1) horizontal links
		DataLink[][] h_links=new DataLink[n][m+1];
		for (int i=0; i<n; i++) for (int j=0; j<m+1; j++) h_links[i][j]=new DataLink(bit_rate);
		// create (n+1)xm vertical links		
		DataLink[][] v_links=new DataLink[n+1][m];
		for (int i=0; i<n+1; i++) for (int j=0; j<m; j++) v_links[i][j]=new DataLink(bit_rate);
				
		// create nxm nodes
		Node[][] nodes=new Node[n][m];	
		for (int i=0; i<n; i++) for (int j=0; j<m; j++) {
			P1Address addr=new P1Address(i,j);
			nodes[i][j]=new P1Node(addr,v_links[i][j],h_links[i][j+1],v_links[i+1][j],h_links[i][j]);
		}
		
		P1Address src=new P1Address(0,0);
		P1Address dst=new P1Address(n-1,m-1);
		P1Packet pkt=new P1Packet(src,dst,"hello");
		System.out.println("Node "+src+" is sending packet: "+pkt);
		nodes[src.getI()][src.getJ()].sendPacket(new P1Packet(src,dst,"hello"));
	}

}
