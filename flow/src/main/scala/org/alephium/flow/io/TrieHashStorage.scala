package org.alephium.flow.io

import akka.util.ByteString
import org.rocksdb.{ReadOptions, WriteOptions}

import org.alephium.flow.io.RocksDBSource.{ColumnFamily, Settings}
import org.alephium.flow.trie.{MerklePatriciaTrie, WorldState}
import org.alephium.protocol.ALF.Hash
import org.alephium.protocol.io.IOResult
import org.alephium.protocol.vm.WorldStateT

trait TrieHashStorage extends KeyValueStorage[Hash, WorldStateT.Hashes] {
  val trieStorage: KeyValueStorage[Hash, MerklePatriciaTrie.Node]

  override def storageKey(key: Hash): ByteString = key.bytes ++ ByteString(Storages.trieHashPostfix)

  def getTrie(hash: Hash): IOResult[WorldState] = {
    get(hash).map(WorldState.from(_, trieStorage))
  }

  def putTrie(hash: Hash, worldState: WorldState): IOResult[Unit] = {
    put(hash, worldState.toHashes)
  }
}

object TrieHashRockDBStorage {
  def apply(trieStorage: KeyValueStorage[Hash, MerklePatriciaTrie.Node],
            storage: RocksDBSource,
            cf: ColumnFamily,
            writeOptions: WriteOptions): TrieHashRockDBStorage = {
    new TrieHashRockDBStorage(trieStorage, storage, cf, writeOptions, Settings.readOptions)
  }

  def apply(trieStorage: KeyValueStorage[Hash, MerklePatriciaTrie.Node],
            storage: RocksDBSource,
            cf: ColumnFamily,
            writeOptions: WriteOptions,
            readOptions: ReadOptions): TrieHashRockDBStorage = {
    new TrieHashRockDBStorage(trieStorage, storage, cf, writeOptions, readOptions)
  }
}

class TrieHashRockDBStorage(
    val trieStorage: KeyValueStorage[Hash, MerklePatriciaTrie.Node],
    storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[Hash, WorldStateT.Hashes](storage, cf, writeOptions, readOptions)
    with TrieHashStorage
