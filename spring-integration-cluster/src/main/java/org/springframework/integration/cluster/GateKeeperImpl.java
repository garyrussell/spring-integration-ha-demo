/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link GateKeeper}; uses a {@link ClusterControl}
 * to determine mastership, or otherwise.
 * 
 * @author Gary Russell
 *
 */
public class GateKeeperImpl implements GateKeeper<Object> {

	private ClusterControl clusterControl;
	
	private Log logger = LogFactory.getLog(this.getClass());
	
	public GateKeeperImpl(ClusterControl clusterControl) {
		this.clusterControl = clusterControl;
	}
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public Object dispatch(Object in) {
		if (!this.clusterControl.verifyStatus(false)) {
			throw new RuntimeException("Message Processing Disabled");
		}
		return in;
	}
	
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void heartbeat(Heartbeat heartbeat) {
		boolean result = this.clusterControl.verifyStatus(true);
		if (logger.isDebugEnabled()) {
			logger.debug("Received heartbeat " + heartbeat.getTimestamp() + " verify result:" + result);
		}
	}

}
