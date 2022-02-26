package org.bs.tightrope.loadbalancer;

import static java.lang.Thread.sleep;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bs.tightrope.loadbalancer.models.Server;
import org.bs.tightrope.loadbalancer.models.ServerPool;
import org.bs.tightrope.loadbalancer.models.Statistics;
import org.bs.tightrope.loadbalancer.network.FrontendHandler;
import org.bs.tightrope.loadbalancer.strategies.LoadBalancerStrategy;
import org.bs.tightrope.loadbalancer.strategies.RoundRobinStrategy;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

@Slf4j
@ToString
public class LoadBalancer {

  private Channel acceptor;
  private ChannelGroup allChannels;
  private ServerBootstrap bootstrap;

  private ServerPool upstreamServerPool;
  private int listenPort;

  private boolean exitFlag = false;

  private Statistics statistics;
  private ClientSocketChannelFactory clientSocketChannelFactory;

  public LoadBalancer(ServerPool upstreamServerPool, int listenPort) {
    this.upstreamServerPool = upstreamServerPool;
    this.listenPort = listenPort;
    this.statistics = new Statistics();
  }

  /**
   * Create cached thread pool.
   *
   * @param minThreadCount minimal thread count of the pool.
   * @param maxThreadCount maximum thread count of the pool.
   * @return created thread pool.
   */
  private static Executor createCachedThreadPool(int minThreadCount, int maxThreadCount) {
    return new ThreadPoolExecutor(minThreadCount,
        maxThreadCount,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
  }

  public synchronized void start() {

    final Executor bossPool = Executors.newCachedThreadPool();
    final Executor workerPool = Executors.newCachedThreadPool();

    bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(bossPool, workerPool));
    final ClientSocketChannelFactory clientSocketChannelFactory = new NioClientSocketChannelFactory(
        bossPool, workerPool);

    bootstrap.setOption("child.tcpNoDelay", true);
    allChannels = new DefaultChannelGroup("handler");

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(
            new FrontendHandler(allChannels, clientSocketChannelFactory, upstreamServerPool,
                statistics));
      }
    });

    log.info("Starting on port {}", listenPort);
    acceptor = bootstrap.bind(new InetSocketAddress(listenPort));

    if (acceptor.isBound()) {
      log.info("Server started successfully");
    }
  }

  public synchronized void stop() {
    this.allChannels.close().awaitUninterruptibly();
    this.acceptor.close().awaitUninterruptibly();
    this.bootstrap.releaseExternalResources();
  }

  public Statistics getStatistics() {
    return this.statistics;
  }

  public static void main(String[] args) throws InterruptedException {
    final LoadBalancerStrategy loadBalancerStrategy = new RoundRobinStrategy();
    final ServerPool serverPool = new ServerPool(loadBalancerStrategy);

    getUpstreamServers().forEach(server -> serverPool.addServer(server));

    final LoadBalancer loadBalancer = new LoadBalancer(serverPool, getLoadBalancerListenPort());

    log.info("Start the server with configuration {}", loadBalancer);

    loadBalancer.start();

    Thread shutdownHook = new Thread() {
      @Override
      public void run() {
        loadBalancer.stop();
      }
    };

    Runtime.getRuntime().addShutdownHook(shutdownHook);

    loadBalancer.doEventLoop();

  }

  private void doEventLoop() throws InterruptedException {
    int lastConnections = 0;
    long lastBytesIn = 0L;
    while (!exitFlag) {
      Statistics status = getStatistics();

      int connections = status.getFrontendConnections();
      long bytesIn = status.getBytesIn();

      boolean hasProgress = (lastConnections != connections || lastBytesIn != bytesIn);

      if (hasProgress) {
        log.info("connections={} in traffic={} bytes, connections.diff={}, traffic.diff={} bytes",
            connections, bytesIn, connections - lastConnections, bytesIn - lastBytesIn);
      }
      lastConnections = connections;
      lastBytesIn = bytesIn;

      sleep(2000L);
    }
  }

  private static int getLoadBalancerListenPort() {
    return Integer.getInteger("listen.port", 9090);
  }

  private static List<Server> getUpstreamServers() {
    String serverCfg = System.getProperty("upstream.servers", "127.0.0.1:8080");

    List<Server> result = new ArrayList<>();
    for (String serverString : serverCfg.split(",")) {
      String[] serverParts = serverString.trim().split(":");

      if (serverParts.length != 2) {
        throw new IllegalArgumentException(
            "Illegal server input: " + serverCfg + ", expected format is host:port!");
      }

      String host = serverParts[0];
      Integer port = Integer.parseInt(serverParts[1]);

      result.add(new Server(host, port));
    }

    return result;
  }

}
