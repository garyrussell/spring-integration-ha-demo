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
package org.springframework.integration.cluster.strictorder;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jdbc.support.lob.OracleLobHandler;
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor;

/**
 * @author Gary Russell
 *
 */
public class JdbcEntityQueuesOracle extends JdbcEntityQueues {

	public JdbcEntityQueuesOracle(JdbcOperations jdbcOperations,
			String dispatcherName) {
		super(jdbcOperations, dispatcherName);
	}

	@Override
	protected byte[] getBlobAsBytes(ResultSet rs) throws SQLException {
		OracleLobHandler oracleLobHandler = new OracleLobHandler();
		oracleLobHandler.setNativeJdbcExtractor(new CommonsDbcpNativeJdbcExtractor());								
		byte[] bytes = oracleLobHandler.getBlobAsBytes(rs, "message");
		return bytes;
	}

	@Override
	protected LobHandler getLobHandler() {
		OracleLobHandler oracleLobHandler = new OracleLobHandler();
		oracleLobHandler.setNativeJdbcExtractor(new CommonsDbcpNativeJdbcExtractor());
		return oracleLobHandler;
	}

	
	@Override
	protected String getInsertSql() {
		return "insert into queued " +
				"(id, entity_id, process_id, message) " +
				"values(queued_sequence.nextval, ?, ?, ?)";
	}

	@Override
	protected String getFirstSql() {
		return "select * from (select id, " +
				"entity_id, process_id, message from queued where entity_id = ? " +
				"order by id) where rownum = 1";
	}
	
	
}
