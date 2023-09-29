// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server

import lucuma.core.util.arb.ArbEnumerated.*
import lucuma.core.util.arb.ArbGid.*
import observe.model.Observation
import org.scalacheck.Arbitrary.*
import org.scalacheck.{Arbitrary, Cogen}
import observe.model.BatchCommandState
import observe.model.enums.Instrument
import observe.model.ObserveModelArbitraries.given
import observe.server.ExecutionQueue.SequenceInQueue
import observe.model.SequenceState
import observe.model.enums.Resource

trait ObserveServerArbitraries {

  given Cogen[Map[Instrument, Observation.Name]] =
    Cogen[List[(Instrument, Observation.Name)]].contramap(_.toList)

  given Arbitrary[SequenceInQueue] = Arbitrary {
    for {
      o <- arbitrary[Observation.Id]
      i <- arbitrary[Instrument]
      s <- arbitrary[SequenceState]
      r <- arbitrary[Set[Resource]]
    } yield SequenceInQueue(o, i, s, r)
  }

  given Cogen[SequenceInQueue] =
    Cogen[(Observation.Id, Instrument, SequenceState, List[Resource])]
      .contramap(x => (x.obsId, x.instrument, x.state, x.resources.toList))

  given Arbitrary[ExecutionQueue] = Arbitrary {
    for {
      n <- arbitrary[String]
      s <- arbitrary[BatchCommandState]
      q <- arbitrary[List[SequenceInQueue]]
    } yield ExecutionQueue(n, s, q)
  }

  given Cogen[ExecutionQueue] =
    Cogen[(String, BatchCommandState, List[SequenceInQueue])]
      .contramap(x => (x.name, x.cmdState, x.queue))

}

object ObserveServerArbitraries extends ObserveServerArbitraries