If you don't have sonic; comment it out in the POM and switch the jms
infrastructure to use amq.

To run from the command-line

export MAVEN_OPTS='-Dbarrier.gate.instance.id=disp1 -Dcom.sun.management.jmxremote'

(to run multiple instances in different terminals, change disp1 to disp2, 3, etc).

Start a stand-alone instance of H2...

java -jar ~/.m2/repository/com/h2database/h2/1.2.147/h2-1.2.147.jar &

Adjust properties in properties/heartbeat.properties

mvn test -Psimple -o

(Press Enter to stop)

The simple test has nothing attached to the inbound channel so the only data are
heartbeats. 

Kill (hit enter) the active instance to watch another take over.

With the -Pjms, -Pfile and -Pjdbc samples you can stop the adapter with 
VisualVM or JConsole to cause a switch.

For strict ordering sample, use -Pjms-strict

