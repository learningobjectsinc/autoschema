// enablePlugins(DECommonSettings)

name := "autoschema"

organization := "com.learningobjects.org.coursera"

scalaVersion := "2.12.10"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.10",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.10",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.10" % "test",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "com.beachape" %% "enumeratum" % "1.5.13",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

scalacOptions -= "-Xfatal-warnings"

//artifactory configurations
publishTo := {
  if (isSnapshot.value)
    Some("LO Misc" at "https://learningobjects.jfrog.io/learningobjects/lo-misc")
  else
    Some(
      "LO Misc" at "https://learningobjects.jfrog.io/learningobjects/lo-misc;build.timestamp=" + new java.util.Date().getTime
    )
}

MimaPlugin.mimaDefaultSettings

mimaPreviousArtifacts := Set("com.learningobjects.org.coursera" %% "autoschema" % "0.3.0")
