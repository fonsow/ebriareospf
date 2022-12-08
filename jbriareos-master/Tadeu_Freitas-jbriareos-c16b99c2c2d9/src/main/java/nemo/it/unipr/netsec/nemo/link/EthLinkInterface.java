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

package nemo.it.unipr.netsec.nemo.link;


import nemo.it.unipr.netsec.ipstack.ethernet.EthAddress;
import nemo.org.zoolu.util.ByteUtils;
import nemo.org.zoolu.util.Random;


/** Ethernet interface attached to a {@link DataLink link}.
 */
public class EthLinkInterface extends DataLinkInterface {

	/** OUI */
	public static byte[] OUI=new byte[] {0x02, 0x02, 0x02};

	
	/** Creates a new interface.
	 * @param link the link to be attached to */
	public EthLinkInterface(DataLink link) {
		this(link,new EthAddress(ByteUtils.concat(OUI,Random.nextBytes(6-OUI.length))));
	}
	
	/** Creates a new interface.
	 * @param link the link to be attached to
	 * @param addr the interface address */
	public EthLinkInterface(DataLink link, EthAddress addr) {
		super(link,addr);
	}
	
}
