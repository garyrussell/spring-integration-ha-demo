package org.springframework.integration.cluster.strictorder;

import java.io.Serializable;
/**
 * A value object to hold entity lock information
 * @author David Turanski
 *
 */
public class LockNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String instanceInfo;
	private final String dispatcherName;
	private final String entityKey;

	
	@SuppressWarnings("unused")
	private LockNode(){throw new UnsupportedOperationException();}
	
	public LockNode(String entityKey, String lockName, String dispatcherName){
		this.entityKey = entityKey; 
		this.instanceInfo = lockName;
		this.dispatcherName = dispatcherName; 
	}
	
	public String getLockName() {
		return instanceInfo;
	}
	
 	public String getEntityKey() {
		return entityKey;
	}

	public boolean equals(Object other){
		if (null == other){
			return false;
		}
		
		if (!(other instanceof LockNode)){
			return false;
		}
		
		LockNode otherLockNode = (LockNode)other;
		return this.getKey().equals(otherLockNode.getKey());
	}
	
	public int hashCode(){
	   return getKey().hashCode();
	}
	
	public String toString(){
		return ("entityKey [" + entityKey + "] instanceInfo [" + instanceInfo + "] dispatcherName [" + dispatcherName + "]");
	}

	public String getDispatcherName() {
		return dispatcherName;
	}
	
	public String getKey(){
		return entityKey + ":" + instanceInfo + ":" + dispatcherName;
	}
}
