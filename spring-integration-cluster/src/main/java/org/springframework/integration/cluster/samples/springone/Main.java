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
package org.springframework.integration.cluster.samples.springone;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Gary Russell
 *
 */
public class Main {
	
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && "amq".equals(args[0])) {
			new ClassPathXmlApplicationContext("/META-INF/spring/integration/amq-broker-context.xml");
			System.out.println("Broker started; Hit Enter to terminate");
			System.in.read();
			System.exit(0);
		}
		
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/META-INF/spring/integration/sample-strict-order-context.xml");
		System.out.println("Enter \n to terminate; otherwise message");
		boolean amqp = System.getProperty("spring.profiles.active").contains("amqp");
		AmqpTemplate amqpTemplate = null;
		JmsTemplate jmsTemplate = null;
		if (amqp) {
			amqpTemplate = ctx.getBean(AmqpTemplate.class);
		} else {
			jmsTemplate = ctx.getBean(JmsTemplate.class);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String msg = br.readLine();
			if (msg.length() == 0) {
				break;
			}
			if (amqp) {
				amqpTemplate.convertAndSend("cluster.inbound", msg);
			} else {
				jmsTemplate.convertAndSend(msg);
			}
		}
		ctx.close();
	}

}
