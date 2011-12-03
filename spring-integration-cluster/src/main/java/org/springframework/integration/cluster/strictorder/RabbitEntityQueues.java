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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Gary Russell
 *
 */
public class RabbitEntityQueues implements
		EntityQueues<String, org.springframework.integration.Message<?>>,
		InitializingBean {

	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@Autowired
	private RabbitAdmin admin;
	
	@Override
	public void setCapacity(int capacity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object remove(String key) {
		try {
			return this.rabbitTemplate.receiveAndConvert(key);
		} catch (AmqpException e) {
			if (rootCause(e).getMessage().contains("NOT_FOUND")) {
				return null;
			}
			throw e;
		}
	}

	@Override
	public int size(String key) {
		List<Message> messages = new ArrayList<Message>();
		Message message;
		int count = 0;
		try {
			Binding binding = new Binding(key, DestinationType.QUEUE, "entities", key, null);
			this.admin.declareBinding(binding);
			while (true) {
				message = this.rabbitTemplate.receive(key);
				if (message == null) {
					break;
				}
				messages.add(message);
				count++;
			}
			for (Message save : messages) {
				this.rabbitTemplate.send("entities", key, save);
			}
		} catch (AmqpException e) {
			if (rootCause(e).getMessage().contains("NOT_FOUND")) {
				return 0;
			}
			throw e;
		}
		if (count == 0) {
			Binding binding = new Binding(key, DestinationType.QUEUE, "entities", key, null);
			this.admin.removeBinding(binding);
		}
		return count;
	}

	@Override
	public Set<String> keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(String key,  org.springframework.integration.Message<?> entity) {
		try {
			Queue queue = new Queue(key);
			this.admin.declareQueue(queue);
			Binding binding = new Binding(key, DestinationType.QUEUE, "entities", key, null);
			this.admin.declareBinding(binding);
			this.rabbitTemplate.convertAndSend("entities", key, entity);
		} catch (AmqpException e) {
			if (rootCause(e).getMessage().contains("NOT_FOUND")) {
				System.out.println("NF");
			}
			throw e;
		}
		
	}

	private Throwable rootCause(Throwable t) {
		if (t.getCause() != null) {
			return rootCause(t.getCause());
		} else {
			return t;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Exchange exchange = new DirectExchange("entities");
		this.admin.declareExchange(exchange);
	}
}
