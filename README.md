# Introduction

This repository contains the demonstration code used during the SpringOne presentation by Gary Russell and David Turanski (VMware).

### Implementing HA Architecture with Spring Integration

The slide deck can be found [here](http://portal.sliderocket.com/vmware/ImplementingHAArchitecturesWithSpringIntegration).

It's comprised of two projects

* spring-integration-cluster
* strict-ordering-test

The spring-integration-cluster project is the main demonstration code; the strict-ordering-test project is a web application that demonstrates the use of the clustering code for an application that requires strict ordering.

The cluster project supports

* Single source applications (e.g. file or JDBC inbound pollers, or JMS when strict ordering is needed)
* Strict ordering (downstream of the cluster component)
* JMX control of the inbound adapters



Strategy interfaces are used

#### Clustering

* ClusterControl
* ClusterStatusRepository
* Gatekeeper

#### Strict Ordering

* EntityLock


The following implementations are currently provided

* jms with jdbc
* amqp(rabbit) with redis

Two flavors of jdbc are provided

* mysql
* oracle


When running the demo, the strict ordering component can be included or not.

Each of these features is enabled using Spring 3.1 profiles to select the appropriate beans.

# Getting Started

The quickest way to get started is to clone this repository, then cd into spring-integration-cluster.

If you are using one of the JDBC flavors, you will have to add the tables to the database and configure the URL, credentials etc 
in the sample-strict-order-context.xml. SQL scripts for the tables are in src/main/resources.

Each instance is identified by two environment variables 'VCAP_APP_HOST' and 'VCAP_APP_PORT'. For *nix environments, 
a shell script (runner.sh) is provided to set up these variables, For other environments you will have to set these up yourself.

The cluster controller exposes attributes and operations so JMX should be enabled to access them using VisualVM or JConsole.
Again, runner.sh will set this up on Linux/Unix.

So, assuming Linux, source (.) this script, with a different port for each...

. runner.sh 1235
and
. runner.sh 1236

(Let's reseve the default 1234 for the web app - see later).


In each terminal start the instance, using maven...

mvn clean test -DskipTests=true -PspringOne -Dwhich=$VCAP_APP_PORT -Dspring.profiles.active="jms-jdbc,mysql,cluster"

Omit the 'cluster' profile to enable Strict Ordering (cluster means just cluster).

Supported profiles are

* jms-jdbc,mysql
* jms-jdbc,oracle
* amqp-redis

(each with, or without 'cluster').

The JMS version is currently configured for ActiveMQ, a convenience class AMQ.java is provided to start a broker.

After a short while, you should see both instances emit information as to which one has been elected as the master. You should
see periodic heartbeats because no real data is being processed.

In either console you can send a message by typing something; you should see the message being processed by one of the instances.

Now comes the interesting part. Open VisualVM (or JConsole); the -Dwhich paramter on the command line enables you to identify 
the instances in VisualVM. Drill down to the spring.application->ClusterControl MBean and use the stopInbound() operation to
stop the inbound adapter on the master instance. This simulates a failure and you should see the other instance take over the
mastership.


# Web Application

Deploy strict-ordering-test after configuring the spring.profiles.active (in web.xml) to match your choice above (in this case
do NOT include the 'cluster' profile).

Open the home page http://localhost:8080/strict-ordering-test

You may have to stop the other instances to force the web app to be the master instance. You can restart them afterwards.

The web app has two functions - you can stop the adapter and watch another instance take over, or you can submit a set of
messages which will demonstrate the strict ordering code, showing that each entity is only processed one-at-a-time.



# Feedback

Please provide feedback via GitHub's Issues.

Other implementations are welcome too.

Thanks!





















