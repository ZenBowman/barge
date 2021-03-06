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

package org.robotninjas.barge;

import com.google.common.base.Optional;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import io.netty.channel.nio.NioEventLoopGroup;
import org.robotninjas.barge.annotations.ClusterMembers;
import org.robotninjas.barge.annotations.LocalReplicaInfo;
import org.robotninjas.barge.log.LogModule;
import org.robotninjas.barge.rpc.RpcModule;
import org.robotninjas.barge.state.StateModule;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
class RaftModule extends PrivateModule {

  private static final long DEFAULT_TIMEOUT = 225;

  private final Replica local;
  private long timeout = DEFAULT_TIMEOUT;
  private final File logDir;
  private final List<Replica> members;
  private final StateMachine stateMachine;
  private Optional<NioEventLoopGroup> eventLoopGroup = Optional.absent();

  public RaftModule(@Nonnull Replica local, @Nonnull List<Replica> members,
                    @Nonnull File logDir, @Nonnull StateMachine stateMachine) {

    this.local = checkNotNull(local);
    checkArgument(timeout > 0);
    this.logDir = checkNotNull(logDir);
    this.members = checkNotNull(members);
    this.stateMachine = checkNotNull(stateMachine);
  }

  public void setNioEventLoop(NioEventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = Optional.of(eventLoopGroup);
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  protected void configure() {

    install(new StateModule(timeout));
    if (eventLoopGroup.isPresent()) {
      install(new RpcModule(local.address(), eventLoopGroup.get()));
    } else {
      install(new RpcModule(local.address()));
    }
    install(new LogModule(logDir, stateMachine));

    bind(Replica.class)
      .annotatedWith(LocalReplicaInfo.class)
      .toInstance(local);

    bind(new TypeLiteral<List<Replica>>() {})
      .annotatedWith(ClusterMembers.class)
      .toInstance(members);

    bind(RaftService.class);
    expose(RaftService.class);

  }
}
