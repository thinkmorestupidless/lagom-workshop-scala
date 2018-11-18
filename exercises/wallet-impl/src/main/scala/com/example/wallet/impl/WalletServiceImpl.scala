package com.example.wallet.impl

import com.example.wallet.api.WalletService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future

class WalletServiceImpl extends WalletService {

  override def balance(walletId: String) = ServiceCall { _ =>
    // TODO: Return the correct value so the tests pass.
    Future.successful("Fix me!")
  }
}
