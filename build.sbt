scalaVersion := "2.11.5"

resolvers ++= Seq(
"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
"Typesafe Snapshots repository" at "http://repo.typesafe.com/typesafe/snapshots/",
"Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
"Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
	"net.wimpi" % "jamod" % "1.2"
)
