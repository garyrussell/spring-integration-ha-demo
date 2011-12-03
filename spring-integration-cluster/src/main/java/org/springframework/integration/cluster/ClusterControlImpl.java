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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import com.springsource.insight.annotation.InsightEndPoint;

/**
 * Implementation of {@link ClusterControl} using a row in a database
 * table as a global lock across nodes. Ensures that one and only one
 * node is active; allows exactly one non-master node to take over
 * if necessary.
 * 
 * @author Gary Russell
 *
 */
public class ClusterControlImpl implements ClusterControl, SmartLifecycle {

	private ControlBusGateway controlBusGateway;
	private HeartbeatGateway<Heartbeat> heartbeatGateway;
	private ClusterStatusRepository clusterStatusRepository;
	private final String applicationId;
	private final String application;
	private final boolean singleSource;
	private final String adapterName;
	private final String member;
	private volatile long monitorInterval;
	private volatile long keepaliveInterval;
	private volatile boolean ranOnce;
	
	private volatile Date lastMonitor = new Date(0);
	private volatile Date lastMessage = new Date(0);
	private volatile long lastUpdate;
	private volatile Date lastHeartbeat = new Date(0);
	private volatile boolean master;
	
	private volatile boolean running;
	private volatile boolean pausing;
	private volatile boolean paused;
	
	private volatile int messageCount;
	
	private Log logger = LogFactory.getLog(getClass());


	public ClusterControlImpl(String applicationId,
							  boolean singleSource,
							  String member,
							  String adapterName,
							  long monitorInterval,
							  long keepaliveInterval,
							  ControlBusGateway controlBusGateway,
							  HeartbeatGateway<Heartbeat> heartbeatGateway,
							  ClusterStatusRepository clusterStatusRepository) {
		this.singleSource = singleSource;
		this.member = member;
		this.application = applicationId;
		if (singleSource) {
			this.applicationId = applicationId; 
		} else {
			this.applicationId = applicationId + ":" + member;
		}
		this.adapterName = adapterName;
		this.monitorInterval = monitorInterval;
		this.keepaliveInterval = keepaliveInterval;
		this.controlBusGateway = controlBusGateway;
		this.heartbeatGateway = heartbeatGateway;
		this.clusterStatusRepository = clusterStatusRepository;
	}

	@Transactional
	public boolean verifyStatus(boolean heartbeat) {
		Long now = System.currentTimeMillis();
		ClusterStatus clusterStatus = null;
		if (!paused && !heartbeat && now - this.lastUpdate < this.keepaliveInterval) {
			this.messageCount++;
			return true;
		}
		try {
			clusterStatus = this.lockClusterStatus();
			if (checkPaused(clusterStatus)) {
				return false;
			}
			if (this.singleSource) {
				boolean result = updateLastProcessedOrStop(clusterStatus);
				this.lastUpdate = now;
				if (!heartbeat && result) {
					this.messageCount++;
				}
				return result;
			} else {
				this.updateLastProcessed(clusterStatus);
				this.messageCount++;
				return true;
			}
		} catch (Exception e) {
			if (this.singleSource) {
				logger.error("Cluster Control Failure; emergency stopping adapter", e);
				this.stopInbound();
				this.master = false;
				return false;
			} else {
				logger.error("Cluster Control Exception", e);
				return true;
			}
		} finally {
			this.lastMessage = new Date(now);
			if (heartbeat) {
				this.lastHeartbeat = this.lastMessage;
			}
			if (clusterStatus != null) {
				this.clusterStatusRepository.unlock(applicationId);
			}
		}
	}

	/**
	 * @param clusterStatus
	 */
	private boolean checkPaused(ClusterStatus clusterStatus) {
		if (ClusterStatus.STATUS_PAUSED.equals(clusterStatus.getStatus())) {
			if (this.singleSource) {
				logger.warn("Application is PAUSED " + clusterStatus.toString());
			} else {
				logger.warn("Instance is PAUSED " + clusterStatus.toString());
			}
			this.stopInbound();
			this.paused = true;
			this.pausing = false;
			return true;
		} 
		return false;
	}

	private boolean updateLastProcessedOrStop(ClusterStatus clusterStatus) {
		if (this.member.equals(clusterStatus.getCurrentMaster())) {
			if (logger.isDebugEnabled()) {
				if (clusterStatus.getPendingUsurper() != null && !("".equals(clusterStatus.getPendingUsurper()))) {
					logger.debug("Removing pending usurper " + clusterStatus.getPendingUsurper());
				}
			}
			updateLastProcessed(clusterStatus);
			return true;
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Current master is " + clusterStatus.getCurrentMaster());
			}
			this.stopInbound();
			return false;
		}
	}

	/**
	 * @param clusterStatus
	 */
	private void updateLastProcessed(ClusterStatus clusterStatus) {
		clusterStatus.setLastProcessed(new Date());
		clusterStatus.setPendingUsurper("");
		this.clusterStatusRepository.updateLastProcessed(clusterStatus);
		logger.trace("Updated Last Processed");
	}
	
	public void sendHeartbeat() {
		Heartbeat heartbeat = new Heartbeat();
		if (logger.isDebugEnabled()) {
			logger.debug("Sending heartbeat " + heartbeat);
		}
		this.heartbeatGateway.sendHeartbeat(heartbeat);
	}

	public void stopInbound() {
		logger.debug("Stopping adapter");		
		this.controlBusGateway.sendCommand("@'" + this.adapterName + "'.stop()");
	}

	public void startInbound() {
		if (this.singleSource && !this.master) {
			throw new RuntimeException("Cannot start adapter - single source application " +
					"and this is not the master - " + 
					this.clusterStatusRepository.find(this.applicationId));
		}
		this.startInboundInternal();
	}

	private void startInboundInternal() {
		logger.debug("Starting adapter");		
		this.controlBusGateway.sendCommand("@'" + this.adapterName + "'.start()");
	}

	@Transactional(timeout=30)
	@InsightEndPoint
	public void doMonitor() {
		if (!this.isRunning()) {
			return;
		}
		ClusterStatus clusterStatus = null;
		try {
			clusterStatus = lockClusterStatus();
			if (checkPaused(clusterStatus)) {
				return;
			}
			if (this.singleSource) {
				monitorSingleSource(clusterStatus);
			} else {
				if (this.paused) {
					this.doResume(clusterStatus);
				}
			}
			this.paused = false;
		} finally {
			if (clusterStatus != null) {
				this.clusterStatusRepository.unlock(applicationId);
			}
		}
	}

	private void doResume(ClusterStatus clusterStatus) {
		if (this.singleSource) {
			logger.info("Application RESUMED " + clusterStatus.toString());
		} else {
			logger.info("Instance RESUMED " + clusterStatus.toString());
		}
		this.startInboundInternal();
	}

	/**
	 * Ensures at most one instance is active.
	 * 
	 * @param clusterStatus
	 */
	private void monitorSingleSource(ClusterStatus clusterStatus) {
		String currentMaster = clusterStatus.getCurrentMaster();
		if (!this.ranOnce) {
			this.ranOnce = true;
			if ("".equals(currentMaster)) { 	
				// nobody is master, preemptive acquisition
				clusterStatus.setCurrentMaster(this.member);
				this.startInboundInternal();
			} else 
			if (this.member.equals(currentMaster)) {
				this.startInboundInternal();
			}
			// if we're the master, start the clock	so
			// we don't immediately think we're delinquent
			this.updateLastProcessedOrStop(clusterStatus);
		}
		Date now = new Date();
		if (this.member.equals(currentMaster)) {
			this.master = true;
			checkMyHealth(clusterStatus, now);
		} else {
			if (this.master) {
				logger.error("Master status has been lost to " + currentMaster);
				this.stopInbound();
			}
			this.master = false;
			checkMasterHealth(clusterStatus, now);
		}
		this.lastMonitor = now;
	}

	private ClusterStatus lockClusterStatus() {
		ClusterStatus clusterStatus = null;
		try {
			clusterStatus = this.clusterStatusRepository.lock(this.applicationId);
		} catch (EmptyResultDataAccessException e) {}
		if (clusterStatus == null) {
			try {
				this.clusterStatusRepository.create(new ClusterStatus(this.applicationId, this.member));
			} catch (DuplicateKeyException e) {
				logger.info("Lost race to become first master");
			}
			clusterStatus = this.clusterStatusRepository.lock(this.applicationId);
		}
		return clusterStatus;
	}

	private void checkMasterHealth(ClusterStatus clusterStatus, Date now) {
		long threshold = calcThreshold(now, this.monitorInterval, 1);
		long lastProcessed = clusterStatus.getLastProcessed().getTime();
		if (lastProcessed >= threshold) {
			if (logger.isDebugEnabled()) {
				logger.debug("Master (" + clusterStatus.getCurrentMaster() + ") processing OK");
			}
			return;
		}
		threshold = calcThreshold(now, this.monitorInterval, 2);
		if (lastProcessed < threshold) {
			if (logger.isDebugEnabled()) {
				logger.debug("Master (" + clusterStatus.getCurrentMaster() + ") not processed for " + 
						((now.getTime() - lastProcessed) / 1000) + " seconds - considering acquisition");
			}
			if (this.member.equals(clusterStatus.getPendingUsurper())) {
				threshold = calcThreshold(now, this.monitorInterval, 1);
				if (clusterStatus.getUsurpTimestamp().getTime() < threshold) {
					logger.warn("Taking over as cluster master " + clusterStatus);
					clusterStatus.setPendingUsurper("");
					clusterStatus.setUsurpTimestamp(now);
					clusterStatus.setCurrentMaster(this.member);
					this.clusterStatusRepository.updateMaster(clusterStatus);
					this.master = true;
					this.startInboundInternal();
					this.sendHeartbeat();
					logger.warn("Taken over as cluster master and sent heartbeat " + clusterStatus);
				}
			} else if (clusterStatus.getPendingUsurper() == null 
					|| clusterStatus.getPendingUsurper().equals("")
					|| clusterStatus.getUsurpTimestamp().getTime() < calcThreshold(now, this.monitorInterval, 3)) {
				logger.debug("Beginning usurp " + clusterStatus);
				clusterStatus.setPendingUsurper(this.member);
				clusterStatus.setUsurpTimestamp(now);
				this.clusterStatusRepository.updateUsurper(clusterStatus);
			} else {
				logger.debug("Another usurper in process of acquisition " + clusterStatus);
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Master (" + clusterStatus.getCurrentMaster() + ") not processed for " + 
						((now.getTime() - lastProcessed) / 1000) + " seconds");
			}
		}
	}

	private void checkMyHealth(ClusterStatus clusterStatus, Date now) {
		long threshold = calcThreshold(now, this.keepaliveInterval, 1);
		long lastProcessed = clusterStatus.getLastProcessed().getTime();
		if (lastProcessed >= threshold) {
			if (logger.isDebugEnabled()) {
				logger.debug("Master (me: " + clusterStatus.getCurrentMaster() + ") processing OK");
			}
			return;
		}
		threshold = calcThreshold(now, this.keepaliveInterval, 3);
		if (lastProcessed >= threshold) {
			if (logger.isDebugEnabled()) {
				logger.debug("Master (me: " + clusterStatus.getCurrentMaster() + ") not processed for " + 
						((now.getTime() - lastProcessed) / 1000) + " seconds - sending heartbeat");
			}
			this.sendHeartbeat();
			return;
		}
		// missed 3 intervals - shut down 
		logger.error("Master (me: " + clusterStatus.getCurrentMaster() + ") not processed for " + 
				((now.getTime() - lastProcessed) / 1000) + " seconds - relinquishing mastership");
		this.stopInbound();
		this.master = false;
		ClusterStatus newClusterStatus = this.clusterStatusRepository.find(applicationId);
		if (this.member.equals(newClusterStatus.getCurrentMaster())) {
			clusterStatus.setCurrentMaster("");
			clusterStatus.setUsurpTimestamp(new Date());
			this.clusterStatusRepository.updateMaster(clusterStatus);
		}
	}

	private long calcThreshold(Date now, long interval, int multiplier) {
		return now.getTime() - interval * multiplier;
	}
	
	public int getMessageCount() {
		return this.messageCount;
	}

	/**
	 * @return the time since last monitor in seconds
	 */
	public float getTimeSinceLastMonitor() {
		return ((float)(System.currentTimeMillis() - this.lastMonitor.getTime())) / 1000;
	}

	/**
	 * @return the time since last message in seconds
	 */
	public float getTimeSinceLastMessage() {
		return ((float)(System.currentTimeMillis() - this.lastMessage.getTime())) / 1000;
	}

	/**
	 * @return the time since last hearbeat in seconds
	 */
	public float getTimeSinceHearbeat() {
		return ((float)(System.currentTimeMillis() - this.lastHeartbeat.getTime())) / 1000;
	}

	/**
	 * @return true if this instance is the master
	 */
	public boolean isMaster() {
		return this.master;
	}

	/**
	 * @return true if this is a single-source application
	 */
	public boolean isSingleSource() {
		return singleSource;
	}

	public String getApplicationId() {
		return this.applicationId;
	}

	public void start() {
		this.running = true;
	}

	public void stop() {
		this.running = false;
		this.stopInbound();
	}

	public boolean isRunning() {
		return this.running;
	}

	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	public boolean isPaused() {
		return this.paused;
	}

	@Transactional
	public String pause() {
		if (this.pausing) {
			return "Already pausing";
		}
		if (this.paused) {
			return "Already paused";
		}
		this.pausing = true;
		this.stopInbound();
		try {
			ClusterStatus clusterStatus = lockClusterStatus();
			clusterStatus.setStatus(ClusterStatus.STATUS_PAUSED);
			this.clusterStatusRepository.updateLastProcessed(clusterStatus);
			if (this.singleSource) {
				logger.info("Pausing application");
				return "Pausing application";
			} else {
				logger.info("Pausing instance");
				return "Pausing instance";
			}
		} catch (Throwable t) {
			this.pausing = false;
			logger.error(t);
			return t.getMessage();
		}
	}

	@Transactional
	public String resume() {
		if (!this.paused) {
			return "Not paused";
		}
		try {
			ClusterStatus clusterStatus = lockClusterStatus();
			clusterStatus.setStatus(ClusterStatus.STATUS_RUNNING);
			this.clusterStatusRepository.updateLastProcessed(clusterStatus);
			if (this.singleSource) {
				logger.info("Resuming application");
				return "Resuming application";
			} else {
				logger.info("Resuming instance");
				return "Resuming instance";
			}
		} catch (Throwable t) {
			logger.error(t);
			return t.getMessage();
		}
	}

	public String obtainApplicationStatus() {
		try {
			ClusterStatus clusterStatus = this.clusterStatusRepository.find(this.applicationId);
			return clusterStatus.toString() + 
				" single-source:" + this.singleSource + 
				" paused:" + this.paused;
		} catch (Throwable t) {
			logger.error(t);
			return t.getMessage();
		}
	}
	
	public String pauseAll() {
		try {
			if (this.singleSource) {
				return "Single source application - use pause()";
			}
			int n = this.clusterStatusRepository.updateStatusAll(this.application, 
					ClusterStatus.STATUS_PAUSED);
			return "All instances will be paused (" + n + " rows updated)";
		} catch (Throwable t) {
			logger.error(t);
			return t.getMessage();
		}
	}

	public String resumeAll() {
		if (this.singleSource) {
			return "Single source application - use resume()";
		}
		try {
			int n = this.clusterStatusRepository.updateStatusAll(this.application, 
					ClusterStatus.STATUS_RUNNING);
			return "All instances will be resumed (" + n + " rows updated)";
		} catch (Throwable t) {
			logger.error(t);
			return t.getMessage();
		}
	}


}
