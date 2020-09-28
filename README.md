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

* Apache Kafka (requires Zookeeper)
* Redis

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
