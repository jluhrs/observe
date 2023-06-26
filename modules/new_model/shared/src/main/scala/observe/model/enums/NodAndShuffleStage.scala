// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.model.enums

import cats.Eq
import cats.data.NonEmptyList
import cats.derived.*
import lucuma.core.util.Enumerated

enum NodAndShuffleStage(val symbol: Symbol) derives Eq:
  case StageA extends NodAndShuffleStage(Symbol("A"))
  case StageB extends NodAndShuffleStage(Symbol("B"))

object NodAndShuffleStage:
  // The sequence of nod and shuffle is always BAAB,
  // In principle we'd expect the OT to send the sequence but instead the
  // sequence is hardcoded in the observe and we only read the positions from
  // the OT
  val NsSequence: NonEmptyList[NodAndShuffleStage] =
    NonEmptyList.of(
      NodAndShuffleStage.StageB,
      NodAndShuffleStage.StageA,
      NodAndShuffleStage.StageA,
      NodAndShuffleStage.StageB
    )
