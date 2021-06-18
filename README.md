# Cluster Healing with Zookeeper

Y4S1 Distributed Systems ZooKeeper Assignment.

## Introduction

Application which uses Zookeeper to monitor and automatically heal a cluster of workers. The cluster healer will launch the requested number of workers, then monitor the cluster to ensure that the requested number of workers is running. If a worker dies, the cluster healer should launch more workers to keep the total number of workers at the requested number.

## Operation

On startup, the cluster healer:

- Connects to Zookeeper

- Checks if the `/workers` parent znode exists, and if it doesn't, creates it.

- Starts the requested number of workers
  - The number of workers and the path to the worker application are passed in as command-line arguments.

- Watch the cluster to check if workers die.
  - Replacement workers are started when workers die.
  - The number of running workers should always be the requested number.
  
## Workers

A worker application, `faulty-worker.jar` is included in the repository. On startup, this application connects to Zookeeper and creates an ephemeral, sequential znode called `worker_` under the `/workers` parent znode, e.g. `/workers /worker_0000001`. This worker is programmed to continually crash at random intervals, which will cause the znode it created to be removed.

## Building and Running the Cluster Healer Application

### Building

Use the maven `package` goal from the IntelliJ tool window to build an executable jar from your code.

### Running

Running the below command from the project directory will start up 3 worker instances using the provided `faulty-worker.jar`, and will monitor the cluster to ensure that 3 instances are always running. **_Ensure that you've started the Zookeeper server first_**.

```
java -jar target/cluster-healer-1.0-SNAPSHOT-jar-with-dependencies.jar 3 ../faulty-worker.jar
```
