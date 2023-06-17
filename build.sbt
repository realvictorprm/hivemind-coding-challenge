import com.typesafe.sbt.packager.docker.DockerApiVersion

enablePlugins(JavaAppPackaging, AshScriptPlugin)

val projectName = "hivemind-coding-challenge"
val scala3Version = "2.13.11"
val elastic4sVersion = "8.7.0"
val slf4jVersion = "2.0.5"

val IntegrationTesting = config("it").extend(Test)

lazy val root = project
  .in(file("."))
  .configs(IntegrationTesting)
  .settings(
    inConfig(IntegrationTesting) {
      Defaults.testSettings
    },
    name := projectName,
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.15",
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-streams" % "2.0.15",
      "dev.zio" %% "zio-json" % "0.5.0",
      "com.github.pureconfig" %% "pureconfig" % "0.17.4",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-effect-zio" % elastic4sVersion,
      "org.slf4j" % "slf4j-api" % slf4jVersion,
      "org.slf4j" % "slf4j-log4j12" % slf4jVersion,
      "log4j" % "log4j" % "1.2.17",
      "dev.zio" %% "zio-test" % "2.0.15" % "it",
      "dev.zio" %% "zio-test-sbt" % "2.0.15" % "it",
      "dev.zio" %% "zio-test-magnolia" % "2.0.15" % "it"
    ),
    scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:locals",
      "-Wunused:params"
    )
  )

libraryDependencies ++= Seq(
)
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

Docker / packageName := projectName
Docker / version := "0.0.1"
dockerBaseImage := "eclipse-temurin:11.0.19_7-jre-jammy"
dockerUpdateLatest := true
dockerExposedPorts ++= Seq(8080)
dockerEntrypoint := Seq(
  s"/opt/docker/bin/$projectName",
  "$INPUT_FILE_URL"
)
