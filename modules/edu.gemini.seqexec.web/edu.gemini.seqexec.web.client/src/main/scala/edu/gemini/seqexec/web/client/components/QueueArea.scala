package edu.gemini.seqexec.web.client.components

import diode.data.{Empty, Pot}
import diode.react.ReactPot._
import diode.react._
import edu.gemini.seqexec.web.client.model._
import edu.gemini.seqexec.web.client.semanticui.elements.icon.Icon.{IconChevronLeft, IconChevronRight, IconAttention}
import edu.gemini.seqexec.web.client.semanticui.elements.message.CloseableMessage
import edu.gemini.seqexec.web.common.{SeqexecQueue, SequenceState}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react._

import scalacss.ScalaCssReact._

object QueueTableBody {
  case class Props(queue: ModelProxy[Pot[SeqexecQueue]])

  // Minimum rows to display, pad with empty rows if needed
  val minRows = 5

  def emptyRow(k: String) = {
    val nbsp = "\u00a0"

    <.tr(
      ^.key := k, // React requires unique keys
      <.td(nbsp),
      <.td(nbsp),
      <.td(nbsp),
      <.td(
        SeqexecStyles.notInMobile,
        nbsp)
    )
  }

  def load(p: Props) =
    // Request to load the queue if not present
    Callback.when(p.queue.value.isEmpty)(p.queue.dispatch(UpdatedQueue(Empty)))

  val component = ReactComponentB[Props]("QueueTableBody")
    .render_P( p =>
      <.tbody(
        // Render after data arrives
        p.queue().render( q =>
          q.queue.map(Some.apply).padTo(minRows, None).zipWithIndex.collect {
            case (Some(s), i) =>
              <.tr(
                ^.classSet(
                  "positive" -> (s.state == SequenceState.Running),
                  "negative" -> (s.state == SequenceState.Error)
                ),
                ^.key := s"item.queue.$i",
                <.td(
                  ^.cls := "collapsing",
                  s.id
                ),
                <.td(s.state.toString),
                <.td(s.instrument),
                <.td(
                  SeqexecStyles.notInMobile,
                  s.error.map(_ => <.p(IconAttention, " Error")).getOrElse(<.p("-"))
                )
              )
            case (_, i) =>
              emptyRow(s"item.queue.$i")
          }
        ),
        // Render some rows when pending
        p.queue().renderPending(_ => (0 until minRows).map(i => emptyRow(s"item.queue.$i"))),
        // Render some rows even if it failed
        p.queue().renderFailed(_ => (0 until minRows).map(i => emptyRow(s"item.queue.$i")))
      )
    )
    .componentDidMount($ => load($.props))
    .build

  def apply(p: ModelProxy[Pot[SeqexecQueue]]) = component(Props(p))

}

/**
  * Shows a message when there is an error loading the queue
  */
object LoadingErrorMsg {
  case class Props(queue :ModelProxy[Pot[SeqexecQueue]])

  val component = ReactComponentB[Props]("LoadingErrorMessage")
    .stateless
    .render_P( p =>
      <.div(
        p.queue().renderFailed(_ =>
          CloseableMessage(CloseableMessage.Props(Some("Sorry, there was an error reading the queue from the server"), CloseableMessage.Style.Negative))
        )
      )
    )
    .build

  def apply(p: ModelProxy[Pot[SeqexecQueue]]) = component(Props(p))
}

/**
  * Component for the title of the queue area, including the search component
  */
object QueueAreaTitle {
  val component = ReactComponentB[Unit]("")
    .stateless
    .render(_ =>
      <.div(
        ^.cls := "ui top attached text menu segment",
        <.div(
          ^.cls := "ui header item",
          "Queue"
        ),
        <.div(
          ^.cls := "right menu",
          SeqexecCircuit.connect(_.searchResults)(SequenceSearch(_))
        )
      )
    ).build

  def apply() = component()
}

/**
  * Container for the queue table
  */
object QueueTableSection {
  val component = ReactComponentB[Unit]("QueueTableSection")
    .stateless
    .render( _ =>
      <.div(
        ^.cls := "segment",
        <.table(
          ^.cls := "ui selectable compact celled table unstackable",
          <.thead(
            <.tr(
              <.th("Obs ID "),
              <.th("State"),
              <.th("Instrument"),
              <.th(
                SeqexecStyles.notInMobile,
                "Notes"
              )
            )
          ),
          SeqexecCircuit.connect(_.queue)(QueueTableBody(_)),
          <.tfoot(
            <.tr(
              <.th(
                ^.colSpan := "4",
                <.div(
                  ^.cls := "ui right floated pagination menu",
                  <.a(
                    ^.cls := "icon item",
                    IconChevronLeft
                  ),
                  <.a(
                    ^.cls := "item", "1"),
                  <.a(
                    ^.cls := "item", "2"),
                  <.a(
                    ^.cls := "item", "3"),
                  <.a(
                    ^.cls := "item", "4"),
                  <.a(
                    ^.cls := "icon item",
                    IconChevronRight
                  )
                )
              )
            )
          )
        )
      )
    ).build

  def apply() = component()

}

/**
  * Displays the elements on the queue
  */
object QueueArea {
  case class Props(searchArea: ModelProxy[SearchAreaState])

  val component = ReactComponentB[Props]("QueueArea")
    .stateless
    .render_P(p =>
      <.div(
        ^.cls := "ui raised segments container",
        QueueAreaTitle(),
        <.div(
          ^.cls := "ui attached segment",
          <.div(
            ^.cls := "ui divided grid",
            <.div(
              ^.cls := "stretched row",
              <.div(
                ^.cls := "column",
                ^.classSet(
                  "ten wide" -> (p.searchArea() == SearchAreaOpen),
                  "sixteen wide" -> (p.searchArea() == SearchAreaClosed)
                ),
                // Show a loading indicator if we are waiting for server data
                {
                  // Special equality check to avoid certain UI artifacts
                  implicit val eq = PotEq.seqexecQueueEq
                  SeqexecCircuit.connect(_.queue)(LoadingIndicator("Loading", _))
                },
                // If there was an error on the process display a message
                SeqexecCircuit.connect(_.queue)(LoadingErrorMsg(_)),
                QueueTableSection()
              ),
              p.searchArea() == SearchAreaOpen ?= SequenceSearchResults() // Display the search area if open
            )
          )
        )
      )
    )
    .build

  def apply(p: ModelProxy[SearchAreaState]) = component(Props(p))

}
