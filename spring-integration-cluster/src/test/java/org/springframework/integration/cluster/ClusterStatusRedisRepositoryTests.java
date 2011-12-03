package org.springframework.integration.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ClusterStatusRedisRepositoryTests {

	static {
		System.setProperty("spring.profiles.active", "redis");
	}

	private String applicationId = "foo";

	@Autowired @Qualifier("status.redisTemplate")
	private RedisTemplate<String, ClusterStatus> redisTemplate;

	@Autowired @Qualifier("lock.redisTemplate")
	private StringRedisTemplate lockRedisTemplate;

	@Autowired
	private ClusterStatusRepository clusterStatusRepository;

	@Before
	public void clean() {
		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
	}

	@Test
	public void testCreate() {
		ClusterStatus cs = new ClusterStatus(applicationId, "bar");
		clusterStatusRepository.create(cs);
		ClusterStatus outCS = clusterStatusRepository.find(applicationId);
		cs.setLastProcessed(outCS.getLastProcessed());
		assertEquals(cs, outCS);
		outCS = clusterStatusRepository.lock(applicationId);
		assertEquals(cs, outCS);

		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
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
		clusterStatusRepository.unlock(applicationId);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getLastProcessed());
		assertEquals("", cs.getPendingUsurper());

		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
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
		clusterStatusRepository.unlock(applicationId);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getUsurpTimestamp());
		assertEquals("baz", cs.getPendingUsurper());

		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
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
		clusterStatusRepository.unlock(applicationId);
		cs = clusterStatusRepository.lock(applicationId);
		cs.setPendingUsurper("");
		ts = new Date();
		cs.setUsurpTimestamp(ts);
		cs.setCurrentMaster("baz");
		clusterStatusRepository.updateMaster(cs);
		clusterStatusRepository.unlock(applicationId);
		cs = clusterStatusRepository.lock(applicationId);
		assertEquals(ts, cs.getUsurpTimestamp());
		assertEquals("", cs.getPendingUsurper());
		assertEquals("baz", cs.getCurrentMaster());

		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
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

		redisTemplate.opsForValue().getOperations().delete(applicationId);
		lockRedisTemplate.opsForValue().getOperations().delete(applicationId + ".lock");
	}


}
