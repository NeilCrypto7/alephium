package org.alephium.appserver

import java.net.InetAddress

import io.circe._
import io.circe.generic.semiauto._

import org.alephium.flow.core.FlowHandler.BlockNotify
import org.alephium.flow.network.bootstrap.IntraCliqueInfo
import org.alephium.protocol.config.GroupConfig
import org.alephium.protocol.model.{BlockHeader, CliqueId, CliqueInfo}
import org.alephium.rpc.CirceUtils._
import org.alephium.util.{AVector, Hex, TimeStamp}

sealed trait RPCModel

object RPCModel {
  object TimeStampCodec {
    implicit val decoderTS: Decoder[TimeStamp] =
      Decoder.decodeLong.ensure(_ >= 0, s"expect positive timestamp").map(TimeStamp.unsafe)
    implicit val encoderTS: Encoder[TimeStamp] = Encoder.encodeLong.contramap(_.millis)
  }

  final case class FetchRequest(fromTs: TimeStamp, toTs: TimeStamp) extends RPCModel
  object FetchRequest {
    import TimeStampCodec._
    def decoder(implicit rpcConfig: RPCConfig): Decoder[FetchRequest] =
      deriveDecoder[FetchRequest]
        .ensure(
          fetchRequest => fetchRequest.fromTs <= fetchRequest.toTs,
          "`toTs` cannot be before `fromTs`"
        )
        .ensure(
          fetchRequest =>
            (fetchRequest.toTs -- fetchRequest.fromTs)
              .exists(_ <= rpcConfig.blockflowFetchMaxAge),
          s"interval cannot be greater than ${rpcConfig.blockflowFetchMaxAge}"
        )
    implicit val encoder: Encoder[FetchRequest] = deriveEncoder[FetchRequest]
    def codec(implicit rpcConfig: RPCConfig): Codec[FetchRequest] =
      Codec.from(decoder, encoder)
  }

  final case class FetchResponse(blocks: Seq[BlockEntry]) extends RPCModel
  object FetchResponse {
    implicit val codec: Codec[FetchResponse] = deriveCodec[FetchResponse]
  }

  final case class BlockEntry(
      hash: String,
      timestamp: TimeStamp,
      chainFrom: Int,
      chainTo: Int,
      height: Int,
      deps: AVector[String]
  ) extends RPCModel
  object BlockEntry {
    import TimeStampCodec._
    implicit val codec: Codec[BlockEntry] = deriveCodec[BlockEntry]

    def from(header: BlockHeader, height: Int)(implicit config: GroupConfig): BlockEntry = {
      BlockEntry(
        hash      = header.shortHex,
        timestamp = header.timestamp,
        chainFrom = header.chainIndex.from.value,
        chainTo   = header.chainIndex.to.value,
        height    = height,
        deps      = header.blockDeps.map(_.shortHex)
      )
    }

    def from(blockNotify: BlockNotify)(implicit config: GroupConfig): BlockEntry = {
      from(blockNotify.header, blockNotify.height)
    }
  }

  final case class PeerAddress(address: InetAddress, rpcPort: Option[Int], wsPort: Option[Int])
  object PeerAddress {
    implicit val codec: Codec[PeerAddress] = deriveCodec[PeerAddress]
  }

  final case class SelfClique(peers: AVector[PeerAddress], groupNumPerBroker: Int) extends RPCModel
  object SelfClique {
    def from(cliqueInfo: IntraCliqueInfo): SelfClique = {
      SelfClique(cliqueInfo.peers.map(peer => PeerAddress(peer.address, peer.rpcPort, peer.wsPort)),
                 cliqueInfo.groupNumPerBroker)
    }

    implicit val codec: Codec[SelfClique] = deriveCodec[SelfClique]
  }

  final case class NeighborCliques(cliques: AVector[CliqueInfo]) extends RPCModel
  object NeighborCliques {
    def createId(s: String): Either[String, CliqueId] = {
      Hex.from(s).flatMap(CliqueId.from) match {
        case Some(id) => Right(id)
        case None     => Left("invalid clique id")
      }
    }

    implicit val idEncoder: Encoder[CliqueId] = Encoder.encodeString.contramap(_.toHexString)
    implicit val idDecoder: Decoder[CliqueId] = Decoder.decodeString.emap(createId)
    implicit val cliqueEncoder: Encoder[CliqueInfo] =
      Encoder.forProduct3("id", "peers", "groupNumPerBroker")(info =>
        (info.id, info.peers, info.groupNumPerBroker))
    implicit val cliqueDecoder: Decoder[CliqueInfo] =
      Decoder.forProduct3("id", "peers", "groupNumPerBroker")(CliqueInfo.unsafe)
    implicit val codec: Codec[NeighborCliques] = deriveCodec[NeighborCliques]
  }

  final case class GetBalance(address: String, `type`: String) extends RPCModel
  object GetBalance {
    implicit val codec: Codec[GetBalance] = deriveCodec[GetBalance]

    // TODO: refactor this once script system gets mature
    val pkh: String = "pkh"
  }

  final case class GetGroup(address: String) extends RPCModel
  object GetGroup {
    implicit val codec: Codec[GetGroup] = deriveCodec[GetGroup]
  }

  final case class Balance(balance: BigInt, utxoNum: Int) extends RPCModel
  object Balance {
    implicit val codec: Codec[Balance] = deriveCodec[Balance]
  }

  final case class Group(group: Int) extends RPCModel
  object Group {
    implicit val codec: Codec[Group] = deriveCodec[Group]
  }

  final case class Transfer(fromAddress: String,
                            fromType: String,
                            toAddress: String,
                            toType: String,
                            value: BigInt,
                            fromPrivateKey: String)
      extends RPCModel
  object Transfer {
    implicit val codec: Codec[Transfer] = deriveCodec[Transfer]
  }

  final case class TransferResult(txId: String) extends RPCModel
  object TransferResult {
    implicit val codec: Codec[TransferResult] = deriveCodec[TransferResult]
  }
}
