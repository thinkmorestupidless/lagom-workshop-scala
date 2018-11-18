package com.example.wallet.impl

import com.example.wallet.api.WalletService
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

class WalletServiceSpec extends AsyncWordSpec
  with Matchers
  with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new WalletApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[WalletService]

  override protected def afterAll() = server.stop()

  "wallet service" should {

    "say check balance" in {
      client.balance("Alice").invoke().map { answer =>
        answer should === ("Where's Alice's money?")
      }
    }
  }
}
