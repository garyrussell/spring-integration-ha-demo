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
package org.springframework.integration.cluster.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.integration.cluster.ClusterStatus;
import org.springframework.integration.cluster.ClusterStatusRepository;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author Gary Russell
 *
 */
@Repository
public class ClusterStatusRepositoryImpl implements ClusterStatusRepository {

	private JdbcOperations jdbcOperations;
	private RowMapper<ClusterStatus> rowMapper = new ClusterStatusRowMapper();
	
	public ClusterStatusRepositoryImpl(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	public void create(ClusterStatus clusterStatus) {
		jdbcOperations.update("insert into CLUSTER_STATUS (application_id, current_master, last_processed, usurp_timestamp) " +
				"values(?, ?, ?, ?)", 
				clusterStatus.getApplicationId(), clusterStatus.getCurrentMaster(),
				new Timestamp(clusterStatus.getLastProcessed().getTime()),
				new Timestamp(System.currentTimeMillis()));
	}
	
	public ClusterStatus find(String applicationId) {
		return jdbcOperations.queryForObject("select * from CLUSTER_STATUS where application_id = ?",
				this.rowMapper, applicationId);
	}
	
	public ClusterStatus lock(String applicationId) {
		return jdbcOperations.queryForObject("select * from CLUSTER_STATUS where application_id = ? FOR UPDATE",
				this.rowMapper, applicationId);
	}
	
	public void updateLastProcessed(ClusterStatus clusterStatus) {
		jdbcOperations.update("update CLUSTER_STATUS set current_master = ?, " +
				"status = ?, " +
				"last_processed = ?, " +
				"pending_usurper = ? where application_id = ?",
				clusterStatus.getCurrentMaster(),
				clusterStatus.getStatus(),
				new Timestamp(clusterStatus.getLastProcessed().getTime()),
				clusterStatus.getPendingUsurper(),
				clusterStatus.getApplicationId());
	}
	
	public void updateUsurper(ClusterStatus clusterStatus) {
		jdbcOperations.update("update CLUSTER_STATUS set pending_usurper = ?, " +
				"usurp_timestamp = ? where application_id = ?",
				clusterStatus.getPendingUsurper(),
				new Timestamp(clusterStatus.getUsurpTimestamp().getTime()),
				clusterStatus.getApplicationId());
	}
	
	public void updateMaster(ClusterStatus clusterStatus) {
		jdbcOperations.update("update CLUSTER_STATUS set current_master = ?, " +
				"pending_usurper = ?, " +
				"usurp_timestamp = ? where application_id = ?",
				clusterStatus.getCurrentMaster(),
				clusterStatus.getPendingUsurper(),
				new Timestamp(clusterStatus.getUsurpTimestamp().getTime()),
				clusterStatus.getApplicationId());
	}

	public int updateStatusAll(String applicationId, String status) {
		return jdbcOperations.update("update CLUSTER_STATUS set STATUS = ? " +
					"where application_id like ?",
					status, applicationId + "%");
	}

	private class ClusterStatusRowMapper implements RowMapper<ClusterStatus> {

		public ClusterStatus mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			return new ClusterStatus(rs.getString(1), rs.getString(2),
					rs.getString(3), rs.getTimestamp(4), rs.getString(5), 
					rs.getTimestamp(6));
		}
	}

	public void unlock(String applicationId) {
		// No-op for JDBC - transaction manager will release the lock
	}

}
