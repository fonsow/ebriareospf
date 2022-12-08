package nemo.it.unipr.netsec.nemo.examples;


import nemo.it.unipr.netsec.ipstack.analyzer.ProtocolAnalyzer;
import nemo.it.unipr.netsec.ipstack.ethernet.EthPacket;
import nemo.it.unipr.netsec.rawsocket.RawLinkSocket;

import java.net.SocketException;


public class TcpdumpExample {

	public static void main(String[] args) throws SocketException {
		RawLinkSocket raw_socket=new RawLinkSocket();
				
		byte[] buf=new byte[65536];
		while (true) {
			int len=raw_socket.recv(buf,0,0);
			System.out.println(ProtocolAnalyzer.packetDump(EthPacket.parseEthPacket(buf,0,len)));
		}
	}

}
