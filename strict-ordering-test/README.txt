War wrapper for strict-ordering sample app from cic-integration-cluster.

Has a web controller that allows you to see status of the cluster as
well as minimal control.

1. Stop the inbound adapter on the master to force a master switch
2. Send 10x5 messages (5 messages for each entity id)

run in tcServer with
export VCAP_APP_HOST=<hostname>
export VCAP_APP_PORT=tcServerHttpPort


