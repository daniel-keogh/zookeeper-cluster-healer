import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClusterHealer implements Watcher {

    // Path to the worker jar
    private final String pathToProgram;
    // The number of worker instances we need to maintain at all times
    private final int numberOfWorkers;

    private ZooKeeper zooKeeper;

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String PARENT_NAMESPACE = "/workers";

    public ClusterHealer(int numberOfWorkers, String pathToProgram) {
        this.numberOfWorkers = numberOfWorkers;
        this.pathToProgram = pathToProgram;
    }

    /**
     * Check if the `/workers` parent znode exists, and create it if it doesn't. Decide for yourself what type of znode
     * it should be (e.g.persistent, ephemeral etc.). Check if workers need to be launched.
     */
    public void initialiseCluster() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(PARENT_NAMESPACE , this);

        if (stat == null) {
            zooKeeper.create(PARENT_NAMESPACE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        checkRunningWorkers();
    }

    /**
     * Instantiates a Zookeeper client, creating a connection to the Zookeeper server.
     */
    public void connectToZookeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    /**
     * Keeps the application running waiting for Zookeeper events.
     */
    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    /**
     * Closes the Zookeeper client connection.
     */
    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    /**
     * Handles Zookeeper events related to: - Connecting and disconnecting from the Zookeeper server. - Changes in the
     * number of workers currently running.
     *
     * @param event A Zookeeper event
     */
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeChildrenChanged:
                checkRunningWorkers();
                break;
            default:
                break;
        }
    }

    /**
     * Checks how many workers are currently running.
     * If less than the required number, then start a new worker.
     */
    public void checkRunningWorkers() {
        try {
            List<String> children = zooKeeper.getChildren(PARENT_NAMESPACE, this);

            System.out.println("Currently there are " + children.size() + " workers.");

            if (children.size() < numberOfWorkers) {
                startWorker();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a new worker using the path provided as a command line parameter.
     *
     * @throws IOException
     */
    public void startWorker() throws IOException {
        File file = new File(pathToProgram);
        String command = "java -jar " + file.getName();
        System.out.println(String.format("Launching worker instance : %s ", command));
        Runtime.getRuntime().exec(command, null, file.getParentFile());
    }
}
