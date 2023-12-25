// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components.sequence.steps

import cats.syntax.all.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.core.enums.Instrument
import lucuma.core.model.Observation
import lucuma.core.model.sequence.Step
import lucuma.react.common.*
import lucuma.react.fa.FontAwesomeIcon
import lucuma.react.fa.IconSize
import lucuma.react.primereact.*
import observe.model.SequenceState
import observe.model.operations.Operations.*
import observe.model.operations.*
import observe.ui.Icons
import observe.ui.ObserveStyles
import observe.ui.components.DefaultTooltipOptions
import observe.ui.model.AppContext
import observe.ui.model.ObservationRequests
import observe.ui.services.SequenceApi
import observe.model.enums.RunOverride

/**
 * Contains a set of control buttons like stop/abort
 */
case class StepControlButtons(
  obsId:           Observation.Id,
  instrument:      Instrument,
  sequenceState:   SequenceState,
  stepId:          Step.Id,
  isObservePaused: Boolean,
  isReadingOut:    Boolean,
  isMultiLevel:    Boolean,
  requests:        ObservationRequests
) extends ReactFnProps(StepControlButtons.component):
  val operations: List[Operations] =
    instrument.operations(OperationLevel.Observation, isObservePaused, isMultiLevel)

  val isRunning: Boolean = sequenceState.isRunning

  val requestInFlight: Boolean = requests.stepRequestInFlight

object StepControlButtons:
  private type Props = StepControlButtons

  private val component = ScalaFnComponent
    .withHooks[Props]
    .useContext(AppContext.ctx)
    .useContext(SequenceApi.ctx)
    .render: (props, ctx, sequenceApi) =>
      import ctx.given

      // def requestedIcon(icon: FontAwesomeIcon): VdomNode =
      //   icon
      // IconGroup(
      //   icon(^.key                                                   := "main"),
      //   IconCircleNotched.copy(loading = true, color = Yellow)(^.key := "requested")
      // )

      // val pauseGracefullyIcon: VdomNode =
      //   props.nsPendingObserveCmd match
      //     case Some(NodAndShuffleStep.PendingObserveCmd.PauseGracefully) => requestedIcon(Icons.Pause)
      //     case _                                                         => Icons.Pause

      // val stopGracefullyIcon: VdomNode =
      //   props.nsPendingObserveCmd match
      //     case Some(NodAndShuffleStep.PendingObserveCmd.StopGracefully) => requestedIcon(Icons.Stop)
      //     case _                                                        => Icons.Stop

      // p.connect { proxy =>
      // val isReadingOut = false // proxy().exists(_.stage === ObserveStage.ReadingOut)
      InputGroup(ObserveStyles.ControlButtonStrip)(
        // ObserveStyles.notInMobile,
        if (props.isRunning) {
          props.operations
            .map[VdomNode]:
              case ResumeObservation =>
                Button(
                  clazz = ObserveStyles.PlayButton,
                  icon = Icons.Play.withFixedWidth(),
                  tooltip = "Resume the current exposure",
                  tooltipOptions = DefaultTooltipOptions,
                  disabled = props.requestInFlight || !props.isObservePaused || props.isReadingOut,
                  onClickE = _.stopPropagationCB >> sequenceApi.resumeObs(props.obsId).runAsync
                )
              case PauseObservation  =>
                Button(
                  clazz = ObserveStyles.PauseButton,
                  icon = Icons.Pause.withFixedWidth(),
                  tooltip = "Pause the current exposure",
                  tooltipOptions = DefaultTooltipOptions,
                  disabled = props.requestInFlight || props.isObservePaused || props.isReadingOut,
                  onClickE = _.stopPropagationCB >> sequenceApi.pauseObs(props.obsId).runAsync
                )
              case StopObservation   =>
                Button(
                  clazz = ObserveStyles.StopButton,
                  icon = Icons.Stop.withFixedWidth().withSize(IconSize.LG),
                  tooltip = "Stop the current exposure early",
                  tooltipOptions = DefaultTooltipOptions,
                  disabled = props.requestInFlight || props.isReadingOut,
                  onClickE = _.stopPropagationCB >> sequenceApi.stop(props.obsId).runAsync
                )
              case AbortObservation  =>
                Button(
                  clazz = ObserveStyles.AbortButton,
                  icon = Icons.XMark.withFixedWidth().withSize(IconSize.LG),
                  tooltip = "Abort the current exposure",
                  tooltipOptions = DefaultTooltipOptions,
                  disabled = props.requestInFlight || props.isReadingOut,
                  onClickE = _.stopPropagationCB >> sequenceApi.abort(props.obsId).runAsync
                )
              // // N&S operations
              // case PauseImmediatelyObservation =>
              //   Popup(
              //     position = PopupPosition.TopRight,
              //     trigger = Button(
              //       icon = true,
              //       color = Teal,
              //       basic = true,
              //       onClick = requestObsPause(p.obsId, p.stepId),
              //       disabled = p.requestInFlight || p.isObservePaused || isReadingOut
              //     )(IconPause)
              //   )("Pause the current exposure immediately")
              // case PauseGracefullyObservation  =>
              //   Popup(
              //     position = PopupPosition.TopRight,
              //     trigger = Button(
              //       icon = true,
              //       color = Teal,
              //       onClick = requestGracefulObsPause(p.obsId, p.stepId),
              //       disabled =
              //         p.requestInFlight || p.isObservePaused || p.nsPendingObserveCmd.isDefined || isReadingOut
              //     )(pauseGracefullyIcon)
              //   )("Pause the current exposure at the end of the cycle")
              // case StopImmediatelyObservation  =>
              //   Popup(
              //     position = PopupPosition.TopRight,
              //     trigger = Button(
              //       icon = true,
              //       color = Orange,
              //       basic = true,
              //       onClick = requestStop(p.obsId, p.stepId),
              //       disabled = p.requestInFlight || isReadingOut
              //     )(IconStop)
              //   )("Stop the current exposure immediately")
              // case StopGracefullyObservation   =>
              //   Popup(
              //     position = PopupPosition.TopRight,
              //     trigger = Button(
              //       icon = true,
              //       color = Orange,
              //       onClick = requestGracefulStop(p.obsId, p.stepId),
              //       disabled =
              //         p.requestInFlight || p.isObservePaused || p.nsPendingObserveCmd.isDefined || isReadingOut
              //     )(stopGracefullyIcon)
              //   )("Stop the current exposure at the end of the cycle")
              case _                 => EmptyVdom
            .toTagMod
        } else
          Button(
            clazz = ObserveStyles.PlayButton |+| ObserveStyles.SingleButton,
            icon = Icons.Play.withFixedWidth(),
            tooltip = "Run from this step", // TODO Get step index
            tooltipOptions = DefaultTooltipOptions,
            disabled = props.requestInFlight,
            onClickE = _.stopPropagationCB >>
              sequenceApi
                .startFrom(props.obsId, props.stepId, runOverride = RunOverride.Override)
                .runAsync
          ): VdomNode
      )
