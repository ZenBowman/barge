/**
 * Copyright 2013 David Rusek <dave dot rusek at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robotninjas.barge.rpc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.PrivateModule;
import io.netty.channel.nio.NioEventLoopGroup;
import org.robotninjas.protobuf.netty.client.RpcClient;
import org.robotninjas.protobuf.netty.server.RpcServer;

import javax.annotation.Nonnull;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

public class RpcModule extends PrivateModule {

  private static final int NUM_THREADS = 1;
  private final SocketAddress saddr;
  private final Optional<NioEventLoopGroup> eventLoopGroup;

  public RpcModule(@Nonnull SocketAddress saddr, NioEventLoopGroup eventLoopGroup) {
    this.saddr = checkNotNull(saddr);
    this.eventLoopGroup = Optional.fromNullable(eventLoopGroup);
  }

  public RpcModule(@Nonnull SocketAddress saddr) {
    this(saddr, null);
  }

  @Override
  protected void configure() {

    final NioEventLoopGroup eventLoop;
    if (!eventLoopGroup.isPresent()) {
      eventLoop = new NioEventLoopGroup(1);
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          eventLoop.shutdownGracefully();
        }
      });
    } else {
      eventLoop = eventLoopGroup.get();
    }

    bind(ListeningExecutorService.class)
      .annotatedWith(RaftExecutor.class)
      .toInstance(listeningDecorator(eventLoop));

    expose(ListeningExecutorService.class)
      .annotatedWith(RaftExecutor.class);

    bind(ScheduledExecutorService.class)
      .annotatedWith(RaftScheduler.class)
      .toInstance(listeningDecorator(eventLoop));

    expose(ScheduledExecutorService.class)
      .annotatedWith(RaftScheduler.class);

    bind(NioEventLoopGroup.class)
      .toInstance(eventLoop);

    RpcServer rpcServer = new RpcServer(eventLoop, saddr);
    bind(RpcServer.class)
      .toInstance(rpcServer);
    expose(RpcServer.class);

    bind(RaftClientProvider.class)
      .asEagerSingleton();

    bind(RpcClient.class)
      .toInstance(new RpcClient(eventLoop));

    bind(Client.class).asEagerSingleton();
    expose(Client.class);
  }

}
