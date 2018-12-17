package org.alephium.flow.storage

import org.alephium.flow.PlatformConfig
import org.alephium.protocol.model.{Block, ModelGen}
import org.alephium.util.{AVector, AlephiumSpec}

class BlockChainSpec extends AlephiumSpec with PlatformConfig.Default {

  behavior of "BlockChain"

  trait Fixture {
    val genesis  = Block.genesis(AVector.empty, config.maxMiningTarget, 0)
    val chain    = BlockChain.fromGenesis(genesis)
    val blockGen = ModelGen.blockGenWith(AVector.fill(config.depsNum)(genesis.hash))
    val chainGen = ModelGen.chainGen(4, genesis)
  }

  it should "add block correctly" in new Fixture {
    chain.numHashes is 1
    forAll(blockGen, minSuccessful(1)) { block =>
      val blocksSize1 = chain.numHashes
      val txSize1     = chain.numTransactions
      chain.add(block, 0)
      val blocksSize2 = chain.numHashes
      val txSize2     = chain.numTransactions
      blocksSize1 + 1 is blocksSize2
      txSize1 + block.transactions.length is txSize2
    }
  }

  it should "add blocks correctly" in new Fixture {
    forAll(chainGen, minSuccessful(1)) { blocks =>
      val blocksSize1 = chain.numHashes
      val txSize1     = chain.numTransactions
      blocks.foreach(block => chain.add(block, 0))
      val blocksSize2 = chain.numHashes
      val txSize2     = chain.numTransactions
      blocksSize1 + blocks.length is blocksSize2
      txSize1 + blocks.sumBy(_.transactions.length) is txSize2

      checkConfirmedBlocks(chain, blocks)
    }
  }

  it should "work correctly for a chain of blocks" in new Fixture {
    forAll(ModelGen.chainGen(4, genesis), minSuccessful(1)) { blocks =>
      val chain0 = BlockChain(genesis, 0, 0)
      blocks.foreach(block => chain0.add(block, 0))
      val headBlock = genesis
      val lastBlock = blocks.last
      val chain     = AVector(genesis) ++ blocks

      chain0.getHeight(headBlock) is 0
      chain0.getHeight(lastBlock) is blocks.length
      chain0.getBlockSlice(headBlock) is AVector(headBlock)
      chain0.getBlockSlice(lastBlock) is chain
      chain0.isTip(headBlock) is false
      chain0.isTip(lastBlock) is true
      chain0.getBestTip is lastBlock.hash
      chain0.getBestBlockChain is chain
      chain0.maxHeight is blocks.length
      chain0.getAllTips is AVector(lastBlock.hash)
      checkConfirmedBlocks(chain0, blocks)
    }
  }

  it should "work correctly with two chains of blocks" in new Fixture {
    forAll(ModelGen.chainGen(4, genesis), minSuccessful(1)) { longChain =>
      forAll(ModelGen.chainGen(2, genesis), minSuccessful(1)) { shortChain =>
        val chain0 = BlockChain(genesis, 0, 0)

        shortChain.foreach(block => chain0.add(block, 0))
        chain0.getHeight(shortChain.head) is 1
        chain0.getHeight(shortChain.last) is shortChain.length
        chain0.getBlockSlice(shortChain.head) is AVector(genesis, shortChain.head)
        chain0.getBlockSlice(shortChain.last) is AVector(genesis) ++ shortChain
        chain0.isTip(shortChain.head) is false
        chain0.isTip(shortChain.last) is true

        longChain.init.foreach(block => chain0.add(block, 0))
        chain0.maxHeight is longChain.length - 1
        chain0.getAllTips.toIterable.toSet is Set(longChain.init.last.hash, shortChain.last.hash)

        chain0.add(longChain.last, 0)
        chain0.getHeight(longChain.head) is 1
        chain0.getHeight(longChain.last) is longChain.length
        chain0.getBlockSlice(longChain.head) is AVector(genesis, longChain.head)
        chain0.getBlockSlice(longChain.last) is AVector(genesis) ++ longChain
        chain0.isTip(longChain.head) is false
        chain0.isTip(longChain.last) is true
        chain0.getBestTip is longChain.last.hash
        chain0.getBestBlockChain is AVector(genesis) ++ longChain
        chain0.maxHeight is longChain.length
        chain0.getAllTips.toIterable.toSet is Set(longChain.last.hash)
      }
    }
  }

  it should "compute correct weights for a single chain" in {
    forAll(ModelGen.chainGen(5), minSuccessful(1)) { blocks =>
      val chain = createBlockChain(blocks.init)
      blocks.init.foreach(block => chain.contains(block) is true)
      chain.maxHeight is 3
      chain.maxWeight is 6
      chain.add(blocks.last, 8)
      chain.contains(blocks.last) is true
      chain.maxHeight is 4
      chain.maxWeight is 8
    }
  }

  it should "compute corrent weights for two chains with same root" in {
    forAll(ModelGen.chainGen(5), minSuccessful(1)) { blocks1 =>
      forAll(ModelGen.chainGen(1, blocks1.head), minSuccessful(1)) { blocks2 =>
        val chain = createBlockChain(blocks1)
        blocks2.foreach(block => chain.add(block, 0))
        blocks1.foreach(block => chain.contains(block) is true)
        blocks2.foreach(block => chain.contains(block) is true)
        chain.maxHeight is 4
        chain.maxWeight is 8
        chain.getBlocks(blocks1.head.hash).length is 5
        chain.getBlocks(blocks2.head.hash).length is 0
        chain.getBlocks(blocks1.tail.head.hash).length is 3
      }
    }
  }

  def checkConfirmedBlocks(chain: BlockChain, newBlocks: AVector[Block]): Unit = {
    newBlocks.indices.foreach { index =>
      val height   = index + 1
      val blockOpt = chain.getConfirmedBlock(height)
      if (height + config.blockConfirmNum <= newBlocks.length) {
        blockOpt.get is newBlocks(index)
      } else {
        blockOpt.isEmpty
      }
    }
    chain.getConfirmedBlock(-1).isEmpty
    ()
  }

  def createBlockChain(blocks: AVector[Block]): BlockChain = {
    assert(blocks.nonEmpty)
    val chain = BlockChain(blocks.head, 0, 0)
    blocks.toIterable.zipWithIndex.tail foreach {
      case (block, index) =>
        chain.add(block, index * 2)
    }
    chain
  }
}