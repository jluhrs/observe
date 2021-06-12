// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.web.client.model

import cats.tests.CatsSuite
import observe.model.Observation
import scala.collection.immutable.SortedMap
import observe.model.enum.Instrument
import observe.model.SequenceMetadata
import observe.model.SequenceView
import observe.model.SequencesQueue
import observe.model.Conditions
import observe.model.SequenceState
import observe.model.SystemOverrides
import observe.web.client.model.PreviewSequenceTab
import observe.web.client.model.SequencesOnDisplay
import observe.web.client.model.CalibrationQueueTab
import observe.web.client.model.InstrumentSequenceTab

/**
 * Tests Sequences on display class
 */
final class SequencesOnDisplaySpec extends CatsSuite with ArbitrariesWebClient {
  test("Starts with just the calibration tab") {
    SequencesOnDisplay.Empty.tabs.length should be(1)
  }
  test("Support adding preview") {
    val m   = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val s   = SequenceView(Observation.Id.unsafeFromString("GS-2018A-Q-0-1"),
                         m,
                         SequenceState.Idle,
                         SystemOverrides.AllEnabled,
                         Nil,
                         None
    )
    val sod =
      SequencesOnDisplay.Empty.previewSequence(s.metadata.instrument, s)
    sod.tabs.length should be(2)
  }
  test("Focus on preview") {
    val m   = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val s   = SequenceView(Observation.Id.unsafeFromString("GS-2018A-Q-0-1"),
                         m,
                         SequenceState.Idle,
                         SystemOverrides.AllEnabled,
                         Nil,
                         None
    )
    val sod =
      SequencesOnDisplay.Empty.previewSequence(s.metadata.instrument, s)
    // focus on preview
    sod.tabs.focus.isPreview should be(true)
  }
  test("Unset preview") {
    val m   = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val s   = SequenceView(Observation.Id.unsafeFromString("GS-2018A-Q-0-1"),
                         m,
                         SequenceState.Idle,
                         SystemOverrides.AllEnabled,
                         Nil,
                         None
    )
    val sod =
      SequencesOnDisplay.Empty.previewSequence(s.metadata.instrument, s)
    // Unset unknow
    sod.tabs.focus.isPreview should be(true)
  }
  test("Replace preview") {
    val obsId = Observation.Id.unsafeFromString("GS-2018A-Q-0-1")
    val m     = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val s     = SequenceView(obsId, m, SequenceState.Idle, SystemOverrides.AllEnabled, Nil, None)
    val sod   =
      SequencesOnDisplay.Empty.previewSequence(s.metadata.instrument, s)

    // Remove unknown
    val sod2 =
      sod.unsetPreviewOn(Observation.Id.unsafeFromString("GS-2018A-Q-0-3"))
    sod2.tabs.length should be(2)
    sod2 === sod should be(true)

    // Remove known
    val sod3 = sod.unsetPreviewOn(obsId)
    sod3.tabs.length should be(1)
    sod3.tabs.focus should matchPattern { case CalibrationQueueTab(_, _) =>
    }
  }
  test("Update loaded") {
    val m      = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val obsId  = Observation.Id.unsafeFromString("GS-2018A-Q-0-1")
    val s      = SequenceView(obsId, m, SequenceState.Idle, SystemOverrides.AllEnabled, Nil, None)
    val queue  = List(s)
    val loaded = Map((Instrument.Gpi: Instrument) -> obsId)
    val sod    = SequencesOnDisplay.Empty.updateFromQueue(
      SequencesQueue(loaded, Conditions.Default, None, SortedMap.empty, queue)
    )
    sod.tabs.length should be(2)
    sod.tabs.toList.lift(1) should matchPattern {
      case Some(InstrumentSequenceTab(_, Right(s), _, _, _, _, _)) if s.id === obsId =>
    }
  }
  test("Add preview with loaded") {
    val m      = SequenceMetadata(Instrument.Gpi, None, "Obs")
    val obsId  = Observation.Id.unsafeFromString("GS-2018A-Q-0-1")
    val s      = SequenceView(obsId, m, SequenceState.Idle, SystemOverrides.AllEnabled, Nil, None)
    val queue  = List(s)
    val loaded = Map((Instrument.Gpi: Instrument) -> obsId)
    val sod    = SequencesOnDisplay.Empty.updateFromQueue(
      SequencesQueue(loaded, Conditions.Default, None, SortedMap.empty, queue)
    )

    val obs2 = Observation.Id.unsafeFromString("GS-2018A-Q-0-2")
    val s2   = SequenceView(obs2, m, SequenceState.Idle, SystemOverrides.AllEnabled, Nil, None)
    val sod2 = sod.previewSequence(s2.metadata.instrument, s2)

    sod2.tabs.length should be(3)
    sod2.tabs.toList.lift(2) should matchPattern {
      case Some(InstrumentSequenceTab(_, Right(s), _, _, _, _, _)) if s.id === obsId =>
    }
    sod2.tabs.focus should matchPattern {
      case PreviewSequenceTab(s, _, _, _) if s.id === obs2 =>
    }
  }
}