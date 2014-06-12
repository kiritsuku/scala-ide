package org.scalaide.core.text

trait Change

case class Add(start: Int, text: String) extends Change
case class Replace(start: Int, end: Int, text: String) extends Change
case class Remove(start: Int, end: Int) extends Change