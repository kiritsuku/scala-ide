package org.scalaide.extensions

import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.Selections
import org.scalaide.core.text.Change
import org.scalaide.core.text.Document
import scala.reflect.internal.util.SourceFile


trait SaveAction extends ScalaIdeExtension
