import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "astrid-api",
    organization := "com.todoroo",
    version := "1.0-SNAPSHOT",
    versionCode := 1,
    scalaVersion := "2.9.2",
    crossPaths := false,
    platformName in Android := "android-14"
  )

  val proguardSettings = Seq (
    useProguard in Android := false
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    addArtifact(Artifact("astrid-api", "apklib", "apklib"),
                apklibPackage in Android).settings
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "astrid-api",
    file("."),
    settings = General.fullAndroidSettings
  )
}
