package org.bs.tightrope.loadbalancer.strategies;

import java.util.List;
import java.util.Random;
import org.bs.tightrope.loadbalancer.models.Server;
import org.bs.tightrope.loadbalancer.models.ServerPool;

public class RandomStrategy implements LoadBalancerStrategy {

  private final String NAME = "Random";

  @Override
  public Server selectServer(final ServerPool serverPool) {
    List<Server> serverList = serverPool.getAvailableServers();
    return serverList.get(new Random().nextInt(serverList.size()));
  }

  @Override
  public String getName() {
    return NAME;
  }

}
