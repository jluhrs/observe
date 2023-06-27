// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.model

import cats.Eq
import cats.derived.*
import monocle.Focus
import monocle.Lens
import observe.model.enums.ActionStatus
import observe.model.enums.NsAction
import org.typelevel.cats.time.given

import java.time.Duration

case class NsRunningState(action: NsAction, sub: NsSubexposure) derives Eq

case class NodAndShuffleStatus(
  observing:         ActionStatus,
  totalExposureTime: Duration,
  nodExposureTime:   Duration,
  cycles:            NsCycles,
  state:             Option[NsRunningState]
) derives Eq

object NodAndShuffleStatus:
  val observing: Lens[NodAndShuffleStatus, ActionStatus]       = Focus[NodAndShuffleStatus](_.observing)
  val totalExposureTime: Lens[NodAndShuffleStatus, Duration]   =
    Focus[NodAndShuffleStatus](_.totalExposureTime)
  val nodExposureTime: Lens[NodAndShuffleStatus, Duration]     =
    Focus[NodAndShuffleStatus](_.nodExposureTime)
  val cycles: Lens[NodAndShuffleStatus, NsCycles]              = Focus[NodAndShuffleStatus](_.cycles)
  val state: Lens[NodAndShuffleStatus, Option[NsRunningState]] = Focus[NodAndShuffleStatus](_.state)
