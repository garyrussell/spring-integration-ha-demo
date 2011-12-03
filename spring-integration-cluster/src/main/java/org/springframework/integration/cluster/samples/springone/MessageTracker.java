package org.springframework.integration.cluster.samples.springone;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;

public class MessageTracker {

	protected static String inboundFileName = System
			.getProperty("java.io.tmpdir")
			+ File.separator
			+ "inboundMessages"
			+ System.getenv("VCAP_APP_PORT") + ".txt";
	protected static String outboundFileName = System
			.getProperty("java.io.tmpdir")
			+ File.separator
			+ "outboundMessages" + System.getenv("VCAP_APP_PORT") + ".txt";

	private static Log logger = LogFactory.getLog(MessageTracker.class);

	public MessageTracker() {

	}

	public Message<?> trackInbound(Message<?> message) {
		logger.debug("inbound message " + message);

		writeMessage(message, inboundFileName);
		return message;
	}

	public synchronized void refresh() {
		logger.debug("refreshing tracker message store");

		for (File file : getMessageFiles("inboundMessages")) {
			logger.debug("deleting " + file.getAbsolutePath());
			if (!file.delete()) {
				logger.error("could not delete " + file.getAbsolutePath());
			}
		}

		for (File file : getMessageFiles("outboundMessages")) {
			if (!file.delete()) {
				logger.debug("deleting " + file.getAbsolutePath());
				logger.error("could not delete " + file.getAbsolutePath());
			}
		}

	}

	public Message<?> trackOutbound(Message<?> message) {
		logger.debug("writing outbound message " + message);
		writeMessage(message, outboundFileName);
		return message;
	}

	public List<Message<?>> getInboundMessages() {

		return getMessages("inboundMessages");
	}

	public List<Message<?>> getOutboundMessages() {
		return getMessages("outboundMessages");
	}

	private List<Message<?>> getMessages(final String fileNamePrefix) {

		List<Message<?>> allMessages = new LinkedList<Message<?>>();

		for (File file : getMessageFiles(fileNamePrefix)) {
			List<Message<?>> messages = readMessages(file.getAbsolutePath());
			allMessages.addAll(messages);
		}

		Collections.sort(allMessages, new Comparator<Message<?>>() {

			@Override
			public int compare(Message<?> m0, Message<?> m1) {
				return (int) (m0.getHeaders().getTimestamp() - m1.getHeaders()
						.getTimestamp());

			}

		});

		return allMessages;
	}

	private File[] getMessageFiles(final String fileNamePrefix) {
		return new File(System.getProperty("java.io.tmpdir"))
				.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith(fileNamePrefix);
					}
				});
	}

	protected synchronized void writeMessage(Message<?> message, String fileName) {
		try {

			List<Message<?>> list = readMessages(fileName);

			FileOutputStream fos = new FileOutputStream(fileName);
			FileLock lock = fos.getChannel().lock();
			try {
				list.add(message);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(list);
			} finally {
				lock.release();
				fos.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	protected synchronized List<Message<?>> readMessages(String fileName) {
		List<Message<?>> messages = new LinkedList<Message<?>>();
		try {
			FileInputStream fis = new FileInputStream(fileName);
			try {
				ObjectInputStream ois = new ObjectInputStream(fis);
				messages = (List<Message<?>>) ois.readObject();
				ois.close();
			} finally {
				fis.close();
			}
		} catch (FileNotFoundException e) {

		} catch (EOFException e) { 
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return messages;
	}
}
