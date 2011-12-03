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

import java.io.Serializable;
import java.util.Date;

import org.springframework.util.Assert;

/**
 * @author Gary Russell
 *
 */
public class ClusterStatus implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String STATUS_RUNNING = "RUNNING";
	public static final String STATUS_PAUSED = "PAUSED";

	private String applicationId;
	private String status;
	private String currentMaster;
	private Date lastProcessed;
	private String pendingUsurper;
	private Date usurpTimestamp;
	
	public ClusterStatus(String applicationId, String currentMaster) {
		this.applicationId = applicationId;
		this.currentMaster = currentMaster;
		this.lastProcessed = new Date();
	}
	
	public ClusterStatus(String applicationId, String status, String currentMaster,
			Date lastProcessed, String pendingUsurper,
			Date usurpTimestamp) {
		this.applicationId = applicationId;
		this.status = status;
		this.currentMaster = currentMaster;
		this.lastProcessed = lastProcessed;
		this.pendingUsurper = pendingUsurper;
		this.usurpTimestamp = usurpTimestamp;
	}
	/**
	 * @return the applicationId
	 */
	public String getApplicationId() {
		return applicationId;
	}
	/**
	 * @param applicationId the applicationId to set
	 */
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		Assert.isTrue(STATUS_PAUSED.equals(status) || STATUS_RUNNING.equals(status),
				"status must be " + STATUS_RUNNING + " or " + STATUS_PAUSED);
		this.status = status;
	}

	/**
	 * @return the currentMaster
	 */
	public String getCurrentMaster() {
		return currentMaster;
	}
	/**
	 * @param currentMaster the currentMaster to set
	 */
	public void setCurrentMaster(String currentMaster) {
		this.currentMaster = currentMaster;
	}
	/**
	 * @return the lastProcessed
	 */
	public Date getLastProcessed() {
		return lastProcessed;
	}
	/**
	 * @param lastProcessed the lastProcessed to set
	 */
	public void setLastProcessed(Date lastProcessed) {
		this.lastProcessed = lastProcessed;
	}
	/**
	 * @return the pendingUsurper
	 */
	public String getPendingUsurper() {
		return pendingUsurper;
	}
	/**
	 * @param pendingUsurper the pendingUsurper to set
	 */
	public void setPendingUsurper(String pendingUsurper) {
		this.pendingUsurper = pendingUsurper;
	}
	/**
	 * @return the usurpTimestamp
	 */
	public Date getUsurpTimestamp() {
		return usurpTimestamp;
	}
	/**
	 * @param usurpTimestamp the usurpTimestamp to set
	 */
	public void setUsurpTimestamp(Date usurpTimestamp) {
		this.usurpTimestamp = usurpTimestamp;
	}

	@Override
	public int hashCode() {
		return this.applicationId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterStatus other = (ClusterStatus) obj;
		if (applicationId == null) {
			if (other.applicationId != null)
				return false;
		} else if (!applicationId.equals(other.applicationId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ClusterStatus [applicationId=" + applicationId + ", status="
				+ status + ", currentMaster=" + currentMaster
				+ ", lastProcessed=" + lastProcessed + ", pendingUsurper="
				+ pendingUsurper + ", usurpTimestamp=" + usurpTimestamp + "]";
	}

	

}
