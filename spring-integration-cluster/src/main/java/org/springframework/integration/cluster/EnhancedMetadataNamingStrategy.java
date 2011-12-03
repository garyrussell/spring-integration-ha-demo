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
package org.springframework.integration.cluster;

import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;

/**
 * @author Gary Russell
 *
 * @deprecated Use the class in cic-common
 */
@Deprecated
public class EnhancedMetadataNamingStrategy extends MetadataNamingStrategy {

	private String staticNameParts = "";
	
	/**
	 * @param annotationSource
	 */
	public EnhancedMetadataNamingStrategy(
			AnnotationJmxAttributeSource annotationSource) {
		super(annotationSource);
	}

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey)
			throws MalformedObjectNameException {
		ObjectName objectName = super.getObjectName(managedBean, beanKey);
		if (this.staticNameParts.length() == 0) {
			return objectName;
		}
		return new ObjectName(objectName.getCanonicalName() + this.staticNameParts);
	}

	/**
	 * Static properties that will be added to all object names.
	 * 
	 * @param objectNameStaticProperties the objectNameStaticProperties to set
	 */
	public void setObjectNameStaticProperties(Map<String, String> objectNameStaticProperties) {
		StringBuilder builder = new StringBuilder();
		for (String key : objectNameStaticProperties.keySet()) {
			builder.append("," + key + "=" + objectNameStaticProperties.get(key));
		}
		this.staticNameParts = builder.toString();
	}

}
