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
package org.springframework.integration.cluster.strictorder.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.cluster.redis.DistributedLockHandler;
import org.springframework.integration.cluster.strictorder.EntityLock;
import org.springframework.integration.cluster.strictorder.LockNode;

/**
 * @author Gary Russell
 *
 */
public class RedisEntityLock implements EntityLock {

	private final DistributedLockHandler lockHandler;
	
	private final String dispatcherName;
	
	private final String instanceInfo;
	
	private int timeout = 60000;
	
	public RedisEntityLock(StringRedisTemplate lockTemplate, 
			String dispatcherName, String instanceInfo) {
		this.lockHandler = new DistributedLockHandler(lockTemplate,this.timeout);
		this.lockHandler.setThreadBound(false);
		this.dispatcherName = dispatcherName;
		this.instanceInfo = instanceInfo;
	}
	
	@Override
	public String getDispatcherName() {
		return this.dispatcherName;
	}
	
	private String makeKey(String entityKey) {
		return entityKey + "|" + this.dispatcherName;
	}

	@Override
	public LockNode lockEntity(String entityKey) {
		LockNode lockNode = new LockNode(entityKey, this.instanceInfo, this.dispatcherName);
		this.lockHandler.acquireLock(makeKey(entityKey), this.instanceInfo);
		return lockNode;
	}

	@Override
	public boolean exists(String entityKey) {
		return this.lockHandler.isLocked(makeKey(entityKey));
	}

	@Override
	public void releaseEntity(String entityKey) {
		this.lockHandler.relinquishLock(makeKey(entityKey), this.instanceInfo);
	}

	@Override
	public void releaseEntity(LockNode lockNode) {
		this.lockHandler.relinquishLock(makeKey(lockNode.getEntityKey()), this.instanceInfo);
	}

}
