name := "playground"
organization in ThisBuild := "edu.uic"
scalaVersion in ThisBuild := "2.12.12"

// PROJECTS

lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(CodegenPlugin)
  .aggregate(
    common,
    example
  )

lazy val common = project
  .in(file("common"))
  .settings(
    name := "playground-common",
    settings,
    assemblySettings,
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.scalaReflect)
  )

lazy val example = project
  .in(file("example"))
  .settings(
    name := "playground-example",
    settings,
    assemblySettings,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(common)

// DEPENDENCIES

lazy val dependencies =
  new {
    // format: off
    val scalaReflect    = "org.scala-lang"                 % "scala-compiler"                  % "2.12.12"
    val macroParadise   = "org.scalamacros"                % "paradise"                        % "2.1.1"
    val calibanClient   = "com.github.ghostdogpr"         %% "caliban-client"                  % "0.9.2"
    val zioBackend      = "com.softwaremill.sttp.client"  %% "async-http-client-backend-zio"   % "2.2.9"

    val typesafeConfig  = "com.typesafe"                   % "config"                          % "1.4.0"

    val logbackClassic  = "ch.qos.logback"                 % "logback-classic"                 % "1.2.3"   % "runtime"

    val scalaTest       = "org.scalatest"                 %% "scalatest"                       % "3.2.2"   % "test"
    // format: on
  }

lazy val commonDependencies = Seq(
  dependencies.typesafeConfig,
  dependencies.logbackClassic,
  dependencies.scalaTest,
  dependencies.calibanClient,
  dependencies.zioBackend
)

// SETTINGS

lazy val settings =
  commonSettings //++ wartremoverSettings ++ scalafmtSettings

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  addCompilerPlugin(dependencies.macroParadise cross CrossVersion.full)
)

//lazy val wartremoverSettings = Seq(
//  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.Throw),
//  wartremoverExcluded ++= Seq(baseDirectory.value / "common" / "src" / "main" / "clients" / "Github.scala")
//)
//
//lazy val scalafmtSettings = Seq(
//  scalafmtOnCompile := true,
//  scalafmtTestOnCompile := true
//)

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case "application.conf"            => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
