package com.example.wallet.impl

import com.example.wallet.api.WalletService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

trait WalletComponents extends LagomServerComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[WalletService](wire[WalletServiceImpl])
}

abstract class WalletApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with WalletComponents
    with AhcWSComponents {
}

class WalletLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new WalletApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new WalletApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[WalletService])
}
