package org.springframework.integration.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.integration.cluster.ClusterStatus;
import org.springframework.integration.cluster.ClusterStatusRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



@Ignore
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ClusterStatusJdbcRepositoryTests {
	
	static {
		System.setProperty("spring.profiles.active", "jdbc");		
	}
	
	private String applicationId = "foo";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	ClusterStatusRepository clusterStatusRepository;

	@Test
	public void testCreate() {
		ClusterStatus cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		ClusterStatus outCS = clusterStatusRepository.find(applicationId);
		cs.setLastProcessed(outCS.getLastProcessed());
		assertEquals(cs, outCS);
		outCS = clusterStatusRepository.lock(applicationId);
		assertEquals(cs, outCS);
		
		jdbcTemplate.update("delete from CLUSTER_STATUS where application_id = ?", applicationId);		
	}

	@Test
	public void testLastProcessed() {
		ClusterStatus cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		cs = clusterStatusRepository.lock(applicationId);
		Date ts = new Date();
		cs.setLastProcessed(ts);
		cs.setPendingUsurper("");
		clusterStatusRepository.updateLastProcessed(cs);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getLastProcessed());
		assertEquals("", cs.getPendingUsurper());
		
		jdbcTemplate.update("delete from CLUSTER_STATUS where application_id = ?", applicationId);		
	}
	
	@Test
	public void testUsurper() {
		ClusterStatus cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		cs = clusterStatusRepository.lock(applicationId);
		Date ts = new Date();
		cs.setUsurpTimestamp(ts);
		cs.setPendingUsurper("baz");
		clusterStatusRepository.updateUsurper(cs);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getUsurpTimestamp());
		assertEquals("baz", cs.getPendingUsurper());
		
		jdbcTemplate.update("delete from CLUSTER_STATUS where application_id = ?", applicationId);		
	}

	@Test
	public void testMaster() {
		ClusterStatus cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		cs = clusterStatusRepository.lock(applicationId);
		Date ts = new Date();
		cs.setUsurpTimestamp(ts);
		cs.setPendingUsurper("baz");
		clusterStatusRepository.updateUsurper(cs);
		cs = clusterStatusRepository.lock(applicationId);
		cs.setPendingUsurper("");
		ts = new Date();
		cs.setUsurpTimestamp(ts);
		cs.setCurrentMaster("baz");
		clusterStatusRepository.updateMaster(cs);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getUsurpTimestamp());
		assertEquals("", cs.getPendingUsurper());
		assertEquals("baz", cs.getCurrentMaster());
		
		jdbcTemplate.update("delete from CLUSTER_STATUS where application_id = ?", applicationId);		
	}
	
	@Test
	public void testCreateAfterNotFound() {
		ClusterStatus cs;
		try {
			cs = clusterStatusRepository.lock(applicationId);
			fail("Expected empty result");
		} catch (EmptyResultDataAccessException e) { }
		cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		ClusterStatus outCS = clusterStatusRepository.find(applicationId);
		cs.setLastProcessed(outCS.getLastProcessed());
		assertEquals(cs, outCS);
		outCS = clusterStatusRepository.lock(applicationId);
		assertEquals(cs, outCS);
		
		jdbcTemplate.update("delete from CLUSTER_STATUS where application_id = ?", applicationId);		
	}


}
