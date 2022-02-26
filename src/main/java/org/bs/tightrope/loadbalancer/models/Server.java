package org.bs.tightrope.loadbalancer.models;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Server {

  private final String hostname;

  private final int port;

  private AtomicBoolean available = new AtomicBoolean(true);

}
