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


/**
 * Interface for a cluster controller - a mechanism whereby
 * a single instance of a service (the 'master') is used to process messages.
 * Non master nodes monitor the state of the master and 
 * exactly one can take over if necessary.
 * 
 * @author Gary Russell
 *
 */
public interface ClusterControl extends ClusterMetrics {

	/** 
	 * Used by the master to determine if he is still the master.
	 * Updates the cluster status last processed timestamp. Cancels
	 * any usurp actions.
	 * @param heartBeat true if this message is a heartbeat
	 */
	public boolean verifyStatus(boolean heartbeat);
	
	/**
	 * Used to send a heartbeat message when no traffic detected.
	 * Should be configured to send a special message to the main
	 * cluster thread so the lastProcessed timestamp will be 
	 * updated. 
	 */
	public void sendHeartbeat();
	
	/**
	 * Called when the monitor detects the main thread is not 
	 * processing; stop the soInbound adapter so another instance
	 * can take over.
	 */
	public void stopInbound();
	
	/**
	 * Called when a usurper successfully takes over the master
	 * role; start the soInbound adapter.
	 */
	public void startInbound();

	/**
	 * Pause the application/instance - if this is a single-source
	 * application, all instances will be paused; otherwise just
	 * this instance will be paused.
	 */
	public String pause();
	
	/**
	 * Resume the application/instance - if this is a single-source
	 * application, normal master election will occur (if the previous
	 * master is available, it will most likely resume that role); 
	 * otherwise, just this instance is resumed.
	 */
	public String resume();
	
	/**
	 * Pause all (non-single-source) application instances. For single-
	 * source applications, use {@link #pause()}.
	 * @return
	 */
	public String pauseAll();
	
	/**
	 * Resume all (non-single-source) application instances. For single-
	 * source applications, use {@link #resume()}.
	 * @return
	 */
	public String resumeAll();
	
	/**
	 * Monitor the state of the master.
	 */
	public void doMonitor();

	/**
	 * @return the application id.
	 */
	public String getApplicationId();

	/**
	 * @return a string representing the current status of the application
	 */
	public String obtainApplicationStatus();
    /**
     * 
     */
	public int getMessageCount();
}
