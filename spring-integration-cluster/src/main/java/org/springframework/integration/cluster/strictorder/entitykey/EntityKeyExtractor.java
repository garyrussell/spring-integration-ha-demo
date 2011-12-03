package org.springframework.integration.cluster.strictorder.entitykey;
/**
 * 
 * @author David Turanski
 *
 * @param <E>
 * @param <K>
 * 
 */
public interface EntityKeyExtractor<E,K> {
   public K getKey(E entity);
}