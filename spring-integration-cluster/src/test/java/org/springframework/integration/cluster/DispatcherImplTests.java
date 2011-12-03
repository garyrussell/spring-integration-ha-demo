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

import static junit.framework.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.integration.cluster.ClusterControl;
import org.springframework.integration.cluster.GateKeeperImpl;



/**
 * @author Gary Russell
 *
 */
public class DispatcherImplTests {

	@Mock
	ClusterControl clusterControl;
	
	@Before
	public void setup() {
		initMocks(this);
	}
	
	@Test
	public void testOk() {
		GateKeeperImpl dispatcher = new GateKeeperImpl(clusterControl);
		Mockito.when(clusterControl.verifyStatus(false)).thenReturn(true);
		Assert.assertEquals("foo", dispatcher.dispatch("foo"));
	}

	@Test
	public void testLostStatus() throws Exception {
		GateKeeperImpl dispatcher = new GateKeeperImpl(clusterControl);
		Mockito.when(clusterControl.verifyStatus(false)).thenReturn(false);
		try {
			dispatcher.dispatch("foo");
			fail("Expected exception");
		} catch (RuntimeException e) {}
	}
	
}
