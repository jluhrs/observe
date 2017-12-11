// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.seqexec.engine

import edu.gemini.seqexec.model.ActionType

import org.scalatest._

import scalaz.concurrent.Task

class ExecutionSpec extends FlatSpec with Matchers {

  private val observeResult: Result.Response = Result.Observed("dummyId")
  private val ok: Result = Result.OK(observeResult)
  private val completedAction: Action = fromTask(ActionType.Observe, Task(ok)).copy(state = Action.Completed(observeResult))
  private val action: Action = fromTask(ActionType.Observe, Task(ok))
  private val curr: Execution = Execution(List(completedAction, action))

  "currentify" should "be None only when an Execution is empty" in {
    assert(Execution.currentify(List(action, action)).nonEmpty)
  }

  "currentify2" should "be None only when an Execution is empty" in {
    assert(Execution.currentify(Nil).isEmpty)
  }

  "uncurrentify" should "be None when not all actions are completed" in {
    assert(Execution(Nil).uncurrentify.isEmpty)
  }

  "uncurrentify2" should "be None when not all actions are completed" in {
    assert(curr.uncurrentify.isEmpty)
  }

  "marking an index out of bounds" should "not modify the execution" in {
    assert(curr.mark(3)(Result.Error("")) === curr)
  }

  "marking an index inbound" should "modify the execution" in {
    assert(curr.mark(1)(Result.Error("")) !== curr)
  }

}
