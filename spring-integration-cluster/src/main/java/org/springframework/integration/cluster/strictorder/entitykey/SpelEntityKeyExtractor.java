package org.springframework.integration.cluster.strictorder.entitykey;

 
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
/**
 * 
 * @author David Turanski
 *
 * @deprecated Use version in cic-common
 */
@Deprecated
public class SpelEntityKeyExtractor implements EntityKeyExtractor<Object, String>{
	 
	private volatile Expression payloadExpression;

	private final SpelExpressionParser parser = new SpelExpressionParser();
	
	public String getKey(Object entity) {
		Object evaluationResult = entity;
		if (payloadExpression != null) {
			 evaluationResult =  payloadExpression.getValue(entity);
		}	 
		return (String) evaluationResult; 
	}
	
	/**
	 * 
	 * @param payloadExpression
	 */
	public void setPayloadExpression(String payloadExpression) {
		if (payloadExpression == null) {
			this.payloadExpression = null;
		}
		else {
			this.payloadExpression = this.parser.parseExpression(payloadExpression);
		}
	}
}
