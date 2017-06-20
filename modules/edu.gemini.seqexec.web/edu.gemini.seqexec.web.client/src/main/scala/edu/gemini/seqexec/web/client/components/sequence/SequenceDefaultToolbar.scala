package edu.gemini.seqexec.web.client.components.sequence

import edu.gemini.seqexec.model.Model.{SequenceState, SequenceView}
import edu.gemini.seqexec.web.client.model._
import edu.gemini.seqexec.web.client.model.ModelOps._
import edu.gemini.seqexec.web.client.semanticui.elements.button.Button
import edu.gemini.seqexec.web.client.semanticui.elements.input.InputEV
import edu.gemini.seqexec.web.client.semanticui.elements.label.Label
import edu.gemini.seqexec.web.client.semanticui.elements.icon.Icon._
import japgolly.scalajs.react.extra.{StateSnapshot, TimerSupport}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent, ScalazReact}
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.component.Scala.Unmounted
import org.scalajs.dom.html.Div

import scalaz.syntax.equal._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.string._
import scalaz.std.option._
import scala.concurrent.duration._

object SequenceObserverField {
  case class Props(s: SequenceView, isLogged: Boolean)

  case class State(currentText: Option[String])

  class Backend(val $: BackendScope[Props, State]) extends TimerSupport {
    def updateObserver(s: SequenceView, name: String): Callback =
      $.props >>= { p => Callback.when(p.isLogged)(Callback(SeqexecCircuit.dispatch(UpdateObserver(s, name)))) }

    def updateState(value: String): Callback =
      $.state >>= { s => Callback.when(!s.currentText.contains(value))($.modState(_.copy(currentText = Some(value)))) }

    def submitIfChanged: Callback =
      ($.state zip $.props) >>= {
        case (s, p) => Callback.when(s.currentText =/= p.s.metadata.observer)(updateObserver(p.s, s.currentText.getOrElse("")))
      }

    def setupTimer: Callback =
      // Every 2 seconds check if the field has changed and submit
      setInterval(submitIfChanged, 2.second)

    def render(p: Props, s: State): VdomTagOf[Div] = {
      val observerEV = StateSnapshot(~s.currentText)(updateState)
      val idObserverText = p.isLogged ? "Observer" | s"Id: ${p.s.id}, Observer: ${~s.currentText}"
      <.div(
        ^.cls := "ui form",
          Label(Label.Props("Id:", basic = true, color = "red".some)),
          Label(Label.Props(p.s.id, basic = true)),
          Label(Label.Props("Observer:", basic = true, color = "red".some)),
          Label(Label.Props(~s.currentText, basic = true)),
          <.div(
            ^.cls := "field",
            ^.classSet(
              "required" -> p.isLogged
            ),
            Label(Label.Props(idObserverText, basic = true)),
            InputEV(InputEV.Props(
              p.s.metadata.instrument + ".observer",
              p.s.metadata.instrument + ".observer",
              observerEV,
              placeholder = "Observer...",
              disabled = !p.isLogged,
              onBlur = _ => submitIfChanged))
          ).when(p.isLogged)
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("SequenceObserverField")
    .initialState(State(None))
    .renderBackend[Backend]
    .configure(TimerSupport.install)
    .componentWillMount(f => f.backend.$.props >>= {p => f.backend.updateState(p.s.metadata.observer.getOrElse(""))})
    .componentDidMount(_.backend.setupTimer)
    .componentWillReceiveProps { f =>
      val observer = f.nextProps.s.metadata.observer
      // Update the observer field
      Callback.when((observer =/= f.state.currentText) && observer.nonEmpty)(f.modState(_.copy(currentText = observer)))
    }
    .build

  def apply(p: Props): Unmounted[Props, State, Backend] = component(p)
}

object SequenceDefaultToolbar {
  case class Props(s: SequenceView, status: ClientStatus, nextStepToRun: Int)
  case class State(runRequested: Boolean, pauseRequested: Boolean, syncRequested: Boolean)
  private val ST = ReactS.Fix[State]

  def requestRun(s: SequenceView): ScalazReact.ReactST[CallbackTo, State, Unit] =
    ST.retM(Callback { SeqexecCircuit.dispatch(RequestRun(s)) }) >> ST.mod(_.copy(runRequested = true, pauseRequested = false, syncRequested = false)).liftCB

  def requestSync(s: SequenceView): ScalazReact.ReactST[CallbackTo, State, Unit] =
    ST.retM(Callback { SeqexecCircuit.dispatch(RequestSync(s)) }) >> ST.mod(_.copy(runRequested = false, pauseRequested = false, syncRequested = true)).liftCB

  def requestPause(s: SequenceView): ScalazReact.ReactST[CallbackTo, State, Unit] =
    ST.retM(Callback { SeqexecCircuit.dispatch(RequestPause(s)) }) >> ST.mod(_.copy(runRequested = false, pauseRequested = true, syncRequested = false)).liftCB

  private def component = ScalaComponent.builder[Props]("SequencesDefaultToolbar")
    .initialState(State(runRequested = false, pauseRequested = false, syncRequested = false))
    .renderPS{ ($, p, s) =>
      val isLogged = p.status.isLogged
      <.div(
        ^.cls := "ui column grid",
        <.div(
          ^.cls := "ui row",
          <.div(
            ^.cls := "left column bottom aligned eight wide computer ten wide tablet only",
            <.div(
              ^.cls := "ui form",
              <.div(
                ^.cls := "field",
                Label(Label.Props(s"Obs. Id: ${p.s.id}"))
              ).when(isLogged),
              <.div(
                ^.cls := "field",
                Label(Label.Props(s"Name: ${p.s.metadata.name}")).when(isLogged)
              )
            ),
            <.h3(
              ^.cls := "ui green header",
              "Sequence complete"
            ).when(isLogged && p.s.status === SequenceState.Completed),
            Button(
              Button.Props(
                icon = Some(IconPlay),
                labeled = true,
                onClick = $.runState(requestRun(p.s)),
                color = Some("blue"),
                dataTooltip = Some(s"${p.s.isPartiallyExecuted ? "Continue" | "Run"} the sequence from the step ${p.nextStepToRun + 1}"),
                disabled = !p.status.isConnected || s.runRequested || s.syncRequested),
              s"${p.s.isPartiallyExecuted ? "Continue" | "Run"} from step ${p.nextStepToRun + 1}"
            ).when(isLogged && p.s.hasError),
            Button(
              Button.Props(
                icon = Some(IconRefresh),
                onClick = $.runState(requestSync(p.s)),
                color = Some("purple"),
                dataTooltip = Some(s"Sync sequence"),
                disabled = !p.status.isConnected || s.runRequested || s.syncRequested),
              s" Sync"
            ).when(isLogged && p.s.status === SequenceState.Idle),
            Button(
              Button.Props(
                icon = Some(IconPlay),
                labeled = true,
                onClick = $.runState(requestRun(p.s)),
                color = Some("blue"),
                dataTooltip = Some(s"${p.s.isPartiallyExecuted ? "Continue" | "Run"} the sequence from the step ${p.nextStepToRun + 1}"),
                disabled = !p.status.isConnected || s.runRequested || s.syncRequested),
              s"${p.s.isPartiallyExecuted ? "Continue" | "Run"} from step ${p.nextStepToRun + 1}"
            ).when(isLogged && p.s.status === SequenceState.Idle),
            Button(
              Button.Props(
                icon = Some(IconPause),
                labeled = true,
                onClick = $.runState(requestPause(p.s)),
                color = Some("teal"),
                dataTooltip = Some("Pause the sequence after the current step completes"),
                disabled = !p.status.isConnected || s.pauseRequested || s.syncRequested),
              "Pause"
            ).when(isLogged && p.s.status === SequenceState.Running),
            Button(
              Button.Props(
                icon = Some(IconPlay),
                labeled = true,
                onClick = $.runState(requestPause(p.s)),
                color = Some("teal"),
                disabled = !p.status.isConnected || s.syncRequested),
              "Continue from step 1"
            ).when(isLogged && p.s.status === SequenceState.Paused)
          ),
          <.div(
            ^.cls := "right column",
            ^.classSet(
              "eight wide computer six wide tablet sixteen wide mobile" -> isLogged,
              "sixteen wide" -> !isLogged
            ),
            SequenceObserverField(SequenceObserverField.Props(p.s, isLogged))
          )
        )
    )
    }.componentWillReceiveProps { f =>
      // Update state of run requested depending on the run state
      Callback.when(f.nextProps.s.status === SequenceState.Running && f.state.runRequested)(f.modState(_.copy(runRequested = false)))
    }.build

  def apply(p: Props): Unmounted[Props, State, Unit] = component(p)
}
