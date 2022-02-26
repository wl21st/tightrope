package org.bs.tightrope.loadbalancer.strategies;


import org.bs.tightrope.loadbalancer.models.Server;
import org.bs.tightrope.loadbalancer.models.ServerPool;

public interface LoadBalancerStrategy {

  Server selectServer(ServerPool serverPool);

  String getName();

}
