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
package org.springframework.integration.cluster.strictorder.jpa.domain;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.integration.cluster.strictorder.LockNode;

/**
 * @author Gary Russell
 *
 */
@Entity
@Table(name="LOCK_STATUS")
public class JpaLockNode extends LockNode implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public static final int LOCKED = 0;
	public static final int RELEASED = 50;
    public static final int AVAILABLE = 100;

//	@SuppressWarnings("unused")
	@Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator="ls_gen")
//    @SequenceGenerator(name="ls_gen", sequenceName="LOCK_STATUS_SEQUENCE")
	@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name="ENTITY_ID", nullable=false)
    private String entityId;
    @Column(name="DISPATCHER_ID", nullable=false)
    private String dispatcherId;
    @Column(name="GLOBAL_TX", nullable=true)
    private String globalTx;
    @Column(name="PROCESS_ID", nullable=false)
    private String processId;
    @Column(name="DEDUP_KEY", nullable=true)
    private String deDupKey;
    @Column(nullable=false)
    private Integer status;
    @Column(name="TS", insertable=false, updatable=false)
    private Timestamp timestamp;
    
	public JpaLockNode() {
		super(null, null, null);
	}

	public JpaLockNode(String entityKey, String lockName, String dispatcherName) {
		super(entityKey, lockName, dispatcherName);
	}
    
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the entityId
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	/**
	 * @return the dispatcherId
	 */
	public String getDispatcherId() {
		return dispatcherId;
	}

	/**
	 * @param dispatcherId the dispatcherId to set
	 */
	public void setDispatcherId(String dispatcherId) {
		this.dispatcherId = dispatcherId;
	}

	/**
	 * @return the globalTx
	 */
	public String getGlobalTx() {
		return globalTx;
	}

	/**
	 * @param globalTx the globalTx to set
	 */
	public void setGlobalTx(String globalTx) {
		this.globalTx = globalTx;
	}

	/**
	 * @return the processId
	 */
	public String getProcessId() {
		return processId;
	}

	/**
	 * @param processId the processId to set
	 */
	public void setProcessId(String processId) {
		this.processId = processId;
	}

	/**
	 * @return the deDupKey
	 */
	public String getDeDupKey() {
		return deDupKey;
	}

	/**
	 * @param deDupKey the deDupKey to set
	 */
	public void setDeDupKey(String deDupKey) {
		this.deDupKey = deDupKey;
	}

	/**
	 * @return the status
	 */
	public Integer getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	public String toString() {
		return entityId + ":" + dispatcherId + ":" + processId + ":" + status + ":" + timestamp + ":" + id;
	}
	
	@Override
	public String getLockName() {
		return this.getProcessId();
	}

	@Override
	public String getEntityKey() {
		return this.getEntityId();
	}

	public boolean equals(Object other){
		if (null == other){
			return false;
		}
		
		if (!(other instanceof JpaLockNode)){
			return false;
		}
	
		JpaLockNode otherLockNode = (JpaLockNode)other;
		if (id != 0 && otherLockNode.getId() != 0) {
			return (id == otherLockNode.getId());
		}
		return this.getKey().equals(otherLockNode.getKey());
	}

	@Override
	public int hashCode() {
		return this.getKey().hashCode();
	}

	@Override
	public String getDispatcherName() {
		return this.getDispatcherId();
	}

	@Override
	public String getKey() {
		return this.entityId + ":" + this.processId + ":" + this.dispatcherId;
	}
	
}
