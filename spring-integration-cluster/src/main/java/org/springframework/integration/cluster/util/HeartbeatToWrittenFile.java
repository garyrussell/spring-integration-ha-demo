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
package org.springframework.integration.cluster.util;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.integration.cluster.Heartbeat;


/**
 * Used to write a heartbeat out to a file; used for 
 * File Reading Channel Adapter for heartbeat processing.
 * 
 * @author Gary Russell
 *
 */
public class HeartbeatToWrittenFile {

	private File directory;
	private String prefix;
	private String suffix;

	public HeartbeatToWrittenFile(String directory, String prefix, String suffix) {
		this.directory = new File(directory);
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	public void transform(Heartbeat heartbeat) {
		try {
			File out = File.createTempFile(prefix, suffix, directory);
			FileOutputStream os = new FileOutputStream(out);
			os.write(heartbeat.toString().getBytes());
			os.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
}
