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

package nemo.it.unipr.netsec.ipstack.icmp6.message;


import nemo.it.unipr.netsec.ipstack.icmp6.Icmp6Message;
import nemo.it.unipr.netsec.ipstack.ip6.Ip6Address;
import nemo.org.zoolu.util.ByteUtils;


/** ICMP Echo request or Echo reply message.
 */
abstract class Icmp6EchoMessage extends Icmp6Message {

	/** ICMP code: 0 */
	public static final int CODE=0;
	
	
	/** Identifier to aid in matching echos and replies (may be zero) */
	//int id;

	/** Sequence number to aid in matching echos and replies (may be zero) */
	//int sqn;

	/** Included data. Data received in the echo message must be returned in the echo reply message */
	//byte[] data;

	
	/** Creates a new ICMP message.
	 * @param src_addr IP source address
	 * @param dst_addr IP destination address
	 * @param type ICMP message type
	 * @param id identifier to aid in matching echos and replies (may be zero)
	 * @param sqn sequence number to aid in matching echos and replies (may be zero)
	 * @param echo_data the echo data to be included in the ICMP message */
	protected Icmp6EchoMessage(Ip6Address src_addr, Ip6Address dst_addr, int type, int id, int sqn, byte[] echo_data) {
		super(src_addr,dst_addr,type,CODE);
		if (type!=Icmp6Message.TYPE_Echo_Request && type!=Icmp6Message.TYPE_Echo_Reply) throw new RuntimeException("ICMP type missmatch ("+type+"): this is neither \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Request+") nor \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Reply+") ICMP message");
		// else
		icmp_body=new byte[4+echo_data.length];
		ByteUtils.intToTwoBytes(id,icmp_body,0);
		ByteUtils.intToTwoBytes(sqn,icmp_body,2);
		System.arraycopy(echo_data,0,icmp_body,4,echo_data.length);
	}
	

	/** Creates a new ICMP message.
	 * @param src_addr IP source address
	 * @param dst_addr IP destination address
	 * @param buf the buffer containing the ICMP message
	 * @param off the offset within the buffer
	 * @param len the length of the ICMP message */
	protected Icmp6EchoMessage(Ip6Address src_addr, Ip6Address dst_addr, byte[] buf, int off, int len) {
		super(src_addr,dst_addr,buf[off]&0xff,buf[off+1]&0xff,null,0,0);
		if (type!=Icmp6Message.TYPE_Echo_Request && type!=Icmp6Message.TYPE_Echo_Reply) throw new RuntimeException("ICMP type missmatch ("+type+"): this is neither \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Request+") nor \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Reply+") ICMP message");
		if (code!=CODE) throw new RuntimeException("ICMP code missmatch ("+code+"): the code should be "+CODE);
	}	

	
	/** Creates a new ICMP message.
	 * @param msg the ICMP message */
	protected Icmp6EchoMessage(Icmp6Message msg) {
		super(msg);
		if (type!=Icmp6Message.TYPE_Echo_Request && type!=Icmp6Message.TYPE_Echo_Reply) throw new RuntimeException("ICMP type missmatch ("+type+"): this is neither \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Request+") nor \"Echo Request\" ("+Icmp6Message.TYPE_Echo_Reply+") ICMP message");
		if (code!=CODE) throw new RuntimeException("ICMP code missmatch ("+code+"): the code should be "+CODE);
	}	

		
	/** Gets the identifier.
	 * @return the identifier */
	public int getIdentifier() {
		return ByteUtils.twoBytesToInt(icmp_body,0);
	}


	/** Gets the sequence number.
	 * @return the sequence number */
	public int getSequenceNumber() {
		return ByteUtils.twoBytesToInt(icmp_body,2);
	}


	/** Gets the echo data included in the message.
	 * @return the data */
	public byte[] getEchoData() {
		byte[] echo_data=new byte[icmp_body.length-4];
		System.arraycopy(icmp_body,4,echo_data,0,echo_data.length);
		return echo_data;
	}

}