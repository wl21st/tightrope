package org.bs.tightrope.loadbalancer.models;


import java.util.ArrayList;
import java.util.List;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bs.tightrope.loadbalancer.strategies.LoadBalancerStrategy;

@Slf4j
@ToString
public class ServerPool {

  private List<Server> servers;
  private LoadBalancerStrategy loadBalancerStrategy;

  public ServerPool(LoadBalancerStrategy loadBalancerStrategy) {
    this.loadBalancerStrategy = loadBalancerStrategy;
    this.servers = new ArrayList<Server>();

    log.info("Initializing server pool with {} strategy", this.loadBalancerStrategy.getName());
  }

  public Server selectServer() {
    return this.loadBalancerStrategy.selectServer(this);
  }

  public List<Server> getServers() {
    return servers;
  }

  public void addServer(Server server) {
    this.servers.add(server);
  }

  public List<Server> getAvailableServers() {
    List<Server> availableServers = new ArrayList<>();

    for (Server server : this.servers) {
      if (server.getAvailable().get()) {
        availableServers.add(server);
      }
    }

    log.debug("{} available servers in pool", availableServers.size());
    return availableServers;
  }
}
