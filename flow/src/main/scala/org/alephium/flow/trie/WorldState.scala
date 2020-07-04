package org.alephium.flow.trie

import org.alephium.flow.io.KeyValueStorage
import org.alephium.protocol.ALF
import org.alephium.protocol.io.IOResult
import org.alephium.protocol.model._
import org.alephium.protocol.vm.{StatelessScript, WorldStateT}
import org.alephium.util.U64

final case class WorldState(outputState: MerklePatriciaTrie[TxOutputRef, TxOutput],
                            contractState: MerklePatriciaTrie[ALF.Hash, StatelessScript])
    extends WorldStateT {

  def get(outputRef: TxOutputRef): IOResult[TxOutput] = {
    outputState.get(outputRef)
  }

  def put(outputRef: TxOutputRef, output: TxOutput): IOResult[WorldState] = {
    outputState.put(outputRef, output).map(WorldState(_, contractState))
  }

  def put(key: ALF.Hash, contract: StatelessScript): IOResult[WorldState] = {
    contractState.put(key, contract).map(WorldState(outputState, _))
  }

  def remove(outputRef: TxOutputRef): IOResult[WorldState] = {
    outputState.remove(outputRef).map(WorldState(_, contractState))
  }

  def remove(key: ALF.Hash): IOResult[WorldState] = {
    contractState.remove(key).map(WorldState(outputState, _))
  }

  def toHashes: WorldStateT.Hashes =
    WorldStateT.Hashes(outputState.rootHash, contractState.rootHash)
}

object WorldState {
  def empty(storage: KeyValueStorage[ALF.Hash, MerklePatriciaTrie.Node]): WorldState = {
    val emptyOutputTrie =
      MerklePatriciaTrie.build(storage, TxOutputRef.empty, TxOutput.burn(U64.Zero))
    val emptyContractTrie =
      MerklePatriciaTrie.build(storage, ALF.Hash.zero, StatelessScript.failure)
    WorldState(emptyOutputTrie, emptyContractTrie)
  }

  def from(hashes: WorldStateT.Hashes,
           storage: KeyValueStorage[ALF.Hash, MerklePatriciaTrie.Node]): WorldState = {
    val outputState = MerklePatriciaTrie[TxOutputRef, TxOutput](hashes.outputStateHash, storage)
    val contractState =
      MerklePatriciaTrie[ALF.Hash, StatelessScript](hashes.contractStateHash, storage)
    WorldState(outputState, contractState)
  }
}
