name := "yotamanager"

version := "0.0.1"

scalaVersion := "2.11.8"

oneJarSettings

mainClass := Some("org.acef304.yotamanager.Manager")

libraryDependencies ++= Seq(
  //"ch.qos.logback" %  "logback-classic" % "1.1.6",
  "com.h2database" % "h2" % "1.4.187",
  "io.reactivex" %% "rxscala" % "0.26.1",
  "com.typesafe.slick" %% "slick" % "3.1.1"
)
    