/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.teku.storage.events;

import com.google.common.primitives.UnsignedLong;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.datastructures.blocks.SlotAndBlockRoot;
import tech.pegasys.teku.datastructures.forkchoice.VoteTracker;
import tech.pegasys.teku.datastructures.state.BeaconState;
import tech.pegasys.teku.datastructures.state.Checkpoint;

public class StorageUpdate {

  private final Optional<UnsignedLong> genesisTime;
  private final Optional<FinalizedChainData> finalizedChainData;
  private final Optional<Checkpoint> justifiedCheckpoint;
  private final Optional<Checkpoint> bestJustifiedCheckpoint;
  private final Map<Bytes32, SlotAndBlockRoot> stateRoots;
  private final Map<Bytes32, SignedBeaconBlock> hotBlocks;
  private final Map<UnsignedLong, VoteTracker> votes;
  private final Set<Bytes32> deletedHotBlocks;

  public StorageUpdate(
      final Optional<UnsignedLong> genesisTime,
      final Optional<FinalizedChainData> finalizedChainData,
      final Optional<Checkpoint> justifiedCheckpoint,
      final Optional<Checkpoint> bestJustifiedCheckpoint,
      final Map<Bytes32, SignedBeaconBlock> hotBlocks,
      final Set<Bytes32> deletedHotBlocks,
      final Map<UnsignedLong, VoteTracker> votes,
      final Map<Bytes32, SlotAndBlockRoot> stateRoots) {
    this.genesisTime = genesisTime;
    this.finalizedChainData = finalizedChainData;
    this.justifiedCheckpoint = justifiedCheckpoint;
    this.bestJustifiedCheckpoint = bestJustifiedCheckpoint;
    this.hotBlocks = hotBlocks;
    this.deletedHotBlocks = deletedHotBlocks;
    this.votes = votes;
    this.stateRoots = stateRoots;
  }

  public boolean isEmpty() {
    return genesisTime.isEmpty()
        && justifiedCheckpoint.isEmpty()
        && finalizedChainData.isEmpty()
        && bestJustifiedCheckpoint.isEmpty()
        && hotBlocks.isEmpty()
        && deletedHotBlocks.isEmpty()
        && votes.isEmpty()
        && stateRoots.isEmpty();
  }

  public Optional<UnsignedLong> getGenesisTime() {
    return genesisTime;
  }

  public Optional<Checkpoint> getJustifiedCheckpoint() {
    return justifiedCheckpoint;
  }

  public Optional<Checkpoint> getFinalizedCheckpoint() {
    return finalizedChainData.map(FinalizedChainData::getFinalizedCheckpoint);
  }

  public Optional<Checkpoint> getBestJustifiedCheckpoint() {
    return bestJustifiedCheckpoint;
  }

  public Map<Bytes32, SignedBeaconBlock> getHotBlocks() {
    return hotBlocks;
  }

  public Set<Bytes32> getDeletedHotBlocks() {
    return deletedHotBlocks;
  }

  public Map<Bytes32, Bytes32> getFinalizedChildToParentMap() {
    return finalizedChainData
        .map(FinalizedChainData::getFinalizedChildToParentMap)
        .orElse(Collections.emptyMap());
  }

  public Map<Bytes32, SignedBeaconBlock> getFinalizedBlocks() {
    return finalizedChainData.map(FinalizedChainData::getBlocks).orElse(Collections.emptyMap());
  }

  public Map<Bytes32, BeaconState> getFinalizedStates() {
    return finalizedChainData.map(FinalizedChainData::getStates).orElse(Collections.emptyMap());
  }

  public Optional<BeaconState> getLatestFinalizedState() {
    return finalizedChainData.map(FinalizedChainData::getLatestFinalizedState);
  }

  public Map<UnsignedLong, VoteTracker> getVotes() {
    return votes;
  }

  public Map<Bytes32, SlotAndBlockRoot> getStateRoots() {
    return stateRoots;
  }
}
