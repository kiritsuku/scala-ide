package scala.tools.eclipse.ui.indentation

import scala.collection.mutable

trait PreferenceProvider {
  private val preferences = mutable.Map.empty[String, String]

  def updateCache(): Unit

  def put(key: String, value: String) {
    preferences(key) = value
  }

  def get(key: String): String = {
    preferences(key)
  }

  def getBoolean(key: String): Boolean = {
    get(key).toBoolean
  }

  def getInt(key: String): Int = {
    get(key).toInt
  }
}