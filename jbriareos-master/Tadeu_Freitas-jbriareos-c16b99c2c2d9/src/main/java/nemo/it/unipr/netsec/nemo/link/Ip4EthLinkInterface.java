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


import nemo.it.unipr.netsec.ipstack.ip4.Ip4AddressPrefix;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4EthInterface;


/** IPv4 interface attached to a {@link DataLink link}.
 */
public class Ip4EthLinkInterface extends Ip4EthInterface {
	
	/** Creates a new interface.
	 * @param link the link to be attached to
	 * @param ipaddr_prefix the IP address and prefix length */
	public Ip4EthLinkInterface(DataLink link, Ip4AddressPrefix ipaddr_prefix) {
		super(new EthLinkInterface(link),ipaddr_prefix);
	}
	
}
