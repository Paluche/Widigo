import android.Keys._

android.Plugin.androidBuild

name := "Widigo"

scalaVersion := "2.11.0"

proguardCache in Android ++= Seq(
  ProguardCache("org.scaloid") % "org.scaloid"
)

proguardOptions in Android ++= Seq("-dontobfuscate", "-dontoptimize", "-ignorewarnings", "-keep class scala.Dynamic")

resolvers += "JCenter" at "http://jcenter.bintray.com"

libraryDependencies += "org.macroid" %% "macroid" % "2.0.0-M1"

scalacOptions in Compile ++= Seq("-feature",
  "-P:wartremover:cp:" + (dependencyClasspath in Compile).value.files.map(_.toURL.toString).find(_.contains("org.macroid/macroid_")).get,
  "-P:wartremover:traverser:macroid.warts.CheckUi")

addCompilerPlugin("org.brianmckenna" %% "wartremover" % "0.10")

run <<= run in Android

install <<= install in Android
