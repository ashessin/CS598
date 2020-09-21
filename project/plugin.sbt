logLevel := Level.Warn

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")

//addSbtPlugin("org.scalastyle"  %% "scalastyle-sbt-plugin" % "1.0.0")
//addSbtPlugin("org.wartremover" % "sbt-wartremover"        % "2.4.10")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.15")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "0.9.2")
