package org.springframework.integration.cluster.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.integration.Message;
import org.springframework.integration.cluster.ClusterControl;
import org.springframework.integration.cluster.ClusterManagementMBean;
import org.springframework.integration.cluster.ClusterStatusRepository;
import org.springframework.integration.cluster.samples.springone.MessageTracker;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {

	private static final Logger logger = LoggerFactory
			.getLogger(HomeController.class);

	@Autowired
	private ClusterStatusRepository repository;

	@Autowired
	private ClusterControl clusterControl;

	@Autowired
	private ClusterManagementMBean mbean;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private MessageTracker messageTracker;

	private static final int NUM_MESSAGES = 10;
	private static final int NUM_ENTITY_KEYS = 5;
	private static String filter="all";

	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model) {
		guts(model);
		return "home";
	}

	@RequestMapping(value = "refresh", method = RequestMethod.GET)
	public String refresh(Model model) {
		messageTracker.refresh();
		return home(model);
	}

	/**
	 * @param model
	 */
	private void guts(Model model) {
		String status = repository.find(this.clusterControl.getApplicationId())
				.toString();
		logger.info(status);
		model.addAttribute("host", System.getenv("VCAP_APP_HOST"));
		model.addAttribute("port", System.getenv("VCAP_APP_PORT"));
		model.addAttribute("status", status);
		model.addAttribute("message", "");
		model.addAttribute("mbean", this.mbean);
	}

	/**
	 * This is just the auto-refresh after we issued a stop.
	 * Just re-display.
	 */
	@RequestMapping(value = "stopInbound", method = RequestMethod.GET)
	public String stopGet(Model model) {
		guts(model);
		return "home";
	}
	
	@RequestMapping(value = "stopInbound", method = RequestMethod.POST)
	public String stop(Model model) {
		guts(model);
		if (this.mbean.isMaster()) {
			this.mbean.stopInbound();
			model.addAttribute("message", "Stopped");
		} else {
			model.addAttribute("message", "Not the master");
		}
		return "home";
	}

	@RequestMapping(value = "send", method = RequestMethod.GET)
	public String get() {
		return "so";
	}
	@RequestMapping(value = "send", method = RequestMethod.POST)
	public String send(Model model) {
		guts(model);
		filter="all";
		messageTracker.refresh();

		for (int j = 0; j < NUM_ENTITY_KEYS; j++) {
			for (int i = 0; i < NUM_MESSAGES; i++) {
				
				// final int ii = i % 20; // force new input while queued

				final int sequence = i;
	
				MessagePostProcessor mpp = new MessagePostProcessor() {
					public org.springframework.amqp.core.Message postProcessMessage(
							org.springframework.amqp.core.Message message)
							throws AmqpException {
						message.getMessageProperties().setHeader("sequence",
								sequence);
						return message;
					}
				};
				String payload = "Test" + j;
				this.amqpTemplate.convertAndSend("strict.order.exchange",
				"cluster.inbound", payload, mpp);
				
				logger.debug("Sent:" + payload + " sequence=" + i);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return "so";
	}

	@RequestMapping(value = "refreshReceived", method = RequestMethod.GET)
	@ResponseBody
	public String refreshReceived(Model model) {
		return display(messageTracker.getInboundMessages());
	}

	private String display(List<Message<?>> messages) {
		StringBuilder sb = new StringBuilder("<table>");
		sb.append("<tr>").append("<th>Time</th>").append("<th>Entity Key</th>")
				.append("<th>Sequence</th>").append("</tr>");
		for (Message<?> msg : messages) {
			if (filter.equals("all") || msg.getPayload().equals(filter)) {
				sb.append("<tr>").append("<td>")
						.append(new SimpleDateFormat("mm:ss.SSS").format(new Date((Long)msg.getHeaders().get("timestamp"))))
						.append("</td>").append("<td>")
						.append(msg.getPayload()).append("</td>")
						.append("<td>")
						.append(msg.getHeaders().get("sequence"))
						.append("</td>").append("</tr>");
			}

		}
		sb.append("</table>");
		return sb.toString();
	}

	@RequestMapping(value = "refreshOrdered", method = RequestMethod.GET)
	@ResponseBody
	public String refreshOrdered(Model model) {
		return display(messageTracker.getOutboundMessages());
	}

	@RequestMapping(value = "setFilter", method = RequestMethod.GET)
	public void setFilter(@Param("filter") String filter) {
		logger.debug("filter = " + filter);
		HomeController.filter = filter;
	}
}
