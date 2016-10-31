// Comment to get more information during initialization
logLevel := Level.Warn

//scalaVersion := "2.11.5"

resolvers ++= Seq(
"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
"Typesafe Snapshots repository" at "http://repo.typesafe.com/typesafe/snapshots/",
"Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
"Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")
