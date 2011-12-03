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
 * @author Gary Russell
 *
 */
public interface ClusterMetrics {

	/**
	 * @return the number of messages processed by this instance
	 */
	public int getMessageCount();
	/**
	 * @return the time since last monitor in seconds
	 */
	public float getTimeSinceLastMonitor();

	/**
	 * @return the time since last message in seconds
	 */
	public float getTimeSinceLastMessage();
	
	/**
	 * @return the time since last heartbeat in seconds
	 */
	public float getTimeSinceHearbeat();
	
	/**
	 * @return true if this instance is the master
	 */
	public boolean isMaster();
	
	/**
	 * @return true if this a single-source application
	 */
	public boolean isSingleSource();

	/**
	 * @return true if this instance is paused.
	 */
	public boolean isPaused();
}
