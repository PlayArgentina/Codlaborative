import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "codlaborative"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "com.twitter" % "util-core" % "1.12.13",
      "com.twitter" % "util-eval" % "1.12.13"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
