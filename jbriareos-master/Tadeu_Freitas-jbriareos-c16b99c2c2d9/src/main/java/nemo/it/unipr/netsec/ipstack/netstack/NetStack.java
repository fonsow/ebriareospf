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

package nemo.it.unipr.netsec.ipstack.netstack;


import nemo.it.unipr.netsec.ipstack.ip4.Ip4EthInterface;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Layer;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Node;
import nemo.it.unipr.netsec.ipstack.ip6.Ip6Address;
import nemo.it.unipr.netsec.ipstack.ip6.Ip6EthInterface;
import nemo.it.unipr.netsec.ipstack.ip6.Ip6Layer;
import nemo.it.unipr.netsec.ipstack.ip6.Ip6Node;
import nemo.it.unipr.netsec.ipstack.link.EthTunnelInterface;
import nemo.it.unipr.netsec.ipstack.net.LoopbackInterface;
import nemo.it.unipr.netsec.ipstack.net.NetInterface;
import nemo.it.unipr.netsec.ipstack.tcp.TcpConnection;
import nemo.it.unipr.netsec.ipstack.tcp.TcpLayer;
import nemo.it.unipr.netsec.ipstack.udp.UdpLayer;
import nemo.it.unipr.netsec.tuntap.Ip4TunInterface;
import nemo.org.zoolu.util.LoggerLevel;
import nemo.org.zoolu.util.LoggerWriter;
import nemo.org.zoolu.util.SystemUtils;


/** TCP/IP default stack.
 * It is initialized at boot by reading the IP configuration file specified by the 'ipcfg' property.
 * If the 'ipcfg' property is not set, an IP layer with only lo interface is used.
 */
public class NetStack {

	/** The IP configuration file property */
	//private static final String CONFIG_FILE_PARAM="ipstack.config"; 
	private static final String CONFIG_FILE_PARAM="ipcfg";

	/** The ipstack verbose property */
	private static final String VERBOSE_PARAM="ipstack.verbose";

	/** The IPv4 layer */
	public static Ip4Layer IP4_LAYER=null; 

	/** The IPv6 layer */
	public static Ip6Layer IP6_LAYER=null; 

	/** The UDP layer */
	public static UdpLayer UDP_LAYER=null;

	/** The TCP layer */
	public static TcpLayer TCP_LAYER=null;

	
	/** Initializes the static attributes */
	static {
		try {
			String verbose= System.getProperty(VERBOSE_PARAM);
			if (verbose!=null) {
				System.out.println("ipstack: verbose level: "+verbose);
				int level= Integer.parseInt(verbose);
				if (level>=1) {
					SystemUtils.setDefaultLogger(new LoggerWriter(System.out,LoggerLevel.DEBUG));
					UdpLayer.DEBUG=true;
					TcpConnection.DEBUG=true;
				}
				if (level>=2) {
					TcpLayer.DEBUG=true;
					Ip4Layer.DEBUG=true;
					Ip6Layer.DEBUG=true;
				}
				if (level>=3) {
					Ip4Node.DEBUG=true;
					Ip6Node.DEBUG=true;
					Ip4EthInterface.DEBUG=true;
					Ip6EthInterface.DEBUG=true;
					Ip4TunInterface.DEBUG=true;
					EthTunnelInterface.DEBUG=true;
				}
				if (level>=4) {
					nemo.it.unipr.netsec.rawsocket.Socket.setDebug(true);
					nemo.it.unipr.netsec.rawsocket.udp.DatagramSocket.DEBUG=true;
				}
			}
			String config_file= System.getProperty(CONFIG_FILE_PARAM);
			IP4_LAYER=config_file!=null? new LinuxIp4Layer(new LinuxIp4Configuration(config_file)) : new LinuxIp4Layer(new LinuxIp4Configuration());			
			IP6_LAYER=new Ip6Layer(new NetInterface[]{new LoopbackInterface(new Ip6Address("::1"))});
			UDP_LAYER=new UdpLayer(IP4_LAYER);			
			TCP_LAYER=new TcpLayer(IP4_LAYER);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*private static RoutingTable parseIp4RoutingTable(String str) {
		return new RoutingTable();
	}*/

}
