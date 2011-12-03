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

import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Object sent to the master in the event it detects no real
 * messages have been received recently. Used as a keep-alive;
 * if the heartbeat is received by the master, it is deemed to
 * be alive.
 * @author Gary Russell
 *
 */
public class Heartbeat implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private transient static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd@HHmmss.SSS");
	
	private final Date timestamp;

	public Heartbeat() {
		this.timestamp = new Date();
	}
	
	public Heartbeat (String heartbeatString) {
		 this.timestamp = dateFormat.parse(heartbeatString,
				new ParsePosition(heartbeatString.indexOf('@')+1));
	}

	public Date getTimestamp() {
		return timestamp;
	}
	
	public String toString() {
		return "Heartbeat@" + dateFormat.format(this.timestamp);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Heartbeat other = (Heartbeat) obj;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}
}
