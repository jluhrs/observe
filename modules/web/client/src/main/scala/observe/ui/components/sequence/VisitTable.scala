// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.components.sequence

import cats.Eq
import cats.syntax.all.*
// import explore.*
// import explore.components.ui.ExploreStyles
// import lucuma.ui.format.UtcFormatter
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
// import lucuma.core.enums.DatasetQaState
import lucuma.core.model.sequence.gmos.DynamicConfig
import lucuma.react.common.ReactFnProps
import lucuma.react.syntax.*
import lucuma.react.table.*
// import lucuma.schemas.model.StepRecord
// import lucuma.ui.reusability.given
// import lucuma.ui.sequence.*
import lucuma.ui.syntax.all.given
import lucuma.ui.table.*
// import observe.ui.Icons
import lucuma.ui.table.hooks.UseDynTable
import cats.data.NonEmptyList

sealed trait VisitTable[D]:
  def rows: Reusable[NonEmptyList[SequenceTableRow]]
  def cols: Reusable[List[ColumnDef[HeaderOrRow[SequenceTableRow], ?]]]
  def dynTable: UseDynTable

case class GmosNorthVisitTable(
  rows:     Reusable[NonEmptyList[SequenceTableRow]],
  cols:     Reusable[List[ColumnDef[HeaderOrRow[SequenceTableRow], ?]]],
  dynTable: UseDynTable
) extends ReactFnProps(GmosNorthVisitTable.component)
    with VisitTable[DynamicConfig.GmosNorth]

case class GmosSouthVisitTable(
  rows:     Reusable[NonEmptyList[SequenceTableRow]],
  cols:     Reusable[List[ColumnDef[HeaderOrRow[SequenceTableRow], ?]]],
  dynTable: UseDynTable
) extends ReactFnProps(GmosSouthVisitTable.component)
    with VisitTable[DynamicConfig.GmosSouth]

private sealed trait VisitTableBuilder[D <: DynamicConfig: Eq]:
  private type Props = VisitTable[D]

  protected[sequence] val component =
    ScalaFnComponent
      .withHooks[Props]
      .useReactTableBy: props =>
        TableOptions(
          props.cols,
          props.rows.map(_.toList.map(_.toHeaderOrRow)),
          enableSorting = false,
          enableColumnResizing = true,
          columnResizeMode = ColumnResizeMode.OnChange, // Maybe we should use OnEnd here?
          state = PartialTableState(
            columnSizing = props.dynTable.columnSizing,
            columnVisibility = props.dynTable.columnVisibility
          ),
          onColumnSizingChange = props.dynTable.onColumnSizingChangeHandler
        )
      .render: (props, table) =>
        PrimeVirtualizedTable(
          table,
          estimateSize = _ => 28.toPx,
          compact = Compact.Very,
          hoverableRows = true,
          celled = true,
          // tableMod = ExploreStyles.SequenceTable,
          renderSubComponent = row =>
            val step = row.original
            (<.div( /*ExploreStyles.VisitStepExtra*/ )(
              <.span( /*ExploreStyles.VisitStepExtraDatetime*/ )(
                // step.interval
                //   .map(_.start.toInstant)
                //   .fold("---")(start => UtcFormatter.format(start))
              ),
              <.span( /*ExploreStyles.VisitStepExtraDatasets*/ )(
                // step.datasets
                //   .map(dataset =>
                //     <.span( /*ExploreStyles.VisitStepExtraDatasetItem*/ )(
                //       dataset.filename.format,
                //       dataset.qaState.map(qaState =>
                //         React.Fragment(
                //           Icons.Circle /*.withClass(
                //             ExploreStyles.VisitStepExtraDatasetStatusIcon |+|
                //               (qaState match
                //                 case DatasetQaState.Pass   => ExploreStyles.IndicatorOK
                //                 case DatasetQaState.Usable => ExploreStyles.IndicatorWarning
                //                 case DatasetQaState.Fail   => ExploreStyles.IndicatorFail
                //               )
                //           )*/,
                //           <.span( /*ExploreStyles.VisitStepExtraDatasetStatusLabel*/ )(
                //             qaState.shortName
                //           )
                //         )
                //       )
                //     )
                //   )
                //   .toVdomArray
              )
            ): VdomNode).some
        )

object GmosNorthVisitTable extends VisitTableBuilder[DynamicConfig.GmosNorth]

object GmosSouthVisitTable extends VisitTableBuilder[DynamicConfig.GmosSouth]
