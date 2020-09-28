# Dataflow

This is a sample for getting data into a Spring Cloud Dataflow data pipeline.

## Architecture

```
  tcp server -> connection handler -> kafka destination
```

The TCP Server is intended to act as a real-world counterpart. It handles client connections and cleanup.
It will also react to heartbeat messages, as well as seed test data to all connected clients.

The Connection Handler application will connect to the TCP Server and, when data payloads are recieved,
transform them into an `InternalMessage` and stream them to an Apache Kafka destination.

### Prerequisites

* Apache Kafka (installed when setting up Spring cloud Data Flow https://dataflow.spring.io/docs/installation/kubernetes/kubectl/)
* Redis (installed from bitnami helm chart)

### Running the Apps

**TCP Server**

Start the TCP Server:
* Locate `TcpServerApplication`, right-click and select `Run`

**Connection Handler**

Start the TCP Server:
* Locate `ConnectionHandlerApplication`, right-click and select `Run`

### Kakfa Console Logging

To watch the output in the Kakfa Topic, use the following command:

```bash
$ kafka-console-consumer --topic adapter.raw --from-beginning --bootstrap-server localhost:9092
```

## Notes

Subsequent changes will create a Spring Cloud Dataflow pipeline to process this stream

## Containerization

Spring Boot provides native support for container images.  This, as well as appropriate kubernetes deployment artifacts
will be added.

### Steps

**NOTE:** be sure to have installed both kafka and redis prior to performing these steps!!

1. Open the terminal and execute `./mvnw -pl connection-handler spring-boot:build-image`
1. Wait for the image to be added to the local docker registry, verify with `docker images`
1. Tag the docker image `docker tag [IMAGE ID] [DOCKERHUB USERNAME]/connection-handler:latest`
1. Push the image `docker push dmfrey/connection-handler:latest`
1. Edit `/connection-handler/kubernetes/connection-handler-deployment.yml`, set the`image` to `[DOCKERHUB USERNAME]/connection-handler`
1. Create the deployment `kubectl create -f /connection-handler/kubernetes/connection-handler-deployment.yml`

