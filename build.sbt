name := "akka-streams-tutorial"
version := "1.0"
scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.8"
lazy val logBackVersion = "1.2.3"
lazy val scalaTestVersion = "3.1.0"

// format: off
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"         % akkaVersion,
  "ch.qos.logback"     % "logback-classic"     % logBackVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion        % Test,
  "com.typesafe.akka" %% "akka-testkit"        % akkaVersion        % Test,
  "org.scalatest"     %% "scalatest"           % scalaTestVersion   % Test
)
// format: on
