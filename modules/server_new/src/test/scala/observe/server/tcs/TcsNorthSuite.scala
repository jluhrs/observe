// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server.tcs

import cats.syntax.all.*
import coulomb.Quantity
import coulomb.units.accepted.Millimeter
import lucuma.core.enums.GuideProbe
import lucuma.core.enums.Instrument
import lucuma.core.enums.LightSinkName
import lucuma.core.enums.StepGuideState
import lucuma.core.math.Offset
import lucuma.core.model.sequence.TelescopeConfig
import observe.common.ObsQueriesGQL.ObsQuery.Data
import observe.common.ObsQueriesGQL.ObsQuery.Data.Observation.TargetEnvironment.GuideEnvironment
import observe.common.ObsQueriesGQL.ObsQuery.Data.Observation.TargetEnvironment.GuideEnvironment.GuideTargets
import observe.server.InstrumentGuide
import observe.server.tcs.TcsController.LightPath
import observe.server.tcs.TcsController.LightSource

class TcsNorthSuite extends munit.FunSuite {

  test("SeqTranslate extracts guide state") {
    // OIWFS target and guide enabled
    assertEquals(
      TcsNorth
        .config(
          new InstrumentGuide {
            override def instrument: Instrument                                       = Instrument.GmosNorth
            override def oiOffsetGuideThreshold: Option[Quantity[Double, Millimeter]] = none
          },
          Data.Observation
            .TargetEnvironment(none, GuideEnvironment(List(GuideTargets(GuideProbe.GmosOIWFS)))),
          TelescopeConfig.Default,
          LightPath(LightSource.Sky, LightSinkName.Gmos),
          none
        )
        .guideWithOI,
      StepGuideState.Enabled.some
    )
    // No OIWFS target
    assertEquals(
      TcsNorth
        .config(
          new InstrumentGuide {
            override def instrument: Instrument = Instrument.GmosNorth

            override def oiOffsetGuideThreshold: Option[Quantity[Double, Millimeter]] = none
          },
          Data.Observation
            .TargetEnvironment(none, GuideEnvironment(List.empty)),
          TelescopeConfig.Default,
          LightPath(LightSource.Sky, LightSinkName.Gmos),
          none
        )
        .guideWithOI,
      none
    )
    // OIWFS target but guide disabled
    assertEquals(
      TcsNorth
        .config(
          new InstrumentGuide {
            override def instrument: Instrument                                       = Instrument.GmosNorth
            override def oiOffsetGuideThreshold: Option[Quantity[Double, Millimeter]] = none
          },
          Data.Observation
            .TargetEnvironment(none, GuideEnvironment(List(GuideTargets(GuideProbe.GmosOIWFS)))),
          TelescopeConfig(Offset.Zero, StepGuideState.Disabled),
          LightPath(LightSource.Sky, LightSinkName.Gmos),
          none
        )
        .guideWithOI,
      StepGuideState.Disabled.some
    )

  }

}
