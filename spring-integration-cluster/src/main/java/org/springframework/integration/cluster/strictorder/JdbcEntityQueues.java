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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.integration.Message;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * @author Gary Russell
 *
 */
public class JdbcEntityQueues implements EntityQueues<String, Message<?>>, Trigger {

	private String dispatcherName;
	private JdbcOperations jdbcOperations;
	
	private List<String> initializationKeys;
	
	private Log logger = LogFactory.getLog(getClass());
	
	public JdbcEntityQueues(JdbcOperations jdbcOperations, String dispatcherName) {
		this.jdbcOperations = jdbcOperations;
		this.dispatcherName = dispatcherName;
		initializationKeys = new LinkedList<String>(keySet());
	}

	public Date nextExecutionTime(TriggerContext triggerContext) {
		if (this.initializationKeys.size() > 0) {
			return new Date(System.currentTimeMillis() + 1000);
		}
		return null;
	}

	public void setCapacity(int capacity) {
		throw new UnsupportedOperationException();
	}

	protected byte[] getBlobAsBytes(ResultSet rs)
			throws SQLException {
		return new DefaultLobHandler().getBlobAsBytes(rs, "message");
	}
	
	public Object remove(String key) {
		try {
			MessageWrapper mw = this.jdbcOperations.queryForObject(getFirstSql(), 
					new RowMapper<MessageWrapper>() {
						public MessageWrapper mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							try {
								byte[] bytes = getBlobAsBytes(rs);
								ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
								ObjectInputStream ois = new ObjectInputStream(bais);
								return new MessageWrapper(rs.getLong("id"), 
										(Message<?>) ois.readObject());
							} catch (IOException e) {
								throw new SQLException(e);
							} catch (ClassNotFoundException e) {
								throw new SQLException(e);
							}
						}
					}, key);
			this.jdbcOperations.update("delete from queued where id = ?", mw.getId());
			return mw.getMessage();
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	protected String getFirstSql() {
		return "select id, " +
				"entity_id, process_id, message from queued where entity_id = ? " +
				"order by id limit 1";
	}

	public int size(String key) {
		return this.jdbcOperations.queryForInt("select count(id) from queued where entity_id = ?",
				key);
	}

	public Set<String> keySet() {
		List<String> keys = this.jdbcOperations.query("select distinct entity_id from queued " +
				"where process_id = ?",
				new RowMapper<String>() {
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString("entity_id");
					}},
				this.dispatcherName);
		return new HashSet<String>(keys);
	}

	public void add(final String key, Message<?> message) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.jdbcOperations.execute(getInsertSql(),
				new AbstractLobCreatingPreparedStatementCallback(getLobHandler()) {
					protected void setValues(PreparedStatement ps, LobCreator lobCreator)
							throws SQLException, DataAccessException {
						ps.setString(1, key);
						ps.setString(2, dispatcherName);
						lobCreator.setBlobAsBytes(ps, 3, bos.toByteArray());
					}
				});
	}

	/**
	 * @return
	 */
	protected String getInsertSql() {
		return "insert into queued " +
				"(entity_id, process_id, message) " +
				"values(?, ?, ?)";
	}

	protected LobHandler getLobHandler() {
		return new DefaultLobHandler();
	}

	public String nextStartUpKey() {
		if (this.initializationKeys.size() < 1) {
			return null;
		}
		String key = this.initializationKeys.remove(0);
		logger.debug("Returning initial key:" + key);
		return key;
	}
	
	private class MessageWrapper {
		private long id;
		private Message<?> message;

		public MessageWrapper(long id, Message<?> message) {
			this.id = id;
			this.message = message;
		}

		public long getId() {
			return id;
		}

		public Message<?> getMessage() {
			return message;
		}
		
		
	}

}
