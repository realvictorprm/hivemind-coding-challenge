val scala3Version = "3.3.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "hivemind-coding-challenge",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "dev.zio" %% "zio" % "2.0.15",
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-streams" % "2.0.15",
      "dev.zio" %% "zio-json" % "0.5.0",
      "com.github.pureconfig" %% "pureconfig" % "0.17.4" cross CrossVersion.for3Use2_13,
      "io.lambdaworks" %% "zio-elasticsearch" % "0.1.0",

    )
  )
