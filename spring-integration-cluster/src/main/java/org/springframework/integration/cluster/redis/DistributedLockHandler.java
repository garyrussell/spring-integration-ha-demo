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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Gary Russell
 *
 */
public class DistributedLockHandler {

	private final Log logger = LogFactory.getLog(this.getClass());

	private ThreadLocal<Date> acquiredThreadLocal = new ThreadLocal<Date>();

	private StringRedisTemplate lockTemplate;

	private int timeout;
	
	private boolean threadBound = true;
	
	/**
	 * @param lockTemplate
	 */
	public DistributedLockHandler(StringRedisTemplate lockTemplate, int timeout) {
		this.lockTemplate = lockTemplate;
		this.timeout = timeout;
	}

	public void acquireLock(String key, String owner) {
		int n = 0;
		while (this.lockTemplate.opsForValue().setIfAbsent(
				key + ".lock",
				getLockValue(owner)) == false) {
			if (++n > this.timeout / 100) {
				throw new CannotAcquireLockException(
						"Failed to procure cluster lock for application "
								+ key);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new DataRetrievalFailureException(
						"Interrupted while procuring lock", e);
			}
		}
		this.acquiredThreadLocal.set(new Date());
		if (logger.isDebugEnabled()) {
			logger.debug("Lock acquiredThreadLocal for " + key + " by "
					+ owner);
		}
		this.lockTemplate.opsForValue().getOperations()
				.expire(key + ".lock", timeout*2, TimeUnit.MILLISECONDS);
	}

	/**
	 * @param owner
	 * @return
	 */
	private String getLockValue(String owner) {
		if (threadBound) {
			return owner + ":" + Thread.currentThread().getId();
		} else {
			return owner;
		}
	}

	public void relinquishLock(String key, String owner) {
		String instanceInfo = this.lockTemplate.opsForValue().get(key + ".lock");
		if (!getLockValue(owner).equals(instanceInfo)) {
			logger.debug(owner
					+ " cannot relinquish lock; owned by " + instanceInfo);
			return;
		}
		if (threadBound && System.currentTimeMillis() > this.acquiredThreadLocal.get().getTime() + this.timeout) {
			logger.error("Held lock for " + key
					+ " too long - unsafe to relinquish - allowing expiration");
			return;
		}
		this.lockTemplate.opsForValue().getOperations()
				.delete(key + ".lock");
		if (logger.isDebugEnabled()) {
			logger.debug("Lock relinquished for " + key);
		}
	}

	public boolean checkIOwnLock(String key) {
		if (System.currentTimeMillis() > this.acquiredThreadLocal.get().getTime() + this.timeout) {
			String message = "Held lock for " + key
					+ " too long - unsafe to update protected key";
			logger.error(message);
			return false;
		}
		return true;
	}

	public boolean isLocked(String key) {
		boolean isLocked = this.lockTemplate.opsForValue().get(key + ".lock") != null;
		if (logger.isDebugEnabled()) {
			logger.debug(key + ".lock: locked=" + isLocked);
		}
		return isLocked;
	}

	/**
	 * @return the threadBound
	 */
	public boolean isThreadBound() {
		return threadBound;
	}

	/**
	 * @param threadBound the threadBound to set
	 */
	public void setThreadBound(boolean threadBound) {
		this.threadBound = threadBound;
	}
}
