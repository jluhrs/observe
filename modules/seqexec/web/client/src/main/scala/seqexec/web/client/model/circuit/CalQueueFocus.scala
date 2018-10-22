// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.web.client.circuit

import cats.Eq
import cats.implicits._
import gem.Observation
import monocle.Getter
import monocle.Lens
import monocle.Traversal
import monocle.macros.Lenses
import monocle.function.Each.each
import monocle.function.Each.listEach
import monocle.function.FilterIndex.filterIndex
import seqexec.model.ExecutionQueueView
import seqexec.model.QueueId
import seqexec.web.client.model._
import seqexec.web.client.components.queue.CalQueueTable
import seqexec.model.enum.QueueManipulationOp
import web.client.table.TableState
import scala.collection.immutable.SortedMap

@Lenses
final case class CalQueueFocus(
  canOperate: Boolean,
  loggedIn:   Boolean,
  seqs:       List[CalQueueSeq],
  tableState: TableState[CalQueueTable.TableColumn],
  seqOps:     SortedMap[Observation.Id, QueueSeqOperations],
  lastOp:     Option[QueueManipulationOp])

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object CalQueueFocus {
  implicit val eq: Eq[CalQueueFocus] =
    Eq.by(x => (x.canOperate, x.seqs, x.seqOps, x.tableState, x.lastOp))

  def seqQueueOpsT(
    id: Observation.Id
  ): Traversal[CalQueueFocus, QueueSeqOperations] =
    CalQueueFocus.seqOps                               ^|->>
      filterIndex((oid: Observation.Id) => oid === id)

  // All metadata of the given obs
  def calSeq(
    id: Observation.Id
  ): Getter[SeqexecAppRootModel, Option[CalQueueSeq]] =
    SeqexecAppRootModel.sequences.composeGetter(CalQueueSeq.calQueueSeqG(id))

  def calTS(
    id: QueueId
  ): Lens[SeqexecAppRootModel, Option[TableState[CalQueueTable.TableColumn]]] =
    SeqexecAppRootModel.uiModel        ^|->
      AppTableStates.tableStateL       ^|->
      AppTableStates.queueTableAtL(id)

  private def seqOpsL(id: QueueId) =
    SeqexecAppRootModel.uiModel             ^|->
      SeqexecUIModel.queues                 ^|-?
      CalibrationQueues.calStateSeqOpsT(id)

  private def qLastOpL(id: QueueId) =
    SeqexecAppRootModel.uiModel        ^|->
      SeqexecUIModel.queues            ^|-?
      CalibrationQueues.calLastOpO(id)

  // A fairly complicated getter
  def calQueueG(
    id: QueueId
  ): Getter[SeqexecAppRootModel, Option[CalQueueFocus]] = {
    // All ids on the queue
    val ids: Traversal[SeqexecAppRootModel, Observation.Id] =
      SeqexecAppRootModel.executionQueuesT(id) ^|->
        ExecutionQueueView.queue               ^|->>
        each

    // combine
    val calQueueSeqG = (s: SeqexecAppRootModel) =>
      ids.getAll(s).map(i => calSeq(i).get(s))

    ClientStatus.clientStatusFocusL.asGetter
      .zip(
        Getter(calQueueSeqG).zip(calTS(id).asGetter.zip(Getter(
          seqOpsL(id).getOption).zip(Getter(qLastOpL(id).getOption))))) >>> {
      case (status, (ids, (ts, (seqOps, lastOp)))) =>
        val obsIds = ids.collect { case Some(x) => x }
        CalQueueFocus(status.canOperate,
                      status.isLogged,
                      obsIds,
                      ts.getOrElse(CalQueueTable.State.ROTableState),
                      seqOps.getOrElse(SortedMap.empty),
                      lastOp.flatten).some
      case _ =>
        none
    }
  }
}
