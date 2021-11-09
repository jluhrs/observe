// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.web.client.circuit

import cats.Eq
import cats.syntax.all._
import monocle.Getter
import observe.model.Observation
import observe.web.client.components.sequence.steps.StepConfigTable
import observe.web.client.components.sequence.steps.StepsTable
import observe.web.client.model._
import web.client.table._

final case class StepsTableAndStatusFocus(
  status:           ClientStatus,
  displayName:      Option[String],
  stepsTable:       Option[StepsTableFocus],
  tableState:       TableState[StepsTable.TableColumn],
  configTableState: TableState[StepConfigTable.TableColumn]
)

object StepsTableAndStatusFocus {
  implicit val eq: Eq[StepsTableAndStatusFocus] =
    Eq.by(x => (x.status, x.displayName, x.stepsTable, x.tableState, x.configTableState))

  def stepsTableAndStatusFocusG(
    id: Observation.Id
  ): Getter[ObserveAppRootModel, StepsTableAndStatusFocus] = {

    val displayNames =
      ObserveAppRootModel.userLoginFocus.asGetter
    ClientStatus.clientStatusFocusL.asGetter
      .zip(
        StepsTableFocus
          .stepsTableG(id)
          .zip(
            ObserveAppRootModel
              .stepsTableStateL(id)
              .asGetter
              .zip(ObserveAppRootModel.configTableStateL.asGetter.zip(displayNames))
          )
      ) >>> { case (s, (f, (a, (t, dn)))) =>
      StepsTableAndStatusFocus(s,
                               dn.displayName,
                               f,
                               a.getOrElse(StepsTable.State.InitialTableState),
                               t
      )
    }
  }

}
