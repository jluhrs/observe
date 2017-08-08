package edu.gemini.seqexec.engine

import scalaz._
import scalaz.syntax.std.option._
import scalaz.std.string._

import edu.gemini.seqexec.model.Model.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.seqexec.model.UserDetails
import Result.{OK, Partial, PartialVal, RetVal}
import scalaz.concurrent.Task
import scalaz.stream.Process

/**
  * Anything that can go through the Event Queue.
  */
sealed trait Event
case class EventUser(ue: UserEvent) extends Event
case class EventSystem(se: SystemEvent) extends Event

/**
  * Events generated by the user.
  */
sealed trait UserEvent {
  def user: Option[UserDetails]
  def username: String = ~user.map(_.username)
}

case class Start(id: Sequence.Id, user: Option[UserDetails]) extends UserEvent
case class Pause(id: Sequence.Id, user: Option[UserDetails]) extends UserEvent
case class Load(id: Sequence.Id, sequence: Sequence[Action \/ Result]) extends UserEvent {
  val user = None
}
case class Unload(id: Sequence.Id) extends UserEvent {
  val user = None
}
case class Breakpoint(id: Sequence.Id, user: Option[UserDetails], step: Step.Id, v: Boolean) extends UserEvent
case class SetOperator(name: String, user: Option[UserDetails]) extends UserEvent
case class SetObserver(id: Sequence.Id, user: Option[UserDetails], name: String) extends UserEvent
case class SetConditions(conditions: Conditions, user: Option[UserDetails]) extends UserEvent
case class SetImageQuality(iq: ImageQuality, user: Option[UserDetails]) extends UserEvent
case class SetWaterVapor(wv: WaterVapor, user: Option[UserDetails]) extends UserEvent
case class SetSkyBackground(wv: SkyBackground, user: Option[UserDetails]) extends UserEvent
case class SetCloudCover(cc: CloudCover, user: Option[UserDetails]) extends UserEvent
case object Poll extends UserEvent {
  val user = None
}
case class GetState(f: (Engine.State) => Task[Option[Process[Task, Event]]]) extends UserEvent {
  val user = None
}
case class Log(msg: String) extends UserEvent {
  val user = None
}

/**
  * Events generated internally by the Engine.
  */
sealed trait SystemEvent
case class Completed[R<:RetVal](id: Sequence.Id, i: Int, r: OK[R]) extends SystemEvent
case class PartialResult[R<:PartialVal](id: Sequence.Id, i: Int, r: Partial[R]) extends SystemEvent
case class Failed(id: Sequence.Id, i: Int, e: Result.Error) extends SystemEvent
case class Busy(id: Sequence.Id) extends SystemEvent
case class Executed(id: Sequence.Id) extends SystemEvent
case class Executing(id: Sequence.Id) extends SystemEvent
case class Finished(id: Sequence.Id) extends SystemEvent

object Event {

  def start(id: Sequence.Id, user: UserDetails): Event = EventUser(Start(id, user.some))
  def pause(id: Sequence.Id, user: UserDetails): Event = EventUser(Pause(id, user.some))
  def load(id: Sequence.Id, sequence: Sequence[Action \/ Result]): Event = EventUser(Load(id, sequence))
  def unload(id: Sequence.Id): Event = EventUser(Unload(id))
  def breakpoint(id: Sequence.Id, user: UserDetails, step: Step.Id, v: Boolean): Event = EventUser(Breakpoint(id, user.some, step, v))
  def setOperator(name: String, user: UserDetails): Event = EventUser(SetOperator(name, user.some))
  def setObserver(id: Sequence.Id, user: UserDetails, name: String): Event = EventUser(SetObserver(id, user.some, name))
  def setConditions(conditions: Conditions, user: UserDetails): Event = EventUser(SetConditions(conditions, user.some))
  def setImageQuality(iq: ImageQuality, user: UserDetails): Event = EventUser(SetImageQuality(iq, user.some))
  def setWaterVapor(wv: WaterVapor, user: UserDetails): Event = EventUser(SetWaterVapor(wv, user.some))
  def setSkyBackground(sb: SkyBackground, user: UserDetails): Event = EventUser(SetSkyBackground(sb, user.some))
  def setCloudCover(cc: CloudCover, user: UserDetails): Event = EventUser(SetCloudCover(cc, user.some))
  val poll: Event = EventUser(Poll)
  def getState(f: (Engine.State) => Task[Option[Process[Task, Event]]]): Event = EventUser(GetState(f))
  def logMsg(msg: String): Event = EventUser(Log(msg))

  def failed(id: Sequence.Id, i: Int, e: Result.Error): Event = EventSystem(Failed(id, i, e))
  def completed[R<:RetVal](id: Sequence.Id, i: Int, r: OK[R]): Event = EventSystem(Completed(id, i, r))
  def partial[R<:PartialVal](id: Sequence.Id, i: Int, r: Partial[R]): Event = EventSystem(PartialResult(id, i, r))
  def busy(id: Sequence.Id): Event = EventSystem(Busy(id))
  def executed(id: Sequence.Id): Event = EventSystem(Executed(id))
  def executing(id: Sequence.Id): Event = EventSystem(Executing(id))
  def finished(id: Sequence.Id): Event = EventSystem(Finished(id))

}
