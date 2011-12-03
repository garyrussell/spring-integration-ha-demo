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
package org.springframework.integration.cluster.redis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.cluster.ClusterStatus;
import org.springframework.integration.cluster.ClusterStatusRepository;

/**
 * @author Gary Russell
 *
 */
public class ClusterStatusRepositoryImpl implements ClusterStatusRepository {

	private final Log logger = LogFactory.getLog(this.getClass());
	
	private final RedisTemplate<String, ClusterStatus> statusTemplate;
	
	private final String instanceInfo;
	
	private int timeout = 10000;
	
	private final DistributedLockHandler lockHandler;
	
	/**
	 * @param lockTemplate
	 * @param statusTemplate
	 */
	public ClusterStatusRepositoryImpl(StringRedisTemplate lockTemplate,
			RedisTemplate<String, ClusterStatus> statusTemplate,
			String instanceInfo) {
		this.statusTemplate = statusTemplate;
		this.instanceInfo = instanceInfo;
		this.lockHandler = new DistributedLockHandler(lockTemplate, this.timeout);		
	}

	public void create(ClusterStatus clusterStatus) {
		String applicationId = clusterStatus.getApplicationId();
		this.lockHandler.acquireLock(applicationId, this.instanceInfo);
		ClusterStatus status = statusTemplate.opsForValue().get(applicationId);
		if (status != null) {
			throw new DuplicateKeyException(
					"Cluster status already exists for application "
							+ applicationId);
		}
		statusTemplate.opsForValue().set(applicationId, clusterStatus);
		this.lockHandler.relinquishLock(applicationId, this.instanceInfo);
	}

	public ClusterStatus find(String applicationId) {
		ClusterStatus status = statusTemplate.opsForValue().get(applicationId);
		if (status == null) {
			throw new EmptyResultDataAccessException(1);
		}
		return status;
	}

	public ClusterStatus lock(String applicationId) {
		this.lockHandler.acquireLock(applicationId, this.instanceInfo);
		ClusterStatus status = statusTemplate.opsForValue().get(applicationId);
		if (status == null) {
			this.lockHandler.relinquishLock(applicationId, this.instanceInfo);			
			throw new EmptyResultDataAccessException(1);
		}
		return find(applicationId);
	}

	public void updateLastProcessed(ClusterStatus clusterStatus) {
		String applicationId = clusterStatus.getApplicationId();
		if (this.lockHandler.checkIOwnLock(applicationId)) {
			statusTemplate.opsForValue().set(applicationId, clusterStatus);
		} else {
			throw new OptimisticLockingFailureException("Held lock for " + applicationId
					+ " too long - unsafe to update protected key");			
		}
	}

	public void updateUsurper(ClusterStatus clusterStatus) {
		updateLastProcessed(clusterStatus);
	}

	public void updateMaster(ClusterStatus clusterStatus) {
		updateLastProcessed(clusterStatus);
	}

	public int updateStatusAll(String applicationId, String status) {
		// TODO - maybe change locks to a set? What about cross-contention?
		return 0;
	}

	public void unlock(String applicationId) {
		this.lockHandler.relinquishLock(applicationId, this.instanceInfo);
	}

}
