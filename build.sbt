name := "playground"
organization in ThisBuild := "edu.uic"
scalaVersion in ThisBuild := "2.13.5"

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
    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.scalaReflect,
      dependencies.scalaGraphCore
    )
  )

lazy val example = project
  .in(file("example"))
  .settings(
    name := "playground-example",
    settings,
    assemblySettings,
    libraryDependencies ++= commonDependencies ++ Seq()
  )
  .dependsOn(common)

// DEPENDENCIES

lazy val dependencies =
  new {
    val scalaReflect   = "org.scala-lang"                  % "scala-compiler"                    % "2.13.5"
    val scalaGraphCore = "org.scala-graph"                %% "graph-core"                       % "1.13.2"
    val calibanClient  = "com.github.ghostdogpr"          %% "caliban-client"                   % "0.9.4"
    val zioBackend     = "com.softwaremill.sttp.client"   %% "async-http-client-backend-zio"    % "2.2.9"
    val mongodbDriver  = "org.mongodb.scala"              %% "mongo-scala-driver"               % "4.2.3"
    val typesafeConfig = "com.typesafe"                    % "config"                           % "1.4.1"
    val logbackClassic = "ch.qos.logback"                  % "logback-classic"                  % "1.2.3"   % "runtime"
    val scalaTest      = "org.scalatest"                  %% "scalatest"                        % "3.2.3"   % "test"
  }

lazy val commonDependencies = Seq(
  dependencies.typesafeConfig,
  dependencies.logbackClassic,
  dependencies.scalaTest,
  dependencies.calibanClient,
  dependencies.zioBackend,
  dependencies.mongodbDriver
)

// SETTINGS

lazy val settings =
  commonSettings //++ wartremoverSettings ++ scalafmtSettings

lazy val compilerOptions = Seq(
  // format: off
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Ymacro-annotations",
  // format: on
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

//lazy val wartremoverSettings = Seq(
//  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.Throw),
//  wartremoverExcluded ++= Seq(
//    sourceManaged.value,
//    baseDirectory.value / "common" / "src" / "main" / "scala" / "com" / "Github.scala"
//  )
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
