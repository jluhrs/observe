// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server

import cats.syntax.all.*
import edu.gemini.spModel.ao.AOConstants.*
import lucuma.core.math.Wavelength
import edu.gemini.spModel.gemini.altair.AltairConstants
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{Wavelength => GNIRSWavelength}
import edu.gemini.spModel.obscomp.InstConstants.*
import observe.model.StepState
import observe.model.enums.Instrument
import CleanConfig.extractItem
import ConfigUtilOps._
import ObserveFailure.Unexpected
import ObserveFailure.UnrecognizedInstrument
import observe.server.flamingos2.Flamingos2
import observe.server.ghost.Ghost
import observe.server.gmos.GmosNorth
import observe.server.gmos.GmosSouth
import observe.server.gnirs.*
import observe.server.gpi.Gpi
import observe.server.gsaoi.*
import observe.server.nifs.*
import observe.server.niri.*

trait SequenceConfiguration {
  def extractInstrument(config: CleanConfig): Either[ObserveFailure, Instrument] =
    config
      .extractInstAs[String](INSTRUMENT_NAME_PROP)
      .adaptExtractFailure
      .flatMap {
        case Flamingos2.name => Instrument.F2.asRight
        case GmosSouth.name  => Instrument.GmosS.asRight
        case GmosNorth.name  => Instrument.GmosN.asRight
        case Gnirs.name      => Instrument.Gnirs.asRight
        case Gpi.name        => Instrument.Gpi.asRight
        case Ghost.name      => Instrument.Ghost.asRight
        case Niri.name       => Instrument.Niri.asRight
        case Nifs.name       => Instrument.Nifs.asRight
        case Gsaoi.name      => Instrument.Gsaoi.asRight
        case ins             => UnrecognizedInstrument(s"inst $ins").asLeft
      }

  def extractStatus(config: CleanConfig): StepState =
    config
      .extractObsAs[String](STATUS_PROP)
      .map {
        case "ready"    => StepState.Pending
        case "complete" => StepState.Completed
        case "skipped"  => StepState.Skipped
        case kw         => StepState.Failed("Unexpected status keyword: " ++ kw)
      }
      .getOrElse(StepState.Failed("Logical error reading step status"))

  /**
   * Attempts to extract the Wavelength from the sequence. The value is not always present thus we
   * can get a None Also errors reading the value are possible thus we produce an Either
   */
  def extractWavelength(config: CleanConfig): Either[ObserveFailure, Option[Wavelength]] =
    if (!config.containsKey(OBSERVING_WAVELENGTH_KEY))
      none[Wavelength].asRight
    else
      // Gmos uses String
      config
        .extractAs[String](OBSERVING_WAVELENGTH_KEY)
        .flatMap(v => Either.catchNonFatal(v.toDouble).leftMap(_ => ContentError(v)))
        .map(w => Wavelength.fromMicrometers(w.toInt))
        .orElse {
          // GNIRS uses its own wavelength!!
          config
            .extractAs[GNIRSWavelength](OBSERVING_WAVELENGTH_KEY)
            .map(w => Wavelength.fromMicrometers(w.doubleValue().toInt))
        }
        .orElse {
          // Maybe we use ocs Wavelength
          config
            .extractAs[Wavelength](OBSERVING_WAVELENGTH_KEY)
            .map(_.some)
        }
        .orElse {
          // Just in case
          config
            .extractAs[java.lang.Double](OBSERVING_WAVELENGTH_KEY)
            .map(w => Wavelength.fromMicrometers(w.toInt))
        }
        .leftMap { _ =>
          Unexpected(
            s"Error reading wavelength ${config.itemValue(OBSERVING_WAVELENGTH_KEY)}: ${config.itemValue(OBSERVING_WAVELENGTH_KEY).getClass}"
          ): ObserveFailure
        }

  def calcStepType(config: CleanConfig, isNightSeq: Boolean): Either[ObserveFailure, StepType] = {
    def extractGaos(inst: Instrument): Either[ObserveFailure, StepType] =
      config.extractAs[String](AO_SYSTEM_KEY) match {
        case Left(ConfigUtilOps.ConversionError(_, _))              =>
          Unexpected("Unable to get AO system from sequence").asLeft
        case Left(ConfigUtilOps.ContentError(_))                    =>
          Unexpected("Logical error").asLeft
        case Left(ConfigUtilOps.KeyNotFound(_))                     =>
          StepType.CelestialObject(inst).asRight
        case Right(AltairConstants.SYSTEM_NAME_PROP)                =>
          StepType.AltairObs(inst).asRight
        case Right(edu.gemini.spModel.gemini.gems.Gems.SYSTEM_NAME) =>
          StepType.Gems(inst).asRight
        case _                                                      =>
          Unexpected("Logical error reading AO system name").asLeft
      }

    (config
       .extractObsAs[String](OBSERVE_TYPE_PROP)
       .leftMap(explainExtractError),
     extractInstrument(config)
    ).mapN { (obsType, inst) =>
      obsType match {
        case SCIENCE_OBSERVE_TYPE                                    => extractGaos(inst)
        case BIAS_OBSERVE_TYPE | DARK_OBSERVE_TYPE                   =>
          inst match {
            case Instrument.GmosN | Instrument.GmosS => StepType.ExclusiveDarkOrBias(inst).asRight
            case _                                   => StepType.DarkOrBias(inst).asRight
          }
        case FLAT_OBSERVE_TYPE | ARC_OBSERVE_TYPE | CAL_OBSERVE_TYPE =>
          if (isNightSeq && inst.hasOI) StepType.NightFlatOrArc(inst).asRight
          else StepType.FlatOrArc(inst).asRight
        case _                                                       => Unexpected("Unknown step type " + obsType).asLeft
      }
    }.flatten
  }

}

object SequenceConfiguration extends SequenceConfiguration
