<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page session="false" %>
<html>
<head>
	<title>Home</title>
	 <link rel="stylesheet" href="resources/main.css" media="screen"/>
	<META HTTP-EQUIV="REFRESH" CONTENT="5">
</head>
<body>
<h4>
ME: ${host}:${port} MASTER: ${mbean.master}<br/>
</h4>
<h4>
	${status}
</h4>

<table>
	<thead>
		<tr><td><h4>MBean</h4></td></tr>
	</thead>
	<tr>
		<td>Messages Processed by this Instance</td><td>${mbean.messageCount}</td>
	</tr>
	<tr>
		<td>Time Since Last Message</td><td>${mbean.timeSinceLastMessage}</td>
	</tr>
</table>

${message}

<br/>
<form  method="get">
	<input type="submit" name="Stop" formaction="stopInbound" formmethod="post" value="Stop Inbound (force switch)"><br/>
	<input type="submit" name="Send" formaction="send" formmethod="post" value="Send messages to Strict Order Dispatcher"><br/>
</form>
<br/><hr/>
<p>
This is a demonstration of the Spring Integration cluster controller and strict ordering. MBean statistics for the cluster controller are displayed at the top of the page. After 
starting this application, you can optionally start another cluster controller instance with a different VCAP_APP_PORT, e.g. 1235 (see spring-integration-cluster/README.md)for the <i>Stop Inbound</i> demo.  
</p>
<h3>Use an external browser - does not work in embedded eclipse browser</h3>
<ul>
 <li>The <i>Stop Inbound</i> button will force a switch to an external cluster controller instance if there is one running. If this is the only instance,
 you can still observe the process, where this instance will simply take over again, and restart the adapter.</li>
 <li>The <i>Send messages</i> button will send 50 messages (10 messages each for 5 entity keys) to the <i>strict.ordering.inbound</i> message channel and display the results.</li> 
</ul>

 
</body>
</html>
