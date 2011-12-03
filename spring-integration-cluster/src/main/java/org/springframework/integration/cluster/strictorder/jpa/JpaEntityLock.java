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
package org.springframework.integration.cluster.strictorder.jpa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.cluster.ClusterControl;
import org.springframework.integration.cluster.strictorder.Dispatcher;
import org.springframework.integration.cluster.strictorder.EntityLock;
import org.springframework.integration.cluster.strictorder.EntityQueues;
import org.springframework.integration.cluster.strictorder.LockNode;
import org.springframework.integration.cluster.strictorder.jpa.domain.JpaLockNode;
import org.springframework.integration.cluster.strictorder.jpa.repository.LockNodeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.springsource.insight.annotation.InsightEndPoint;

/**
 * @author Gary Russell
 *
 */
public class JpaEntityLock implements EntityLock {

	private final LockNodeRepository lockStatusRepository;
	private final String dispatcherName;
	private final ClusterControl clusterControl;
	private final EntityQueues<String, ?> entityQueues;
	private final String lockName;
	
	private final Log logger = LogFactory.getLog(this.getClass());

	public JpaEntityLock(LockNodeRepository lockStatusRepository,
			String dispatcherName,
			ClusterControl clusterControl, 
			EntityQueues<String, ?> entityQueues,
			String lockName) {
		this.lockStatusRepository = lockStatusRepository;
		this.dispatcherName = dispatcherName;
		this.clusterControl = clusterControl;
		this.entityQueues = entityQueues;
		this.lockName = lockName;
	}
	
	public String getDispatcherName() {
		return this.dispatcherName;
	}
	
	@Transactional
	@Deprecated
	public void lockEntity(String entityKey, String lockName) {
		this.lockEntity(entityKey, lockName, null);
	}
	
	@Transactional
	public void lockEntity(String entityKey, String lockName, String globalTx) {
		Assert.isTrue(!this.exists(entityKey, lockName));
		JpaLockNode lockStatus = new JpaLockNode();
		lockStatus.setEntityId(entityKey);
		lockStatus.setDispatcherId(this.dispatcherName);
		lockStatus.setGlobalTx(globalTx);
		lockStatus.setProcessId(lockName);
		lockStatus.setStatus(0);
		this.lockStatusRepository.save(lockStatus);
	 	logger.info("locked " + entityKey + ":" + lockName);		
	}

	public boolean exists(String entityKey, String lockName) {
		JpaLockNode locked = this.lockStatusRepository.findLocked(entityKey, this.dispatcherName, lockName);
		if (locked != null) {
			logger.debug(locked.getId() + " is locked");
		}
		return locked != null;
	}

	public boolean exists(String entityKey) {
		return this.lockStatusRepository.findLocked(entityKey, this.dispatcherName).size() > 0;
	}

	@Transactional
	public void releaseEntity(String entityKey, String lockName) {
		JpaLockNode lockStatus = this.lockStatusRepository.findLocked(entityKey, this.dispatcherName, lockName);
		if (lockStatus != null) {
			lockStatus.setStatus(JpaLockNode.RELEASED);
//			this.lockStatusRepository.save(lockStatus);
			logger.info("released " + entityKey + ":" + lockName);
		}
	}

	public void fullyReleaseEntity(LockNode lockNode) {
		if (!(lockNode instanceof JpaLockNode)) {
			logger.debug("Received dummy lock release");
			return;
		}
		//TODO: check if still locked
		JpaLockNode jpaLockNode = (JpaLockNode) lockNode;
		jpaLockNode.setStatus(JpaLockNode.AVAILABLE);
		logger.info("available " + jpaLockNode.getEntityId() + ":"
				+ jpaLockNode.getProcessId() + " (" + jpaLockNode.getId() + ")");		
	}

	@Transactional
	public void releaseEntity(String entityKey) {
		List<JpaLockNode> locks = this.lockStatusRepository.findLocked(entityKey, this.dispatcherName);
		for (JpaLockNode lock : locks) {
			this.releaseEntity(entityKey, lock.getProcessId());
		}
	}

	@Transactional
	public void releaseEntity(LockNode lockNode) {
		lockNode = this.lockStatusRepository.findOne(((JpaLockNode) lockNode).getId());
		this.fullyReleaseEntity(lockNode);
	}

	@Transactional
	public void fork(String entityKey, String fromLockName,
			String... toLockNames) {
		if (fromLockName == null) {
			fromLockName = dispatcherName;
		}
		Assert.isTrue(exists(entityKey, fromLockName),  fromLockName + " does not have a lock for entity [" + entityKey + "]" );
		for (String lockName : toLockNames) {
			this.lockEntity(entityKey, lockName);
		}
		this.releaseEntity(entityKey, fromLockName);
	}

	public Set<LockNode> getLocks(String entityKey) {
		List<JpaLockNode> locks = this.lockStatusRepository.findLocked(entityKey, this.dispatcherName);
		Set<LockNode> nodes = new HashSet<LockNode>();
		for (JpaLockNode lock : locks) {
			nodes.add(new LockNode(lock.getEntityId(), lock.getProcessId(), this.dispatcherName));
		}
		return nodes;
	}

	/**
	 * Used when polling for released locks (if the unlock process marks them 
	 * as such, instead of sending a message to {@link Dispatcher#processQueue(LockNode)}
	 * directly.
	 * 
	 * @return
	 */
	@Transactional
	@InsightEndPoint
	public List<? extends LockNode> released() {
		if (!clusterControl.isMaster()) {
			logger.trace("Not master; will not poll locks");
			return null;
		}
		List<JpaLockNode> unavailableLocks = this.lockStatusRepository
				.findUnavailable(this.dispatcherName);
		List<LockNode> locks = new ArrayList<LockNode>();
		Set<String> keys = new HashSet<String>();
		for (JpaLockNode lockNode : unavailableLocks) {
			keys.add(lockNode.getEntityKey());
			if (lockNode.getStatus() == JpaLockNode.RELEASED) {
				locks.add(lockNode);
			}
		}
		Set<String> queuedEntities = entityQueues.keySet();
		for (String queuedEntity : queuedEntities) {
			if (!keys.contains(queuedEntity)) {
				locks.add(new LockNode(queuedEntity, lockName, this.dispatcherName));
			}
		}
		if (logger.isDebugEnabled() && locks.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (LockNode lock : locks) {
				sb.append(lock.getEntityKey());
				if (!(lock instanceof JpaLockNode)) {
					sb.append('*');
				}
				sb.append(',');
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("]");
			logger.debug("Now " + locks.size() + " in released list:" + sb.toString());
		}
		if (locks.size() > 0) {
			return locks;
		}
		return null;
	}

	@Override
	public LockNode lockEntity(String entityKey) {
		Assert.isTrue(!this.exists(entityKey, lockName));
		JpaLockNode lockStatus = new JpaLockNode();
		lockStatus.setEntityId(entityKey);
		lockStatus.setDispatcherId(this.dispatcherName);
		lockStatus.setGlobalTx("");
		lockStatus.setProcessId(lockName);
		lockStatus.setStatus(0);
		this.lockStatusRepository.save(lockStatus);
	 	logger.info("locked " + entityKey + ":" + lockName);
	 	return lockStatus;
	}

}
