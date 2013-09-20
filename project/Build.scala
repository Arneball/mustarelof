import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "maps"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
    libraryDependencies ++= Seq("net.sourceforge.jexcelapi" % "jxl" % "2.6.12", 
      "org.reactivemongo" % "reactivemongo_2.10" % "0.9",
      "org.apache.poi" % "poi-ooxml" % "3.9",
      "com.google.code.geocoder-java" % "geocoder-java" % "0.15"),
    EclipseKeys.withSource := true
  )

}
