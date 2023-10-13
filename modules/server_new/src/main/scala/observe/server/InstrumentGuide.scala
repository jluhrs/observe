// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server

import lucuma.core.enums.Instrument
import squants.space.Length

trait InstrumentGuide {
  def instrument: Instrument
  def oiOffsetGuideThreshold: Option[Length]
}
