package org.alephium.flow.storage

import org.alephium.crypto.Keccak256
import org.alephium.flow.PlatformConfig
import org.alephium.protocol.model.{Block, BlockHeader}
import org.alephium.util.AVector

import scala.collection.mutable.HashMap

trait BlockHeaderChain extends BlockHeaderPool with BlockHashChain {
  protected val blockHeadersTable: HashMap[Keccak256, BlockHeader] = HashMap.empty

  def getBlockHeader(hash: Keccak256): BlockHeader = {
    blockHeadersTable(hash)
  }

  def add(blockHeader: BlockHeader, weight: Int): Unit = {
    add(blockHeader, blockHeader.parentHash, weight)
  }

  def add(header: BlockHeader, parentHash: Keccak256, weight: Int): Unit = {
    assert(!contains(header.hash) && contains(parentHash))
    val parent = blockHashesTable(parentHash)
    addHash(header.hash, parent, weight)
    addHeader(header)
  }

  def getHeadersAfter(locator: Keccak256): AVector[BlockHeader] =
    getHashesAfter(locator).map(getBlockHeader)

  protected def addHeader(header: BlockHeader): Unit = {
    blockHeadersTable += header.hash -> header
  }

  def getConfirmedHeader(height: Int): Option[BlockHeader] = {
    getConfirmedHash(height).map(getBlockHeader)
  }

  def getHashTarget(hash: Keccak256): BigInt = {
    val header    = getBlockHeader(hash)
    val height    = getHeight(hash)
    val refHeight = height - config.retargetInterval
    getConfirmedHeader(refHeight) match {
      case Some(refHeader) =>
        val timeSpan = header.timestamp - refHeader.timestamp
        val retarget = header.target * config.retargetInterval * config.blockTargetTime.toMillis / timeSpan
        retarget
      case None => config.maxMiningTarget
    }
  }
}

object BlockHeaderChain {
  def fromGenesis(genesis: Block)(implicit config: PlatformConfig): BlockHeaderChain =
    apply(genesis.header, 0, 0)

  def apply(rootHeader: BlockHeader, initialHeight: Int, initialWeight: Int)(
      implicit _config: PlatformConfig): BlockHeaderChain = {
    val rootNode = BlockHashChain.Root(rootHeader.hash, initialHeight, initialWeight)

    new BlockHeaderChain {
      override implicit def config: PlatformConfig     = _config
      override protected def root: BlockHashChain.Root = rootNode

      this.addNode(rootNode)
      this.addHeader(rootHeader)
    }
  }
}
