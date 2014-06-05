package org.scalaide.logging

import org.eclipse.jface.util.PropertyChangeEvent
import org.scalaide.core.ScalaPlugin
import org.scalaide.logging.log4j.Log4JFacade
import org.scalaide.logging.ui.preferences.LoggingPreferenceConstants._
import org.scalaide.util.internal.eclipse.SWTUtils

object LogManager extends Log4JFacade with HasLogger {

  private def updateLogLevel(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == LogLevel) {
      val level = event.getNewValue.asInstanceOf[String]
      setLogLevel(Level.withName(level))
    }
  }

  private def updateConsoleAppenderStatus(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == IsConsoleAppenderEnabled) {
      val enable = event.getNewValue.asInstanceOf[Boolean]
      withoutConsoleRedirects {
        updateConsoleAppender(enable)
      }
    }
  }

  private def updateStdRedirectStatus(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == RedirectStdErrOut) {
      val enable = event.getNewValue.asInstanceOf[Boolean]
      if (enable) redirectStdOutAndStdErr()
      else disableRedirectStdOutAndStdErr()

      // we need to restart the presentation compilers so that
      // the std out/err streams are refreshed by Console.in/out
      if (enable != event.getOldValue.asInstanceOf[Boolean])
        ScalaPlugin.plugin.resetAllPresentationCompilers()
    }
  }

  override protected def logFileName = "scala-ide.log"

  override def configure(logOutputLocation: String, preferredLogLevel: Level.Value) {
    import SWTUtils.fnToPropertyChangeListener

    super.configure(logOutputLocation, preferredLogLevel)

    val prefStore = ScalaPlugin.plugin.getPreferenceStore
    prefStore.addPropertyChangeListener(updateLogLevel _)
    prefStore.addPropertyChangeListener(updateConsoleAppenderStatus _)
    prefStore.addPropertyChangeListener(updateStdRedirectStatus _)

    if (prefStore.getBoolean(RedirectStdErrOut)) {
      redirectStdOutAndStdErr()
      ScalaPlugin.plugin.resetAllPresentationCompilers()
    }
  }

  override protected def setLogLevel(level: Level.Value) {
    super.setLogLevel(level)
    logger.info("Log level is `%s`".format(level))
  }

  override def currentLogLevel: Level.Value = {
    val levelName = ScalaPlugin.plugin.getPreferenceStore.getString(LogLevel)
    if (levelName.isEmpty) defaultLogLevel
    else Level.withName(levelName)
  }

  private[logging] def defaultLogLevel: Level.Value = Level.Warn

  override def isConsoleAppenderEnabled: Boolean =
    ScalaPlugin.plugin.getPreferenceStore.getBoolean(IsConsoleAppenderEnabled)

  private def withoutConsoleRedirects(f: => Unit) {
    try {
      disableRedirectStdOutAndStdErr()
      f
    }
    finally { redirectStdOutAndStdErr() }
  }

  private def redirectStdOutAndStdErr() {
    StreamRedirect.redirectStdOutput()
    StreamRedirect.redirectStdError()
  }

  private def disableRedirectStdOutAndStdErr() {
    StreamRedirect.disableRedirectStdOutput()
    StreamRedirect.disableRedirectStdError()
  }
}
