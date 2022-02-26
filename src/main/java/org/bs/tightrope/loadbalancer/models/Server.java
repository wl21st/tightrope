package org.bs.tightrope.loadbalancer.models;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class Server {

  private final String hostname;
  private final int port;
  private AtomicBoolean available = new AtomicBoolean(true);


}
