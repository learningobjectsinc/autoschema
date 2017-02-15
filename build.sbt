import com.learningobjects.sbt.libraries.Scala

enablePlugins(DECommonSettings)

name := "autoschema"

organization := "com.learningobjects.org.coursera"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")

libraryDependencies ++= Seq(
  JSON.Jackson.core,
  JSON.Jackson.databind,
  JSON.Jackson.scala % "test",
  Scala.reflect(scalaVersion.value),
  ScalaExtensions.enumeratum,
  Testing.scalaTest % "test",
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