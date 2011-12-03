package org.springframework.integration.cluster.strictorder;


/**
 * 
 * @author David Turanski
 *
 */
public interface EntityLock {
    
	/**
	 * 
	 * @return the dispatcher name. One instance of EntityLock per dispatcher
	 */
	public String getDispatcherName();
     
     /**
      * 
      * @param entityKey
      */
     public LockNode lockEntity(String entityKey);
     
     /**
      * 
      * @param entityKey
      * @return
      */
     public boolean exists(String entityKey);
     
     
    /**
     * Release the lock
     * @param entityKey
     */
     public void releaseEntity(String entityKey);

	/**
	 * @param lockNode
	 */
	public void releaseEntity(LockNode lockNode);
     
}
