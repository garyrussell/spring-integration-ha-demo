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

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.cluster.strictorder.RabbitEntityQueues;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitEntityQueuesTests {

	@Autowired
	private RabbitEntityQueues entityQueues;
	
	@Test
	public void testSize() {
		while (entityQueues.remove("abc") != null) {
			System.out.println("removed");
		}
		assertEquals(0, entityQueues.size("abc"));
		GenericMessage<String> message1 = new GenericMessage<String>("Hello, world!");
		GenericMessage<String> message2 = new GenericMessage<String>("Hello, world!");
		entityQueues.add("abc", message1);
		entityQueues.add("abc", message2);
		assertEquals(2, entityQueues.size("abc"));
		assertEquals(message1, entityQueues.remove("abc"));
		assertEquals(message2, entityQueues.remove("abc"));
		assertEquals(0, entityQueues.size("abc"));
	}

}
