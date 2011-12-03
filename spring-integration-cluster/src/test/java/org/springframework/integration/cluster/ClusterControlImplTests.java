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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.integration.cluster.ClusterControlImpl;
import org.springframework.integration.cluster.ClusterStatus;
import org.springframework.integration.cluster.ClusterStatusRepository;
import org.springframework.integration.cluster.ControlBusGateway;
import org.springframework.integration.cluster.Heartbeat;
import org.springframework.integration.cluster.HeartbeatGateway;


/**
 * @author Gary Russell
 *
 */
public class ClusterControlImplTests {
	
	@Mock
	private ControlBusGateway controlBusGateway;
	
	@Mock
	private HeartbeatGateway<Heartbeat> heartbeatGateway;

	@Mock
	private ClusterStatusRepository clusterStatusRepository;

	private ClusterStatus clusterStatus = new ClusterStatus("foo", "bar");
	
	private ClusterStatus clusterStatusFromStub;

	int interval = 1000;
	int delay = (int) (interval * 2.1);


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testVerifyStatusMaster() {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				60000, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		when(clusterStatusRepository.lock("foo")).thenReturn(clusterStatus);
		clusterStatus.setPendingUsurper("baz");
		assertTrue(clusterControl.verifyStatus(false));
		verify(clusterStatusRepository).updateLastProcessed(clusterStatus);
		assertEquals("", clusterStatus.getPendingUsurper());
		// second call to take short path
		assertTrue(clusterControl.verifyStatus(false));
	}

	@Test
	public void testVerifyStatusNotMaster() {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		clusterStatus.setCurrentMaster("baz");
		when(clusterStatusRepository.lock("foo")).thenReturn(clusterStatus);
		assertFalse(clusterControl.verifyStatus(false));
		verify(clusterStatusRepository, never()).updateLastProcessed(clusterStatus);
		verify(this.controlBusGateway).sendCommand("@'inboundAdapter'.stop()");
	}

	@Test
	public void testHeartbeat() {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		clusterControl.sendHeartbeat();
		verify(this.heartbeatGateway).sendHeartbeat(any(Heartbeat.class));
	}

	@Test
	public void testStop() {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		clusterControl.stopInbound();
		verify(this.controlBusGateway).sendCommand("@'inboundAdapter'.stop()");
	}

	@Test
	public void testStart() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		when(clusterStatusRepository.lock("foo")).thenReturn(clusterStatus);
		clusterControl.start();
		clusterControl.doMonitor();
		clusterControl.startInbound();
		verify(this.controlBusGateway,times(2)).sendCommand("@'inboundAdapter'.start()");
	}
	
	@Test
	public void testStartDisallowed() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, this.clusterStatusRepository);
		clusterStatus.setCurrentMaster("baz");
		when(clusterStatusRepository.find("foo")).thenReturn(clusterStatus);
		when(clusterStatusRepository.lock("foo")).thenReturn(clusterStatus);
		clusterControl.doMonitor();
		try {
			clusterControl.startInbound();
			fail("Expected Exception");
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().startsWith("Cannot start adapter"));
		}
	}
	
	@Test
	public void testMaster() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				new ClusterStatusRepositoryStub());
		clusterControl.start();
		clusterControl.doMonitor(); // create initial status object 
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.start()");
		assertNotNull(clusterStatusFromStub);
		assertEquals("foo", clusterStatusFromStub.getApplicationId());
		assertEquals("bar", clusterStatusFromStub.getCurrentMaster());
		clusterControl.doMonitor();
		Thread.sleep(delay);
		clusterControl.doMonitor();
		verify(heartbeatGateway).sendHeartbeat(any(Heartbeat.class));
		clusterControl.doMonitor();
		Thread.sleep(delay*2);
		clusterControl.doMonitor();
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.stop()");
	}
	
	@Test
	public void testNoMaster() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				this.clusterStatusRepository);
		clusterControl.start();
		ClusterStatus clusterStatus = new ClusterStatus("foo", "");
		when(clusterStatusRepository.lock("foo")).thenReturn(clusterStatus);
		clusterControl.doMonitor(); // create initial status object 
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.start()");
		assertNotNull(clusterStatus.getLastProcessed());
		assertEquals("foo", clusterStatus.getApplicationId());
		assertEquals("bar", clusterStatus.getCurrentMaster());
	}

	@Test
	public void testNoRow() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				this.clusterStatusRepository);
		clusterControl.start();
		ClusterStatus clusterStatus = new ClusterStatus("foo", "bar");
		when(clusterStatusRepository.lock("foo"))
			.thenThrow(new EmptyResultDataAccessException(1))
			.thenReturn(clusterStatus);
		clusterControl.doMonitor(); // create initial status object
		verify(clusterStatusRepository).create(any(ClusterStatus.class));
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.start()");
		assertNotNull(clusterStatus.getLastProcessed());
		assertEquals("foo", clusterStatus.getApplicationId());
		assertEquals("bar", clusterStatus.getCurrentMaster());
	}

	@Test
	public void testNoRowLoser() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				this.clusterStatusRepository);
		clusterControl.start();
		ClusterStatus clusterStatus = new ClusterStatus("foo", "baz");
		when(clusterStatusRepository.lock("foo"))
			.thenThrow(new EmptyResultDataAccessException(1))
			.thenReturn(clusterStatus);
		doThrow(new DuplicateKeyException("test")).when(clusterStatusRepository).create(any(ClusterStatus.class));
		clusterControl.doMonitor(); // create initial status object
		verify(clusterStatusRepository).create(any(ClusterStatus.class));
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.stop()");
	}

	@Test
	public void testUsurper() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", delay,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				new ClusterStatusRepositoryStub());
		clusterControl.start();
		clusterControl.doMonitor();
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.start()");
		assertNotNull(clusterStatusFromStub);
		assertEquals("foo", clusterStatusFromStub.getApplicationId());
		assertEquals("bar", clusterStatusFromStub.getCurrentMaster());
		clusterStatusFromStub.setCurrentMaster("baz");
		clusterControl.doMonitor();
		Thread.sleep(delay*4);
		clusterControl.doMonitor();
		Thread.sleep(delay*4);
		clusterControl.doMonitor();
		verify(controlBusGateway,times(2)).sendCommand("@'inboundAdapter'.start()");
	}
	
	@Test
	public void testOtherUsurper() throws Exception {
		ClusterControlImpl clusterControl = new ClusterControlImpl("foo", true, "bar", 
				"inboundAdapter", 30000,
				interval * 2, this.controlBusGateway, this.heartbeatGateway, 
				new ClusterStatusRepositoryStub());
		clusterControl.start();
		clusterControl.doMonitor();
		verify(controlBusGateway).sendCommand("@'inboundAdapter'.start()");
		assertNotNull(clusterStatusFromStub);
		assertEquals("foo", clusterStatusFromStub.getApplicationId());
		assertEquals("bar", clusterStatusFromStub.getCurrentMaster());
		clusterStatusFromStub.setCurrentMaster("baz");
		clusterControl.doMonitor();
		Thread.sleep(delay);
		clusterControl.doMonitor();
		clusterStatusFromStub.setPendingUsurper("quz");
		Thread.sleep(delay/2);
		clusterControl.doMonitor();
		assertEquals("quz", clusterStatusFromStub.getPendingUsurper());
	}
	
	private class ClusterStatusRepositoryStub implements ClusterStatusRepository {

		public void create(ClusterStatus clusterStatus) {
			clusterStatusFromStub = clusterStatus;
		}

		public ClusterStatus find(String applicationId) {
			return clusterStatusFromStub;
		}

		public ClusterStatus lock(String applicationId) {
			return clusterStatusFromStub;
		}

		public void updateLastProcessed(ClusterStatus clusterStatus) {
			assertSame(clusterStatus, clusterStatusFromStub);
		}

		public void updateUsurper(ClusterStatus clusterStatus) {
			assertSame(clusterStatus, clusterStatusFromStub);			
		}

		public void updateMaster(ClusterStatus clusterStatus) {
			assertSame(clusterStatus, clusterStatusFromStub);			
		}

		public int updateStatusAll(String applicationId, String status) {
			return 0;
		}

		public void unlock(String applicationId) {
		}
		
	}
	
}
