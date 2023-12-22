// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.model

import cats.Eq
import cats.derived.*
import cats.syntax.option.*
import crystal.Pot
import crystal.react.View
import eu.timepit.refined.cats.given
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.core.model.Observation
import lucuma.core.model.sequence.Step
import lucuma.ui.sso.UserVault
import monocle.Focus
import monocle.Lens
import observe.model.Conditions
import observe.model.Environment
import observe.model.ExecutionState
import observe.model.Observer
import observe.model.Operator
import observe.model.StepProgress
import observe.ui.model.enums.ClientMode

case class RootModelData(
  userVault:            Option[UserVault],
  readyObservations:    Pot[List[ObsSummary]],
  selectedObservation:  Option[Observation.Id],
  nighttimeObservation: Option[LoadedObservation],
  daytimeObservations:  List[LoadedObservation],
  executionState:       Map[Observation.Id, ExecutionState],
  obsProgress:          Map[Observation.Id, StepProgress],
  userSelectedStep:     Map[Observation.Id, Step.Id],
  conditions:           Conditions,
  observer:             Option[Observer],
  operator:             Option[Operator],
  userSelectionMessage: Option[NonEmptyString],
  log:                  List[NonEmptyString]
) derives Eq:
  // TODO Readonly mode won't depend on user logged or not, but on their permissions.
  // For the moment we are requiring the STAFF role, so all logged users can operate.
  val clientMode: ClientMode = userVault.fold(ClientMode.ReadOnly)(_ => ClientMode.CanOperate)

  val isUserLogged: Boolean = userVault.isDefined

  lazy val readyObservationsMap: Map[Observation.Id, ObsSummary] =
    readyObservations.toOption.orEmpty.map(o => o.obsId -> o).toMap

  lazy val nighttimeDisplayedObservation: Option[ObsSummary] =
    nighttimeObservation
      .map(_.obsId)
      .orElse(selectedObservation)
      .flatMap(readyObservationsMap.get)

  def isObsLocked(obsId: Observation.Id): Boolean =
    executionState.get(obsId).exists(_.isLocked)

  def obsSelectedStep(obsId: Observation.Id): Option[Step.Id] =
    executionState.get(obsId).flatMap(_.runningStepId).orElse(userSelectedStep.get(obsId))

object RootModelData:
  def initial(userVault: Either[Throwable, Option[UserVault]]): RootModelData =
    val vault: Option[UserVault] = userVault.toOption.flatten
    RootModelData(
      userVault = vault,
      readyObservations = Pot.pending,
      selectedObservation = none,
      nighttimeObservation = none,
      daytimeObservations = List.empty,
      executionState = Map.empty,
      obsProgress = Map.empty,
      userSelectedStep = Map.empty,
      conditions = Conditions.Default,
      observer =
        vault.flatMap(v => NonEmptyString.from(v.user.displayName).toOption.map(Observer(_))),
      operator = none,
      userSelectionMessage =
        userVault.left.toOption.map(t => NonEmptyString.unsafeFrom(t.getMessage)),
      log = List.empty
    )

  val userVault: Lens[RootModelData, Option[UserVault]]                        = Focus[RootModelData](_.userVault)
  val readyObservations: Lens[RootModelData, Pot[List[ObsSummary]]]            =
    Focus[RootModelData](_.readyObservations)
  val selectedObservation: Lens[RootModelData, Option[Observation.Id]]         =
    Focus[RootModelData](_.selectedObservation)
  val nighttimeObservation: Lens[RootModelData, Option[LoadedObservation]]     =
    Focus[RootModelData](_.nighttimeObservation)
  val daytimeObservations: Lens[RootModelData, List[LoadedObservation]]        =
    Focus[RootModelData](_.daytimeObservations)
  val executionState: Lens[RootModelData, Map[Observation.Id, ExecutionState]] =
    Focus[RootModelData](_.executionState)
  val obsProgress: Lens[RootModelData, Map[Observation.Id, StepProgress]]      =
    Focus[RootModelData](_.obsProgress)
  val userSelectedStep: Lens[RootModelData, Map[Observation.Id, Step.Id]]      =
    Focus[RootModelData](_.userSelectedStep)
  val conditions: Lens[RootModelData, Conditions]                              = Focus[RootModelData](_.conditions)
  val observer: Lens[RootModelData, Option[Observer]]                          = Focus[RootModelData](_.observer)
  val operator: Lens[RootModelData, Option[Operator]]                          = Focus[RootModelData](_.operator)
  val userSelectionMessage: Lens[RootModelData, Option[NonEmptyString]]        =
    Focus[RootModelData](_.userSelectionMessage)
  val log: Lens[RootModelData, List[NonEmptyString]]                           = Focus[RootModelData](_.log)

case class RootModel(environment: Pot[Environment], data: View[RootModelData])
