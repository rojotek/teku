/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.storage.client;

import static tech.pegasys.teku.logging.StatusLogger.STATUS_LOG;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.teku.core.lookup.BlockProvider;
import tech.pegasys.teku.infrastructure.async.AsyncRunner;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.protoarray.ProtoArrayStorageChannel;
import tech.pegasys.teku.storage.api.FinalizedCheckpointChannel;
import tech.pegasys.teku.storage.api.ReorgEventChannel;
import tech.pegasys.teku.storage.api.StorageQueryChannel;
import tech.pegasys.teku.storage.api.StorageUpdateChannel;
import tech.pegasys.teku.storage.store.StoreBuilder;
import tech.pegasys.teku.util.config.Constants;

public class StorageBackedRecentChainData extends RecentChainData {
  private static final Logger LOG = LogManager.getLogger();
  private final BlockProvider blockProvider;
  private final StorageQueryChannel storageQueryChannel;

  public StorageBackedRecentChainData(
      final MetricsSystem metricsSystem,
      final StorageQueryChannel storageQueryChannel,
      final StorageUpdateChannel storageUpdateChannel,
      final ProtoArrayStorageChannel protoArrayStorageChannel,
      final FinalizedCheckpointChannel finalizedCheckpointChannel,
      final ReorgEventChannel reorgEventChannel,
      final EventBus eventBus) {
    super(
        metricsSystem,
        storageQueryChannel::getHotBlocksByRoot,
        storageUpdateChannel,
        protoArrayStorageChannel,
        finalizedCheckpointChannel,
        reorgEventChannel,
        eventBus);
    this.storageQueryChannel = storageQueryChannel;
    this.blockProvider = storageQueryChannel::getHotBlocksByRoot;
    eventBus.register(this);
  }

  public static SafeFuture<RecentChainData> create(
      final MetricsSystem metricsSystem,
      final AsyncRunner asyncRunner,
      final StorageQueryChannel storageQueryChannel,
      final StorageUpdateChannel storageUpdateChannel,
      final ProtoArrayStorageChannel protoArrayStorageChannel,
      final FinalizedCheckpointChannel finalizedCheckpointChannel,
      final ReorgEventChannel reorgEventChannel,
      final EventBus eventBus) {
    StorageBackedRecentChainData client =
        new StorageBackedRecentChainData(
            metricsSystem,
            storageQueryChannel,
            storageUpdateChannel,
            protoArrayStorageChannel,
            finalizedCheckpointChannel,
            reorgEventChannel,
            eventBus);

    return client.initializeFromStorageWithRetry(asyncRunner);
  }

  @VisibleForTesting
  public static RecentChainData createImmediately(
      final MetricsSystem metricsSystem,
      final StorageQueryChannel storageQueryChannel,
      final StorageUpdateChannel storageUpdateChannel,
      final ProtoArrayStorageChannel protoArrayStorageChannel,
      final FinalizedCheckpointChannel finalizedCheckpointChannel,
      final ReorgEventChannel reorgEventChannel,
      final EventBus eventBus) {
    StorageBackedRecentChainData client =
        new StorageBackedRecentChainData(
            metricsSystem,
            storageQueryChannel,
            storageUpdateChannel,
            protoArrayStorageChannel,
            finalizedCheckpointChannel,
            reorgEventChannel,
            eventBus);

    return client.initializeFromStorage().join();
  }

  private SafeFuture<RecentChainData> initializeFromStorage() {
    STATUS_LOG.beginInitializingChainData();
    return processStoreFuture(requestInitialStore());
  }

  private SafeFuture<RecentChainData> initializeFromStorageWithRetry(
      final AsyncRunner asyncRunner) {
    STATUS_LOG.beginInitializingChainData();
    return processStoreFuture(requestInitialStoreWithRetry(asyncRunner));
  }

  private SafeFuture<RecentChainData> processStoreFuture(
      SafeFuture<Optional<StoreBuilder>> storeFuture) {
    return storeFuture.thenApply(
        maybeStore -> {
          maybeStore
              .map(builder -> builder.blockProvider(blockProvider).build())
              .ifPresent(this::setStore);
          STATUS_LOG.finishInitializingChainData();
          return this;
        });
  }

  private SafeFuture<Optional<StoreBuilder>> requestInitialStore() {
    return storageQueryChannel
        .onStoreRequest()
        .orTimeout(Constants.STORAGE_REQUEST_TIMEOUT, TimeUnit.SECONDS);
  }

  private SafeFuture<Optional<StoreBuilder>> requestInitialStoreWithRetry(
      final AsyncRunner asyncRunner) {
    return requestInitialStore()
        .exceptionallyCompose(
            (err) -> {
              if (Throwables.getRootCause(err) instanceof TimeoutException) {
                LOG.trace("Storage initialization timed out, will retry.");
                return asyncRunner.runAfterDelay(
                    () -> requestInitialStoreWithRetry(asyncRunner),
                    Constants.STORAGE_REQUEST_TIMEOUT,
                    TimeUnit.SECONDS);
              } else {
                STATUS_LOG.fatalErrorInitialisingStorage(err);
                return SafeFuture.failedFuture(err);
              }
            });
  }
}
