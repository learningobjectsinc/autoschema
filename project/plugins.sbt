resolvers ++= Seq(
  Resolver.url("LO Repo", url("https://learningobjects.jfrog.io/learningobjects/repo"))(Resolver.ivyStylePatterns)
)


addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1")
