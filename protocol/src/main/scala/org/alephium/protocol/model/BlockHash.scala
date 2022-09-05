// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.alephium.protocol.model

import akka.util.ByteString

import org.alephium.crypto.Blake3
import org.alephium.serde.{RandomBytes, Serde}

final case class BlockHash(value: Blake3) extends RandomBytes {
  def bytes: ByteString = value.bytes
}

object BlockHash {
  implicit val serde: Serde[BlockHash] = Serde.forProduct1(BlockHash.apply, t => t.value)

  val zero: BlockHash     = BlockHash(Blake3.zero)
  val length: Int         = Blake3.length
  def random: BlockHash   = BlockHash(Blake3.random)
  def generate: BlockHash = BlockHash(Blake3.generate)

  def from(bytes: ByteString): Option[BlockHash] = {
    Blake3.from(bytes).map(BlockHash.apply)
  }

  def hash(bytes: ByteString): BlockHash = {
    BlockHash(Blake3.hash(bytes))
  }

  def hash(string: String): BlockHash = {
    BlockHash(Blake3.hash(string))
  }

  def unsafe(str: ByteString): BlockHash = {
    BlockHash(Blake3.unsafe(str))
  }
}
