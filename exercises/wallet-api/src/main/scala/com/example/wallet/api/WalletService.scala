package com.example.wallet.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait WalletService extends Service {

  def balance(walletId: String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._
    named("wallet")
      .withCalls(
        pathCall("/api/balance/:walletId", balance _)
    )
    .withAutoAcl(true)
  }
}