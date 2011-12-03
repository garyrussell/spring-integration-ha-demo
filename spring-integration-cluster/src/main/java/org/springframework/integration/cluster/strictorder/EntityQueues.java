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
package org.springframework.integration.cluster.strictorder;

import java.util.Set;

/**
 * @author Gary Russell
 *
 */
public interface EntityQueues<K, T> {

	/**
	 * Set the queue capacity
	 * @param capacity
	 */
	public abstract void setCapacity(int capacity);

	/**
	 * 
	 * @param key
	 * @return
	 */
	public abstract Object remove(K key);

	/**
	 * 
	 * @param key
	 * @return
	 */
	public abstract int size(K key);

	/**
	 * 
	 * @return
	 */
	public abstract Set<K> keySet();

	/**
	 * 
	 * @param key
	 * @param entity
	 */
	public abstract void add(K key, T entity);

}