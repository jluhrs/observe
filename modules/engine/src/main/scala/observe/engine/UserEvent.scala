// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.engine

import cats.syntax.all.*
import fs2.Stream
import lucuma.core.enums.Breakpoint
import lucuma.core.model.User
import lucuma.core.model.sequence.Step
import observe.model.ClientId
import observe.model.Observation

import java.time.Instant

/**
 * Events generated by the user.
 */
sealed trait UserEvent[F[_], S, U] extends Product with Serializable {
  def user: Option[User]
  def username: String = user.foldMap(_.displayName)
}

object UserEvent {

  case class Start[F[_], S, U](
    id:       Observation.Id,
    user:     Option[User],
    clientId: ClientId
  ) extends UserEvent[F, S, U]
  case class Pause[F[_], S, U](id: Observation.Id, user: Option[User]) extends UserEvent[F, S, U]
  case class CancelPause[F[_], S, U](id: Observation.Id, user: Option[User])
      extends UserEvent[F, S, U]
  case class Breakpoints[F[_], S, U](
    id:    Observation.Id,
    user:  Option[User],
    steps: List[Step.Id],
    v:     Breakpoint
  ) extends UserEvent[F, S, U]
  case class SkipMark[F[_], S, U](
    id:   Observation.Id,
    user: Option[User],
    step: Step.Id,
    v:    Boolean
  ) extends UserEvent[F, S, U]
  case class Poll[F[_], S, U](clientId: ClientId)                      extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }
  // Generic event to put a function in the main Stream process, which takes an
  // action depending on the current state
  case class GetState[F[_], S, U](f: S => Option[Stream[F, Event[F, S, U]]])
      extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }
  // Generic event to put a function in the main Process process, which changes the state
  // depending on the current state
  case class ModifyState[F[_], S, U](f: Handle[F, S, Event[F, S, U], U])
      extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }
  // Calls a user given function in the main Stream process to stop an Action.
  // It sets the Sequence to be stopped. The user function is called only if the Sequence is running.
  case class ActionStop[F[_], S, U](
    id: Observation.Id,
    f:  S => Option[Stream[F, Event[F, S, U]]]
  ) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  // Uses `cont` to resume execution of a paused Action. If the Action is not paused, it does nothing.
  case class ActionResume[F[_], S, U](id: Observation.Id, i: Int, cont: Stream[F, Result])
      extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  case class LogDebug[F[_], S, U](msg: String, timestamp: Instant) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  case class LogInfo[F[_], S, U](msg: String, timestamp: Instant) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  case class LogWarning[F[_], S, U](msg: String, timestamp: Instant) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  case class LogError[F[_], S, U](msg: String, timestamp: Instant) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }

  case class Pure[F[_], S, U](ev: U) extends UserEvent[F, S, U] {
    val user: Option[User] = None
  }
}
