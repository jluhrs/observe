// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components.sequence

import cats.Eq
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.core.enums.Breakpoint
import lucuma.core.enums.Instrument
import lucuma.core.enums.SequenceType
import lucuma.core.model.Observation
import lucuma.core.model.sequence.*
import lucuma.core.model.sequence.gmos.*
import lucuma.react.SizePx
import lucuma.react.common.*
import lucuma.react.resizeDetector.hooks.*
import lucuma.react.syntax.*
import lucuma.react.table.*
import lucuma.typed.{tanstackTableCore => raw}
import lucuma.ui.reusability.given
import lucuma.ui.sequence.SequenceColumns.*
import lucuma.ui.sequence.*
import lucuma.ui.table.ColumnSize.*
import lucuma.ui.table.*
import lucuma.ui.table.hooks.*
import observe.model.ExecutionState
import observe.model.ObserveStep
import observe.model.StepProgress
import observe.model.StepState
import observe.ui.Icons
import observe.ui.ObserveStyles
import observe.ui.components.sequence.steps.*
import observe.ui.model.ObservationRequests
import observe.ui.model.enums.ClientMode
import observe.ui.model.reusability.given

import scalajs.js

sealed trait SequenceTables[S, D](
  protected[sequence] val instrument:    Instrument,
  protected[sequence] val nodAndShuffle: Option[GmosNodAndShuffle]
):
  def clientMode: ClientMode
  def obsId: Observation.Id
  def config: ExecutionConfig[S, D]
  def executionState: ExecutionState
  def progress: Option[StepProgress]
  def selectedStepId: Option[Step.Id]
  def setSelectedStepId: Step.Id => Callback
  def requests: ObservationRequests
  def isPreview: Boolean
  def flipBreakpoint: (Observation.Id, Step.Id, Breakpoint) => Callback

  private def steps(sequence: ExecutionSequence[D]): List[SequenceRow.FutureStep[DynamicConfig]] =
    SequenceRow.FutureStep
      .fromAtoms(
        sequence.possibleFuture,
        _ => none // TODO Pass signal to noise
      )
      .map(_.asInstanceOf[SequenceRow.FutureStep[DynamicConfig]]) // ObserveStep loses the type

  protected[sequence] lazy val (acquisitionCurrentSteps, scienceCurrentSteps)
    : (List[ObserveStep], List[ObserveStep]) =
    executionState.sequenceType match
      case SequenceType.Acquisition => (executionState.loadedSteps, List.empty)
      case SequenceType.Science     => (List.empty, executionState.loadedSteps)

  protected[sequence] def currentStepsToRows(
    currentSteps: List[ObserveStep]
  ): List[CurrentAtomStepRow] =
    currentSteps.map: step =>
      CurrentAtomStepRow(
        step,
        breakpoint =
          if (executionState.breakpoints.contains_(step.id)) Breakpoint.Enabled
          else Breakpoint.Disabled,
        isFirstOfAtom = currentSteps.headOption.exists(_.id === step.id)
      )

  // TODO Stitch past visits
  protected[sequence] lazy val acquisitionRows: List[SequenceRow[DynamicConfig]] =
    currentStepsToRows(acquisitionCurrentSteps) ++ config.acquisition.map(steps).orEmpty

  protected[sequence] lazy val scienceRows: List[SequenceRow[DynamicConfig]] =
    currentStepsToRows(scienceCurrentSteps) ++ config.science.map(steps).orEmpty

case class GmosNorthSequenceTables(
  clientMode:        ClientMode,
  obsId:             Observation.Id,
  config:            ExecutionConfig[StaticConfig.GmosNorth, DynamicConfig.GmosNorth],
  executionState:    ExecutionState,
  progress:          Option[StepProgress],
  selectedStepId:    Option[Step.Id],
  setSelectedStepId: Step.Id => Callback,
  requests:          ObservationRequests,
  isPreview:         Boolean,
  flipBreakpoint:    (Observation.Id, Step.Id, Breakpoint) => Callback
) extends ReactFnProps(GmosNorthSequenceTables.component)
    with SequenceTables[StaticConfig.GmosNorth, DynamicConfig.GmosNorth](
      Instrument.GmosNorth,
      config.static.nodAndShuffle
    )

case class GmosSouthSequenceTables(
  clientMode:        ClientMode,
  obsId:             Observation.Id,
  config:            ExecutionConfig[StaticConfig.GmosSouth, DynamicConfig.GmosSouth],
  executionState:    ExecutionState,
  progress:          Option[StepProgress],
  selectedStepId:    Option[Step.Id],
  setSelectedStepId: Step.Id => Callback,
  requests:          ObservationRequests,
  isPreview:         Boolean,
  flipBreakpoint:    (Observation.Id, Step.Id, Breakpoint) => Callback
) extends ReactFnProps(GmosSouthSequenceTables.component)
    with SequenceTables[StaticConfig.GmosSouth, DynamicConfig.GmosSouth](
      Instrument.GmosSouth,
      config.static.nodAndShuffle
    )

private sealed trait SequenceTablesBuilder[S: Eq, D: Eq] extends SequenceTablesDefs:
  private type Props = SequenceTables[S, D]

  protected[sequence] val component =
    ScalaFnComponent
      .withHooks[Props]
      .useResizeDetector()
      .useMemoBy((props, _) => // cols
        (props.clientMode,
         props.instrument,
         props.obsId,
         props.requests,
         props.executionState,
         props.progress,
         props.isPreview,
         props.selectedStepId
        )
      ): (props, _) =>
        columnDefs(props.flipBreakpoint)
      .useMemoBy((props, _, _) => (props.acquisitionRows, props.scienceRows))( // sequences
        (props, _, _) =>
          (acquisitionRows, scienceRows) =>
            (
              acquisitionRows, // .zipWithStepIndex.map(SequenceTableRow.apply),
              scienceRows      // .zipWithStepIndex.map(SequenceTableRow.apply)
            )
      )
      .useDynTableBy((_, resize, _, _) => (DynTableDef, SizePx(resize.width.orEmpty)))
      .useReactTableBy: (props, resize, cols, sequences, dynTable) =>
        TableOptions(
          cols,
          sequences.map(_._1),
          enableSorting = false,
          enableColumnResizing = true,
          columnResizeMode = ColumnResizeMode.OnChange, // Maybe we should use OnEnd here?
          state = PartialTableState(
            columnSizing = dynTable.columnSizing,
            columnVisibility = dynTable.columnVisibility
          ),
          onColumnSizingChange = dynTable.onColumnSizingChangeHandler
        )
      .useReactTableBy: (props, resize, cols, sequences, dynTable, _) =>
        TableOptions(
          cols,
          sequences.map(_._2),
          enableSorting = false,
          enableColumnResizing = true,
          columnResizeMode = ColumnResizeMode.OnChange, // Maybe we should use OnEnd here?
          state = PartialTableState(
            columnSizing = dynTable.columnSizing,
            columnVisibility = dynTable.columnVisibility
          ),
          onColumnSizingChange = dynTable.onColumnSizingChangeHandler
        )
      .render: (props, resize, _, _, _, acquisitionTable, scienceTable) =>
        extension (row: SequenceTableRow)
          def isSelected: Boolean =
            props.selectedStepId match
              case Some(stepId) => row.id.contains(stepId)
              case _            => false

        val tableStyle: Css =
          ObserveStyles.ObserveTable |+| ObserveStyles.StepTable |+| SequenceStyles.SequenceTable

        def computeRowMods(row: raw.buildLibTypesMod.Row[SequenceTableRow]): TagMod =
          val step                       = row.original
          val stepIdOpt: Option[Step.Id] = step.id.toOption

          TagMod(
            stepIdOpt
              .map: stepId =>
                (^.onClick --> props.setSelectedStepId(stepId))
                  .when(step.stepTime === StepTime.Present)
                  .unless(props.executionState.isLocked)
              .whenDefined,
            if (step.isSelected) ObserveStyles.RowSelected else ObserveStyles.RowIdle,
            ObserveStyles.StepRowWithBreakpoint.when_(
              stepIdOpt.exists(props.executionState.breakpoints.contains)
            ),
            ObserveStyles.StepRowFirstInAtom.when_(step.isFirstInAtom),
            ObserveStyles.StepRowPossibleFuture.when_(step.stepTime === StepTime.Future),
            step.stepState match
              case s if s.hasError                      => ObserveStyles.StepRowError
              case StepState.Paused | StepState.Skipped => ObserveStyles.StepRowWarning
              case StepState.Completed                  => ObserveStyles.StepRowDone
              case StepState.Aborted                    => ObserveStyles.StepRowError
              case _                                    => Css.Empty
          )

        def computeHeaderCellMods(
          headerCell: raw.buildLibTypesMod.Header[SequenceTableRow, Any]
        ): Css =
          headerCell.column.id match
            case id if id == BreakpointColumnId.value   => ObserveStyles.BreakpointTableHeader
            case id if id == RunningStateColumnId.value => ObserveStyles.RunningStateTableHeader
            case _                                      => Css.Empty

        def computeCellMods(cell: raw.buildLibTypesMod.Cell[SequenceTableRow, Any]): TagMod =
          cell.column.id match
            case id if id == BreakpointColumnId.value                                   =>
              ObserveStyles.BreakpointTableCell
            case id if id == RunningStateColumnId.value && cell.row.original.isSelected =>
              TagMod(
                ObserveStyles.SelectedStateTableCellShown,
                resize.width
                  .map: w =>
                    ^.width := s"${w - ColumnSizes(BreakpointSpaceColumnId).initial.value}px"
                  .whenDefined
              )
            case _                                                                      =>
              TagMod.empty

        val acquisition =
          PrimeTable(
            acquisitionTable,
            tableMod = tableStyle,
            rowMod = computeRowMods,
            headerCellMod = computeHeaderCellMods,
            cellMod = computeCellMods
          )

        React.Fragment(
          // VisitsViewer(props.obsId),
          PrimeAutoHeightVirtualizedTable(
            scienceTable,
            estimateSize = _ => 25.toPx,
            containerRef = resize.ref,
            tableMod = TagMod(tableStyle, ^.marginTop := "15px"),
            rowMod = computeRowMods,
            // We display the whole acquisition table as a preamble to the science table, which is virtualized.
            // This renders as:
            //  <div outer>
            //    <div inner>
            //      Acquisition Table (complete)
            //      Science Table (virtualized)
            // TODO Test if virtualization scrollbar works well with this approach when there are a lot of rows.
            innerContainerMod = TagMod(^.width := "100%", acquisition)
              .unless(acquisitionTable.getRowModel().rows.isEmpty),
            headerCellMod = computeHeaderCellMods,
            cellMod = computeCellMods,
            overscan = 5
          )
        )

object GmosNorthSequenceTables
    extends SequenceTablesBuilder[StaticConfig.GmosNorth, DynamicConfig.GmosNorth]

object GmosSouthSequenceTables
    extends SequenceTablesBuilder[StaticConfig.GmosSouth, DynamicConfig.GmosSouth]
