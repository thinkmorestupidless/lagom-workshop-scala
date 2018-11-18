organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val mockito = "org.mockito" % "mockito-core" % "2.22.0" % Test

val step = "00"

def project(id: String) = Project(s"${id}_${step}", base = file(id))
  .settings(javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"))

lazy val `wallet-api` = project("wallet-api")
  .settings(common: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `wallet-impl` = project("wallet-impl")
  .enablePlugins(LagomScala, Cinnamon, SbtReactiveAppPlugin)
  .settings(common: _*)
  .settings(sharedSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      Cinnamon.library.cinnamonLagom,
      Cinnamon.library.cinnamonJvmMetricsProducer,
      Cinnamon.library.cinnamonPrometheusHttpServer,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`wallet-api`)


def common = Seq(
  javacOptions in compile += "-parameters"
)

aggregateProjects(`wallet-api`, `wallet-impl`)

lazy val sharedSettings = Seq(

  // Enable Cinnamon during tests
  cinnamon in test := true,

  // Enable Cinnamon at runtime
  cinnamon in run := true,

  // Set the Cinnamon Agent log level
  cinnamonLogLevel := "INFO",

  // Add a play secret to javaOptions in run in Test, so we can run Lagom forked
  javaOptions in (Test, run) += "-Dplay.http.secret.key=x",

  annotations := Map(
    // enable scraping
    "prometheus.io/scrape" -> "true",
    // set scheme - defaults to "http"
    "prometheus.io/scheme" -> "http",
    // set path - defaults to "/metrics"
    "prometheus.io/path" -> "/metrics",
    // set port - defaults to "9001"
    "prometheus.io/port" -> "9001"
  ),

  // declare container ports that should be exposed
  endpoints ++= List(
    TcpEndpoint("app", 8080, PortIngress(8080)),
    TcpEndpoint("cinnamon", 9001, None)
  ),

  // cluster bootstrap deployment settings
  enableAkkaClusterBootstrap := true,
  deployMinikubeAkkaClusterBootstrapContactPoints := 2
)