package org.bs.tightrope.loadbalancer.network;


import lombok.extern.slf4j.Slf4j;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;


@Slf4j
public class BackendHandler extends SimpleChannelHandler {

  private final Channel frontendChannel;

  public BackendHandler(Channel frontendChannel) {
    this.frontendChannel = frontendChannel;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    this.frontendChannel.write(e.getMessage());
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    if (this.frontendChannel.isConnected()) {
      log.debug("closing channel from backend");
      this.frontendChannel.write(ChannelBuffers.EMPTY_BUFFER)
          .addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    Channel channel = e.getChannel();
    if (channel.isConnected()) {
      log.debug("Exception caught connecting to {}. {}", channel.getRemoteAddress(), e.getCause());
      channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }


}
