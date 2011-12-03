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
package org.springframework.integration.cluster.samples.strictorder;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StrictOrderSampleTests {

	static {
		System.setProperty("spring.profiles.active", "amqp-redis");
	}
	
	@Autowired @Qualifier("sample.inbound")
	SubscribableChannel inbound;
	
	@Autowired @Qualifier("strict.ordering.inbound")
	SubscribableChannel soInbound;
	
	@Autowired @Qualifier("test.output")
	PollableChannel testOut;
	
	@Test
	public void test() throws Exception {
		//TODO: When running in standalone - send a message to the queue instead of putting in on a channel.
		Message<String> message = MessageBuilder.withPayload("Hello1")
				.setHeader("sequence", 1)
				.build();
		soInbound.send(message);
		Message<?> out = testOut.receive(10000);
		assertNotNull(out);
		soInbound.send(message);
		soInbound.send(message);
		out = testOut.receive(10000);
		assertNotNull(out);
		out = testOut.receive(10000);
		assertNotNull(out);
		
//		Thread.sleep(120000);
//		inbound.send(message);
//		out = testOut.receive(10000);
//		assertNotNull(out);
		
	}

}
