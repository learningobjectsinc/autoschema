import com.learningobjects.sbt.libraries.Scala

enablePlugins(DECommonSettings)

name := "autoschema"

organization := "com.learningobjects.org.coursera"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")

libraryDependencies ++= Seq(
  JSON.Jackson.core,
  JSON.Jackson.databind,
  JSON.Jackson.scala % "test",
  Scala.reflect(scalaVersion.value),
  Testing.scalaTest % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

scalacOptions -= "-Xfatal-warnings"

//artifactory configurations
publishTo := {
  if (isSnapshot.value)
    Some("LO Misc" at "https://learningobjects.artifactoryonline.com/learningobjects/lo-misc")
  else
    Some(
      "LO Misc" at "https://learningobjects.artifactoryonline.com/learningobjects/lo-misc;build.timestamp=" + new java.util.Date().getTime
    )
}