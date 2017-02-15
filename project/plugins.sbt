resolvers ++= Seq(
  Resolver.url("LO Repo", url("https://learningobjects.jfrog.io/learningobjects/repo"))(Resolver.ivyStylePatterns)
)

//https://stash.difference-engine.com/projects/DE/repos/sbt-de-commons/browse
addSbtPlugin("com.learningobjects.sbt" % "sbt-de-commons" % "1.8.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.12")
