package org.springframework.integration.cluster.strictorder;



import org.apache.log4j.Logger;
import org.springframework.integration.Message;
import org.springframework.integration.cluster.strictorder.entitykey.EntityKeyExtractor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import com.springsource.insight.annotation.InsightEndPoint;

/**
 * Enforces strict ordering by using an {@link EntityLock}. This is configured as a {@link MessageRouter}. If the entityKey
 * extracted from the Message is not locked, the message will be simply routed to the outputChannel. If a lock exists, the 
 * Message will be held in an internal queue. When all locks on the entity are released, the next queued message will be dispatched. 
 * 
 * @author David Turanski
 * @author Gary Russell
 *
 */
public class Dispatcher  {
	private static final String QUEUED_HEADER_KEY = "strict.order.queued.key";
	private static final String HEADER_LOCK = "strict.order.lock";

	@SuppressWarnings("rawtypes")
	// Manages internal queues
	private EntityQueues entityQueues;
    
	//A distributed lock implementation
	private final EntityLock  entityLock;

	private static Logger logger = Logger.getLogger(Dispatcher.class);
	
	//A strategy interface used to extract the entityKey from the message. If not set, the payload will be used as the key
	private EntityKeyExtractor<Message<?>,?> entityKeyExtractor;
	
	/**
	 * 
	 * @param entityLock
	 * @param outputChannelName
	 */
	public Dispatcher(EntityLock  entityLock){
		this.entityLock = entityLock;
		
	}

	/**
	 * Dispatch or queue the Message
	 * @param message
	 * @return
	 */
	@InsightEndPoint
	public synchronized Message<?> dispatch(Message<?> message) {
	
		logger.debug("got message " + message);
		String key = (String)extractKey(message);
	    Message<?> transformedMessage = message;
	    
	    /*
	     * Message removed from queue in processQueue() (we're still
	     * running on that thread 
	     */
	    if ( message.getHeaders().get(QUEUED_HEADER_KEY) != null ){
		    logger.debug ("processing queued message "+ transformedMessage);
	    	LockNode lockNode = (LockNode) message.getHeaders().get(HEADER_LOCK);
	    	entityLock.releaseEntity(lockNode);
	    	lockNode = entityLock.lockEntity(key);
			transformedMessage = MessageBuilder.fromMessage(transformedMessage)
					.setHeader(HEADER_LOCK, lockNode)
					.build();
	    	return transformedMessage; 
	    }
	    
	    /*
	     * Message from original producer. It may be that the lock is clear but the queue has
	     * not processed yet
	     */
		boolean entityLocked = entityLock.exists(key);
		if (!entityLocked){
			logger.debug ("no lock on entity - processing message "+ transformedMessage);
			LockNode lockNode = entityLock.lockEntity(key);
			transformedMessage = MessageBuilder.fromMessage(transformedMessage)
					.setHeader(HEADER_LOCK, lockNode)
					.build();
	    } else {
    		logger.debug("entity locked - queuing message "+ message);
			queue(message);
			transformedMessage = null;
		}
		  
		return transformedMessage;
	}

	/**
	 * Process the next queued message if lock is cleared. Message will
	 * ultimately be sent to dispatch() on this same thread.
	 * @param entityKey
	 * @return
	 */
	@InsightEndPoint
	public synchronized Message<?> processQueue(LockNode lockNode){
        Message<?> queuedMessage = null;
        String entityKey = lockNode.getEntityKey();
        queuedMessage = nextMessage(entityKey);
        if (queuedMessage == null) {
        	entityLock.releaseEntity(lockNode);
        	return null;
        } else {
        	// Add the lock to the message header so we can unlock it during dispatch
        	return MessageBuilder.fromMessage(queuedMessage)
        		.setHeader(HEADER_LOCK, lockNode).build();
        }
	}
	
	@SuppressWarnings("unchecked")
	private Message<?> nextMessage(String entityKey){
		Message<?> queuedMessage = null;
		queuedMessage = (Message<?>)entityQueues.remove(entityKey);
		if (queuedMessage == null) {
			logger.info("queue empty for "+"[" + entityKey + "]");
		} else {
			logger.info("got next message from queue "+"[" + entityKey + "]");
		}
		return queuedMessage;
	}
	
	/**
	 * 
	 * @param entityKeyExtractor
	 */
	public void setEntityKeyExtractor(EntityKeyExtractor<Message<?>, ?> entityKeyExtractor){
		Assert.notNull(entityKeyExtractor, "Entity Key Extractor must not be null");		
		this.entityKeyExtractor = entityKeyExtractor;
	}
	
	/**
	 * @param entityQueues the entityQueues to set
	 */
	public void setEntityQueues(EntityQueues<?, ?> entityQueues) {
		Assert.notNull(entityQueues, "Entity Queues must not be null");		
		this.entityQueues = entityQueues;
	}

	private Object extractKey(Message<?> message) {
		return (null == entityKeyExtractor) ? message.getPayload() : entityKeyExtractor.getKey(message);
	}

	@SuppressWarnings("unchecked")
	private void queue(Message<?> message) {
		Message<?> queuedMessage = MessageBuilder.fromMessage(message).setHeaderIfAbsent(QUEUED_HEADER_KEY,true).build();
		entityQueues.add(extractKey(message),queuedMessage);
	}

	public void unlock(Message<?> message) {
		String key = (String) extractKey(message);
		this.entityLock.releaseEntity(key);
	}

}
