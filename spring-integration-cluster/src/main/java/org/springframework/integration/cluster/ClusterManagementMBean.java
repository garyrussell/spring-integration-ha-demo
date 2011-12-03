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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MetricType;

/**
 * @author Gary Russell
 *
 */
@ManagedResource(objectName="spring.application:type=ClusterControl,name=clusterControl")
public class ClusterManagementMBean implements ClusterMetrics {

	private ClusterControl clusterControl;

	public ClusterManagementMBean(ClusterControl clusterControl) {
		this.clusterControl = clusterControl;
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Number of messages processed by this instance")
	public int getMessageCount() {
		return this.clusterControl.getMessageCount();
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Time since last status check (seconds)")
	public float getTimeSinceLastMonitor() {
		return this.clusterControl.getTimeSinceLastMonitor();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Time since last message (seconds)")
	public float getTimeSinceLastMessage() {
		return this.clusterControl.getTimeSinceLastMessage();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Time since last hearbeat (seconds)")
	public float getTimeSinceHearbeat() {
		return this.clusterControl.getTimeSinceHearbeat();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "True if this instance is the master")
	public boolean isMaster() {
		return this.clusterControl.isMaster();
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "True if this is a single-source application")
	public boolean isSingleSource() {
		return this.clusterControl.isSingleSource();
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "True if this application/instance is paused")
	public boolean isPaused() {
		return this.clusterControl.isPaused();
	}
	
	@ManagedOperation(description="Stop the soInbound adapter")
	public void stopInbound() {
		this.clusterControl.stopInbound();
	}
	
	@ManagedOperation(description="Start the soInbound adapter")
	public void startInbound() {
		this.clusterControl.startInbound();
	}
	
	@ManagedOperation(description="Send Heartbeat Message") 
	public void sendHeartbeat() {
		this.clusterControl.sendHeartbeat();
	}

	@ManagedOperation(description="Pause Application/Instance") 
	public String pause() {
		return this.clusterControl.pause();
	}
	
	@ManagedOperation(description="Resume Application/Instance") 
	public String resume() {
		return this.clusterControl.resume();
	}
	
	@ManagedOperation(description="Pause All Application Instances") 
	public String pauseAll() {
		return this.clusterControl.pauseAll();
	}
	
	@ManagedOperation(description="Resume All Application Instances") 
	public String resumeAll() {
		return this.clusterControl.resumeAll();
	}
	
	@ManagedOperation(description="Resume Application/Instance") 
	public String obtainApplicationStatus() {
		return this.clusterControl.obtainApplicationStatus();
	}

}
