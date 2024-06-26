// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components.sequence.steps

import cats.syntax.all.*
import lucuma.core.util.TimeSpan
import observe.model.ObserveStage
import observe.model.dhs.ImageFileId

trait ProgressLabel:
  protected def renderLabel(
    fileId:            ImageFileId,
    remainingTimeSpan: Option[TimeSpan],
    isStopping:        Boolean,
    isPausedInStep:    Boolean,
    stage:             ObserveStage
  ): String =
    val durationStr: String = remainingTimeSpan
      .map(_.toMicroseconds)
      .filter(_ > 0)
      .foldMap: micros =>
        // s"mm:ss (s s) Remaining"
        val remainingSecs: Int = (micros / 1000000).toInt
        val remainingMins: Int = remainingSecs / 60
        val secsRemainder: Int = remainingSecs % 60
        val onlySecs: String   = if (remainingMins == 0) "" else s"($remainingSecs s)"
        List(f"$remainingMins:$secsRemainder%02d", onlySecs, "Remaining")
          .filterNot(_.isEmpty)
          .mkString(" ")

    val stageStr: String = (isPausedInStep, isStopping, stage) match
      case (true, _, _)                    => "Paused"
      case (_, true, _)                    => "Stopping - Reading out..."
      case (_, _, ObserveStage.Preparing)  => "Preparing"
      case (_, _, ObserveStage.ReadingOut) => "Reading out..."
      case _                               => ""

    // if (paused) s"$fileId - Paused$durationStr"
    // else if (stopping) s"$fileId - Stopping - Reading out..."
    // else
    //   stageStr match
    //     case Some(stage) => s"$fileId - $stage"
    //     case _           =>
    //       remainingMillis.fold(fileId.value): millis =>
    //         if (millis > 0) s"${fileId.value}$durationStr" else s"${fileId.value} - Reading out..."

    List(fileId.value, durationStr, stageStr)
      .filterNot(_.isEmpty)
      .mkString(" - ")
