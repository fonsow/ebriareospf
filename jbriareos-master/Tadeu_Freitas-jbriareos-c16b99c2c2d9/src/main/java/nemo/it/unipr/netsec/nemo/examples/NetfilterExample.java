package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.analyzer.ProtocolAnalyzer;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Packet;
import nemo.it.unipr.netsec.netfilter.NetfilterQueue;
import nemo.it.unipr.netsec.netfilter.PacketHandler;
import nemo.org.zoolu.util.SystemUtils;


/** Example program that creates a linux netfilter queue {@link nemo.it.unipr.netsec.netfilter.NetfilterQueue}
 * and prints all captured packets.
 * <p>
 * In order to pass all incoming packets to this program you could use the following Linux command:
 * <pre>
 * sudo iptables -A INPUT -j NFQUEUE --queue-num 0
 * </pre>
 */
public class NetfilterExample {

	
   /** The main method. */
	public static void main(String[] args) {
		
		final NetfilterQueue qh=new NetfilterQueue(0,new PacketHandler() {
		@Override
			public int processPacket(byte[] buf, int len) {
				Ip4Packet ip_pkt=Ip4Packet.parseIp4Packet(buf);
				System.out.println("Captured packet:  "+ProtocolAnalyzer.exploreInner(ip_pkt));
				// with "return 0;" the packet is discarded
				return len;
			}	
		});

		new Thread() {
			@Override
			public void run() {
				qh.start();
			}
		}.start();
		
		System.out.println("Press 'Return' to stop.");
		SystemUtils.readLine();
		qh.stop();
		System.exit(0);
	}	

}
