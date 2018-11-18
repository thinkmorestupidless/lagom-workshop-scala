// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.4.8")

// Needed for importing the project into Eclipse
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

// Enterprise Suite Stuff
addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.4.0")
addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.10.9")

credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials")
resolvers += Resolver.url("lightbend-commercial", url("https://repo.lightbend.com/commercial-releases"))(Resolver.ivyStylePatterns)