// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components.sequence.steps

import cats.syntax.all.*
import crystal.Pot
import crystal.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.core.model.Observation
import lucuma.core.model.sequence.Step
import lucuma.react.common.*
import lucuma.react.primereact.ProgressBar
import observe.model.ImageFileId
import observe.model.ObservationProgress
import observe.model.enums.ObservationStage
import observe.ui.ObserveStyles

import java.time.Duration
import java.util.UUID

/**
 * Component to wrap the progress bar
 */
case class ObservationProgressBar(
  obsId:    Observation.Id,
  stepId:   Step.Id,
  fileId:   ImageFileId,
  stopping: Boolean,
  paused:   Boolean
) extends ReactFnProps(ObservationProgressBar.component)
// TODO Substitute for a stream hook - Progress should be a Topic somewhere
// protected[steps] val connect: ReactConnectProxy[Option[ObservationProgress]] =
//   ObserveCircuit.connect(ObserveCircuit.obsProgressReader[ObservationProgress](obsId, stepId))

object ObservationProgressBar extends ProgressLabel:
  private type Props = ObservationProgressBar

  private val component = ScalaFnComponent
    .withHooks[Props]
    .useMemo[Unit, Pot[ObservationProgress]](())(_ =>
      ObservationProgress
        .Regular(
          obsId = Observation.Id.fromLong(133742).get,
          obsName = "Test observation",
          stepId = Step.Id.fromUuid(UUID.randomUUID),
          total = Duration.ofSeconds(1200),
          remaining = Duration.ofSeconds(932),
          stage = ObservationStage.Acquiring
        )
        .ready
    )
    .render((props, progress) =>
      progress.value.toOption match
        case Some(ObservationProgress.Regular(_, _, _, total, remaining, stage)) =>
          // TODO Smooth Progress Bar
          // val remainingMillis = p.maxValue - s.value

          val totalMillis     = total.toMillis.toInt
          val remainingMillis = remaining.toMillis.toInt

          val progress = ((totalMillis - remainingMillis) * 100) / totalMillis

          ProgressBar(
            value = progress.toInt,
            clazz = ObserveStyles.ObservationProgressBar,
            displayValueTemplate = _ =>
              // This is a trick to be able to center when text fits, but align left when it doesn't, overflowing only to the right.
              // Achieved by rendering the 3 divs inside a space-between flexbox.
              React.Fragment(
                <.div,
                <.div(
                  renderLabel(
                    props.fileId,
                    remainingMillis.some,
                    props.stopping,
                    props.paused,
                    stage
                  )
                ),
                <.div
              )
          )

        // SmoothObservationProgressBar(
        //   p.fileId,
        //   total.toMilliseconds.toInt,
        //   total.toMilliseconds.toInt - max(0, remaining.toMilliseconds.toInt),
        //   p.stopping,
        //   p.paused,
        //   stage
        // )
        case _ =>
          val msg = if (props.paused) s"${props.fileId.value} - Paused" else props.fileId.value

          React.Fragment(
            ProgressBar(
              value = 100,
              clazz = ObserveStyles.ObservationProgressBar,
              displayValueTemplate = _ => msg
            )
          )
    )
