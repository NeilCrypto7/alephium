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

package org.alephium.wallet.api

import sttp.tapir.EndpointIO.Example

import org.alephium.api.ErrorExamples
import org.alephium.api.model.{Destination, Token}
import org.alephium.crypto.wallet.Mnemonic
import org.alephium.protocol.Hash
import org.alephium.protocol.model.Address
import org.alephium.protocol.vm.LockupScript
import org.alephium.util.{AVector, Hex, U256}
import org.alephium.wallet.api.model._

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
trait WalletExamples extends ErrorExamples {

  private val lockupScript =
    LockupScript.asset("1AujpupFP4KWeZvqA7itsHY9cLJmx4qTzojVZrg8W9y9n").get
  private val address  = Address.Asset(lockupScript)
  private val password = "my-secret-password"
  private val mnemonic =
    Mnemonic
      .from(
        "vault alarm sad mass witness property virus style good flower rice alpha viable evidence run glare pretty scout evil judge enroll refuse another lava"
      )
      .get
  private val txId =
    Hash.from(Hex.from("503bfb16230888af4924aa8f8250d7d348b862e267d75d3147f1998050b6da69").get).get

  private val walletName         = "wallet-super-name"
  private val mnemonicPassphrase = "optional-mnemonic-passphrase"
  private val fromGroup          = 2
  private val toGroup            = 1
  // scalastyle:off magic.number
  private val tokens = AVector(
    Token(Hash.hash("token1"), U256.unsafe(42)),
    Token(Hash.hash("token2"), U256.unsafe(1000))
  )

  val mnemonicSizes: String = Mnemonic.Size.list.toSeq.map(_.value).mkString(", ")

  implicit val walletCreationExamples: List[Example[WalletCreation]] = List(
    defaultExample(WalletCreation(password, None, None, None, None)),
    moreSettingsExample(
      WalletCreation(
        password,
        Some(walletName),
        Some(true),
        Some(mnemonicPassphrase),
        Some(Mnemonic.Size.list.head)
      )
    )
  )
  implicit val walletCreationResultExamples: List[Example[WalletCreation.Result]] =
    simpleExample(WalletCreation.Result(walletName, mnemonic))

  implicit val walletRestoreExamples: List[Example[WalletRestore]] =
    List(
      defaultExample(
        WalletRestore(password, mnemonic, None, None, None)
      ),
      moreSettingsExample(
        WalletRestore(password, mnemonic, Some(true), Some(walletName), Some(mnemonicPassphrase))
      )
    )

  implicit val walletRestoreResultExamples: List[Example[WalletRestore.Result]] =
    simpleExample(WalletRestore.Result(walletName))

  implicit val walletStatusExamples: List[Example[WalletStatus]] =
    simpleExample(WalletStatus(walletName, locked = true))

  implicit val walletsStatusExamples: List[Example[AVector[WalletStatus]]] =
    simpleExample(AVector(WalletStatus(walletName, locked = true)))

  implicit val walletUnlockExamples: List[Example[WalletUnlock]] =
    List(
      defaultExample(WalletUnlock(password, None)),
      moreSettingsExample(WalletUnlock(password, Some(mnemonicPassphrase)))
    )

  implicit val walletDeletionExamples: List[Example[WalletDeletion]] =
    simpleExample(WalletDeletion(password))

  implicit val balancesExamples: List[Example[Balances]] =
    simpleExample(Balances(U256.Million, AVector(Balances.AddressBalance(address, U256.Million))))

  implicit val revealMnemonicExamples: List[Example[RevealMnemonic]] =
    simpleExample(RevealMnemonic(password))

  implicit val revealMnemonicResultExamples: List[Example[RevealMnemonic.Result]] =
    simpleExample(RevealMnemonic.Result(mnemonic))

  implicit val transferExamples: List[Example[Transfer]] =
    simpleExample(Transfer(AVector(Destination(address, U256.Million, Some(tokens)))))

  implicit val sweepAllExamples: List[Example[SweepAll]] =
    simpleExample(SweepAll(address))

  implicit val transferResultExamples: List[Example[Transfer.Result]] =
    simpleExample(Transfer.Result(txId, fromGroup, toGroup))

  implicit val addressesExamples: List[Example[Addresses]] =
    simpleExample(Addresses(address, AVector(address)))

  implicit val addressExamples: List[Example[Address]] =
    simpleExample(address)

  implicit val addressInfoExamples: List[Example[AddressInfo]] =
    simpleExample(AddressInfo(address, fromGroup))

  implicit val minerAddressesInfoExample: List[Example[AVector[MinerAddressesInfo]]] =
    simpleExample(AVector(MinerAddressesInfo(AVector(AddressInfo(address, fromGroup)))))

  implicit val addressessInfoExamples: List[Example[AVector[AddressInfo]]] =
    simpleExample(AVector(AddressInfo(address, fromGroup)))

  implicit val changeActiveAddressExamples: List[Example[ChangeActiveAddress]] =
    simpleExample(ChangeActiveAddress(address))

  implicit val deriveNextAddressResultExamples: List[Example[DeriveNextAddress.Result]] =
    simpleExample(DeriveNextAddress.Result(address))
}
