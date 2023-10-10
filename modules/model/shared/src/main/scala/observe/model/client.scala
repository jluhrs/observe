// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.model.events.client

import cats.*
import cats.derived.*
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import io.circe.syntax.*
import lucuma.core.model.Observation
import observe.model.Conditions
import observe.model.Environment
import observe.model.ExecutionState
import observe.model.SequenceView
import observe.model.SequencesQueue

sealed trait ClientEvent derives Eq

private given KeyEncoder[Observation.Id] = _.toString
private given KeyDecoder[Observation.Id] = Observation.Id.parse(_)

extension (v: SequencesQueue[SequenceView])
  def sequencesState: Map[Observation.Id, ExecutionState] =
    v.sessionQueue.map(o => (o.obsId, o.executionState)).toMap

extension (q: SequenceView)
  def executionState: ExecutionState =
    ExecutionState(q.status, q.runningStep.flatMap(_.id), None, Nil)

object ClientEvent:
  case class InitialEvent(environment: Environment) extends ClientEvent
      derives Eq,
        Encoder.AsObject,
        Decoder

  case class ObserveState(
    sequenceExecution: Map[Observation.Id, ExecutionState],
    conditions:        Conditions
  ) extends ClientEvent
      derives Eq,
        Encoder.AsObject,
        Decoder

  given Encoder[ClientEvent] = Encoder.instance:
    case e @ InitialEvent(_)    => e.asJson
    case e @ ObserveState(_, _) => e.asJson

  given Decoder[ClientEvent] =
    List[Decoder[ClientEvent]](
      Decoder[InitialEvent].widen,
      Decoder[ObserveState].widen
    ).reduceLeft(_ or _)
