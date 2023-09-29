// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server

import cats.Id
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.eq.*
import observe.engine.*
import observe.model.enums.*
import observe.server.TestCommon.*

class StepsViewNSSuite extends munit.FunSuite {

  test("running after the first observe") {
    val executions: List[ParallelActions[IO]] =
      List(NonEmptyList.one(running(Resource.TCS)), NonEmptyList.one(observePartial))
    assert(StepsView.observeStatus(executions) === ActionStatus.Running)
  }

  test("running after the observe and configure") {
    val executions: List[ParallelActions[Id]] = List(NonEmptyList.one(running(Resource.TCS)),
                                                     NonEmptyList.one(observePartial),
                                                     NonEmptyList.one(done(Instrument.GmosN))
    )
    assert(StepsView.observeStatus(executions) === ActionStatus.Running)
  }

  test("running after the observe/configure/continue/complete") {
    val executions: List[ParallelActions[Id]] = List(NonEmptyList.one(running(Resource.TCS)),
                                                     NonEmptyList.one(observePartial),
                                                     NonEmptyList.one(done(Instrument.GmosN)),
                                                     NonEmptyList.one(observed)
    )
    assert(StepsView.observeStatus(executions) === ActionStatus.Running)
  }

}
