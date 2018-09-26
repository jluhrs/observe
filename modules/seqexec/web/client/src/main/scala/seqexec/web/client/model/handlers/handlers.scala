// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.web.client.handlers

import cats.implicits._
import diode.ActionHandler
import diode.ActionResult
import diode.Effect
import diode.ModelRW
import diode.NoAction
import gem.enum.Site
import seqexec.model.Observer
import seqexec.model.Operator
import seqexec.model.SequencesQueue
import seqexec.model.SequenceView
import seqexec.web.client.model._
import seqexec.web.client.ModelOps._
import seqexec.web.client.actions._
import seqexec.web.client.services.SeqexecWebClient
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Handles actions requesting sync
  */
class SyncRequestsHandler[M](modelRW: ModelRW[M, Boolean])
    extends ActionHandler(modelRW)
    with Handlers[M, Boolean] {
  def handleSyncRequestOperation: PartialFunction[Any, ActionResult[M]] = {
    case RequestSync(s) =>
      updated(true, Effect(SeqexecWebClient.sync(s).map(r => if (r.sessionQueue.isEmpty) RunSyncFailed(s) else RunSync(s))))
  }

  def handleSyncResult: PartialFunction[Any, ActionResult[M]] = {
    case RunSyncFailed(_) =>
      updated(false)

    case RunSync(_) =>
      updated(false)
  }

  override def handle: PartialFunction[Any, ActionResult[M]] =
    List(handleSyncRequestOperation, handleSyncResult).combineAll
}

/**
  * Handles sequence execution actions
  */
class SequenceExecutionHandler[M](
  modelRW: ModelRW[M, SequencesQueue[SequenceView]])
    extends ActionHandler(modelRW)
    with Handlers[M, SequencesQueue[SequenceView]] {
  def handleUpdateObserver: PartialFunction[Any, ActionResult[M]] = {
    case UpdateObserver(sequenceId, name) =>
      val updateObserverE = Effect(SeqexecWebClient.setObserver(sequenceId, name.value).map(_ => NoAction))
      val updatedSequences = value.copy(sessionQueue = value.sessionQueue.collect {
        case s if s.id === sequenceId =>
          s.copy(metadata = s.metadata.copy(observer = Some(name)))
        case s                        => s
      })
      updated(updatedSequences, updateObserverE)
  }

  def handleFlipSkipBreakpoint: PartialFunction[Any, ActionResult[M]] = {
    case FlipSkipStep(sequenceId, step) =>
      val skipRequest = Effect(SeqexecWebClient.skip(sequenceId, step.flipSkip).map(_ => NoAction))
      updated(value.copy(sessionQueue = value.sessionQueue.collect {
        case s if s.id === sequenceId => s.flipSkipMarkAtStep(step)
        case s                        => s
      }), skipRequest)

    case FlipBreakpointStep(sequenceId, step) =>
      val breakpointRequest = Effect(SeqexecWebClient.breakpoint(sequenceId, step.flipBreakpoint).map(_ => NoAction))
      updated(value.copy(sessionQueue = value.sessionQueue.collect {
        case s if s.id === sequenceId => s.flipBreakpointAtStep(step)
        case s                        => s
      }), breakpointRequest)
  }

  override def handle: PartialFunction[Any, ActionResult[M]] =
    List(handleUpdateObserver, handleFlipSkipBreakpoint).combineAll
}

/**
  * Handles updates to the operator
  */
class OperatorHandler[M](modelRW: ModelRW[M, Option[Operator]])
    extends ActionHandler(modelRW)
    with Handlers[M, Option[Operator]] {
  override def handle: PartialFunction[Any, ActionResult[M]] = {
    case UpdateOperator(name) =>
      val updateOperatorE = Effect(SeqexecWebClient.setOperator(name).map(_ => NoAction))
      updated(name.some, updateOperatorE)
  }
}

/**
  * Handles setting the site
  */
class SiteHandler[M](modelRW: ModelRW[M, Option[Site]])
    extends ActionHandler(modelRW)
    with Handlers[M, Option[Site]] {

  override def handle: PartialFunction[Any, ActionResult[M]] = {
    case Initialize(site) =>
      updated(Some(site))
  }
}

/**
  * Handles updates to the log
  */
class GlobalLogHandler[M](modelRW: ModelRW[M, GlobalLog])
    extends ActionHandler(modelRW)
    with Handlers[M, GlobalLog] {
  override def handle: PartialFunction[Any, ActionResult[M]] = {
    case AppendToLog(s) =>
      updated(value.copy(log = value.log.append(s)))

    case ToggleLogArea =>
      updated(value.copy(display = value.display.toggle))
  }
}

/**
  * Handles updates to the defaultObserver
  */
class DefaultObserverHandler[M](modelRW: ModelRW[M, Observer])
    extends ActionHandler(modelRW)
    with Handlers[M, Observer] {
  override def handle: PartialFunction[Any, ActionResult[M]] = {
    case UpdateDefaultObserver(o) =>
      updated(o)
  }
}

/**
  * Handle for UI debugging events
  */
class DebuggingHandler[M](modelRW: ModelRW[M, SequencesQueue[SequenceView]])
    extends ActionHandler(modelRW)
    with Handlers[M, SequencesQueue[SequenceView]] {
  override def handle: PartialFunction[Any, ActionResult[M]] = {
    case MarkStepAsRunning(obsId, step) =>
      updated(value.copy(sessionQueue = value.sessionQueue.collect {
        case v: SequenceView if v.id === obsId => v.showAsRunning(step)
        case v                                 => v
      }))
  }
}
