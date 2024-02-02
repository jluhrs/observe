// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components

import cats.syntax.all.*
import crystal.react.*
import japgolly.scalajs.react.*
import lucuma.core.enums.Breakpoint
import lucuma.core.model.Observation
import lucuma.core.model.sequence.InstrumentExecutionConfig
import lucuma.core.model.sequence.Step
import lucuma.react.common.ReactFnProps
import observe.model.ExecutionState
import observe.model.StepProgress
import observe.ui.components.sequence.GmosNorthSequenceTables
import observe.ui.components.sequence.GmosSouthSequenceTables
import observe.ui.model.AppContext
import observe.ui.model.ObservationRequests
import observe.ui.model.enums.ClientMode
import observe.ui.services.SequenceApi

case class ObservationSequence(
  obsId:           Observation.Id,
  config:          InstrumentExecutionConfig,
  executionState:  View[ExecutionState],
  progress:        Option[StepProgress],
  requests:        ObservationRequests,
  selectedStep:    Option[Step.Id],
  setSelectedStep: Step.Id => Callback,
  clientMode:      ClientMode
) extends ReactFnProps(ObservationSequence.component)

object ObservationSequence:
  private type Props = ObservationSequence

  private val component = ScalaFnComponent
    .withHooks[Props]
    .useContext(AppContext.ctx)
    .useContext(SequenceApi.ctx)
    .render: (props, ctx, sequenceApi) =>
      import ctx.given

      val breakpoints: View[Set[Step.Id]] =
        props.executionState.zoom(ExecutionState.breakpoints)

      val flipBreakPoint: (Observation.Id, Step.Id, Breakpoint) => Callback =
        (obsId, stepId, value) =>
          breakpoints
            .mod(set => if (set.contains(stepId)) set - stepId else set + stepId) >>
            sequenceApi.setBreakpoint(obsId, stepId, value).runAsync

      props.config match
        case InstrumentExecutionConfig.GmosNorth(config) =>
          GmosNorthSequenceTables(
            props.clientMode,
            props.obsId,
            config,
            props.executionState.get,
            props.progress,
            props.selectedStep,
            props.setSelectedStep,
            props.requests,
            isPreview = false,
            flipBreakPoint
          )
        case InstrumentExecutionConfig.GmosSouth(config) =>
          GmosSouthSequenceTables(
            props.clientMode,
            props.obsId,
            config,
            props.executionState.get,
            props.progress,
            props.selectedStep,
            props.setSelectedStep,
            props.requests,
            isPreview = false,
            flipBreakPoint
            // )
          )
