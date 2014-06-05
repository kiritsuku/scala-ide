package org.scalaide.util.internal

/** Utility to unify how we convert settings to preference names */
object SettingConverterUtil {
  val UseProjectSettingsPreference = "scala.compiler.useProjectSettings"
  val ScalaDesiredSourceLevel = "scala.compiler.sourceLevel"

  /** Creates preference name from "name" of a compiler setting. */
  def convertNameToProperty(name : String) = {
    //Returns the underlying name without the -
    name.substring(1)
  }
}
