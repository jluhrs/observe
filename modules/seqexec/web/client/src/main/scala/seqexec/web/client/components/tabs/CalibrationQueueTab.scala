// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.web.client.components.tabs

import cats.implicits._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react._
import seqexec.model.enum.BatchExecState
import seqexec.web.client.model.Pages._
import seqexec.web.client.model.CalibrationQueueTabActive
import seqexec.web.client.model.TabSelected
import seqexec.web.client.semanticui._
import seqexec.web.client.semanticui.elements.label.Label
import seqexec.web.client.semanticui.elements.icon.Icon._
import seqexec.web.client.components.SeqexecStyles
import seqexec.web.client.reusability._
import web.client.style._

object CalibrationQueueTab {
  final case class Props(router: RouterCtl[SeqexecPages],
                         tab:    CalibrationQueueTabActive)

  implicit val propsReuse: Reusability[Props] =
    Reusability.by(x => (x.tab.active, x.tab.calibrationTab.state))

  private def showCalibrationQueue(p: Props, page: SeqexecPages)(e: ReactEvent): Callback =
    // prevent default to avoid the link jumping
    e.preventDefaultCB *>
      // Request to display the selected sequence
      p.router
        .setUrlAndDispatchCB(page)
        .unless(p.tab.active === TabSelected.Selected) *>
      Callback.empty

  private def linkTo(p: Props, page: SeqexecPages)(mod: TagMod*) = {
    val active = p.tab.active

    <.a(
      ^.href := p.router.urlFor(page).value,
      ^.onClick ==> showCalibrationQueue(p, page),
      ^.cls := "item",
      ^.classSet(
        "active" -> (active === TabSelected.Selected)
      ),
      SeqexecStyles.tab,
      dataTab := "daycalqueue",
      SeqexecStyles.inactiveTabContent.when(active === TabSelected.Background),
      SeqexecStyles.activeTabContent.when(active === TabSelected.Selected),
      mod.toTagMod
    )
  }

  private val component = ScalaComponent
    .builder[Props]("CalibrationQueueTab")
    .stateless
    .render_P { p =>
      val icon = p.tab.calibrationTab.state match {
        case BatchExecState.Running =>
          IconCircleNotched.copyIcon(loading = true)
        case BatchExecState.Completed => IconCheckmark
        case _                        => IconSelectedRadio
      }

      val color = p.tab.calibrationTab.state match {
        case BatchExecState.Running   => "orange"
        case BatchExecState.Completed => "green"
        case _                        => "grey"
      }

      val tabContent: VdomNode =
        <.div(
          SeqexecStyles.tabLabel,
          <.div(SeqexecStyles.activeInstrumentLabel, "Daytime Queue"),
          Label(
            Label.Props(p.tab.calibrationTab.state.show,
                        color       = color.some,
                        icon        = icon.some,
                        extraStyles = List(SeqexecStyles.labelPointer)))
        )

      linkTo(p, CalibrationQueuePage)(tabContent)
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(p: Props): Unmounted[Props, Unit, Unit] = component(p)
}
