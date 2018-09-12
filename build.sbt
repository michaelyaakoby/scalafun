import sbt.Keys.organization

name := "scalafun"

organization := "michaelyaakoby.github.io"

version in ThisBuild := "1.0.0"

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-infer-any",
//TODO-Scala-2.12, see https://github.com/scala/bug/issues/10394  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ypatmat-exhaust-depth","40"
)

fork := true
cancelable in Global := true
parallelExecution in Test := false

updateOptions := updateOptions.value.withCachedResolution(true)

val akkaActorVer = "2.5.16"

libraryDependencies ++= Seq(
  "com.github.pathikrit"       %% "better-files"               % "3.6.0",
  "com.google.guava"           % "guava"                       % "23.0",
  "com.google.code.findbugs"   % "jsr305"                      % "3.0.2" % "compile",
  "com.bluejeans.common"       % "bigqueue"                    % "1.7.0.5",
  "com.typesafe.akka"          %% "akka-stream"                % akkaActorVer,
  "com.typesafe.akka"          %% "akka-actor"                 % akkaActorVer,
  "com.typesafe.akka"          %% "akka-slf4j"                 % akkaActorVer,
  "com.typesafe.scala-logging" %% "scala-logging"              % "3.9.0",
  "ch.qos.logback"             % "logback-classic"             % "1.2.3",
  "org.scalatest"              %% "scalatest"                  % "3.0.5" % "test"
)

