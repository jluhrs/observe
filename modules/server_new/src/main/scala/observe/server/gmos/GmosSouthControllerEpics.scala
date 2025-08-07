// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server.gmos

import cats.effect.*
import lucuma.core.enums.*
import observe.server.EpicsCodex.EncodeEpicsValue
import observe.server.gmos.GmosController.Config.BuiltinROI
import observe.server.gmos.GmosController.GmosSite
import observe.server.gmos.GmosControllerEpics.ROIValues
import org.typelevel.log4cats.Logger

object GmosSouthEncoders extends GmosControllerEpics.Encoders[GmosSite.South.type] {
  override val disperser: EncodeEpicsValue[GmosSite.Grating[GmosSite.South.type], String] =
    EncodeEpicsValue {
      case GmosSouthGrating.B1200_G5321 => "B1200+_G5321"
      case GmosSouthGrating.R831_G5322  => "R831+_G5322"
      case GmosSouthGrating.R600_G5324  => "R600+_G5324"
      case GmosSouthGrating.R400_G5325  => "R400+_G5325"
      case GmosSouthGrating.R150_G5326  => "R150+_G5326"
      case GmosSouthGrating.B480_G5327  => "B480+_G5327"
    }

  override val fpu: EncodeEpicsValue[GmosSite.FPU[GmosSite.South.type], String] =
    EncodeEpicsValue {
      case GmosSouthFpu.LongSlit_0_25 => "0.25arcsec"
      case GmosSouthFpu.LongSlit_0_50 => "0.5arcsec"
      case GmosSouthFpu.LongSlit_0_75 => "0.75arcsec"
      case GmosSouthFpu.LongSlit_1_00 => "1.0arcsec"
      case GmosSouthFpu.LongSlit_1_50 => "1.5arcsec"
      case GmosSouthFpu.LongSlit_2_00 => "2.0arcsec"
      case GmosSouthFpu.LongSlit_5_00 => "5.0arcsec"
      case GmosSouthFpu.Ifu2Slits     => "IFU-2"
      case GmosSouthFpu.IfuRed        => "IFU-B"
      case GmosSouthFpu.IfuBlue       => "IFU-R"
      case GmosSouthFpu.IfuNS2Slits   => "IFU-NS-2"
      case GmosSouthFpu.IfuNSBlue     => "IFU-NS-B"
      case GmosSouthFpu.IfuNSRed      => "IFU-NS-R"
      case GmosSouthFpu.Ns1           => "NS0.5arcsec"
      case GmosSouthFpu.Ns2           => "NS0.75arcsec"
      case GmosSouthFpu.Ns3           => "NS1.0arcsec"
      case GmosSouthFpu.Ns4           => "NS1.5arcsec"
      case GmosSouthFpu.Ns5           => "NS2.0arcsec"
    }

  override val filter
    : EncodeEpicsValue[Option[GmosSite.Filter[GmosSite.South.type]], (String, String)] =
    EncodeEpicsValue {
      _.map {
        case GmosSouthFilter.CaT              => ("open1-6", "CaT_G0333")
        case GmosSouthFilter.GG455            => ("GG455_G0329", "open2-8")
        case GmosSouthFilter.GPrime           => ("open1-6", "g_G0325")
        case GmosSouthFilter.GPrime_GG455     => ("GG455_G0329", "g_G0325")
        case GmosSouthFilter.GPrime_OG515     => ("OG515_G0330", "g_G0325")
        case GmosSouthFilter.Ha               => ("open1-6", "Ha_G0336")
        case GmosSouthFilter.HaC              => ("open1-6", "HaC_G0337")
        case GmosSouthFilter.HartmannA_RPrime => ("HartmannA_G0337", "r_G0326")
        case GmosSouthFilter.HartmannB_RPrime => ("HartmannB_G0338", "r_G0326")
        case GmosSouthFilter.HeII             => ("open1-6", "HeII_G0340")
        case GmosSouthFilter.HeIIC            => ("open1-6", "HeIIC_G0341")
        case GmosSouthFilter.IPrime           => ("open1-6", "i_G0327")
        case GmosSouthFilter.IPrime_CaT       => ("CaT_G0333", "i_G0327")
        case GmosSouthFilter.IPrime_RG780     => ("RG780_G0334", "i_G0327")
        case GmosSouthFilter.OG515            => ("OG515_G0330", "open2-8")
        case GmosSouthFilter.OIII             => ("OIII_G0338", "open2-8")
        case GmosSouthFilter.OIIIC            => ("open1-6", "OIIIC_G0339")
        case GmosSouthFilter.OVI              => ("OVI_G0347", "open2-8")
        case GmosSouthFilter.OVIC             => ("OVIC_G0348", "open2-8")
        case GmosSouthFilter.RG610            => ("RG610_G0331", "open2-8")
        case GmosSouthFilter.RG780            => ("open1-6", "RG780_G0334")
        case GmosSouthFilter.RPrime           => ("open1-6", "r_G0326")
        case GmosSouthFilter.RPrime_RG610     => ("RG610_G0331", "r_G0326")
        case GmosSouthFilter.SII              => ("open1-6", "SII_G0335")
        case GmosSouthFilter.UPrime           => ("open1-6", "u_G0332")
        case GmosSouthFilter.Y                => ("open1-6", "Y_G0344")
        case GmosSouthFilter.Z                => ("Z_G0343", "open2-8")
        case GmosSouthFilter.ZPrime           => ("open1-6", "z_G0328")
        case GmosSouthFilter.ZPrime_CaT       => ("CaT_G0333", "z_G0328")
      }
        .getOrElse(("open1-6", "open2-8"))
    }

  override val stageMode: EncodeEpicsValue[GmosSite.StageMode[GmosSite.South.type], String] =
    EncodeEpicsValue {
      case GmosSouthStageMode.NoFollow  => "MOVE"
      case GmosSouthStageMode.FollowXyz => "FOLLOW"
      case GmosSouthStageMode.FollowZ   => "FOLLOW-Z"
    }

  override val builtInROI: EncodeEpicsValue[BuiltinROI, Option[ROIValues]] = EncodeEpicsValue {
    case GmosRoi.FullFrame       =>
      ROIValues.fromInt(xStart = 1, xSize = 6144, yStart = 1, ySize = 4224)
    case GmosRoi.Ccd2            => ROIValues.fromInt(xStart = 2049, xSize = 2048, yStart = 1, ySize = 4224)
    case GmosRoi.CentralSpectrum =>
      ROIValues.fromInt(xStart = 1, xSize = 6144, yStart = 1625, ySize = 1024)
    case GmosRoi.CentralStamp    =>
      ROIValues.fromInt(xStart = 2923, xSize = 300, yStart = 1987, ySize = 300)
    case _                       => None
  }

  override val autoGain: EncodeEpicsValue[(GmosAmpReadMode, GmosAmpGain), Int] = {
    // gmosAutoGain.lut
    case (GmosAmpReadMode.Slow, GmosAmpGain.Low)  => 0
    case (GmosAmpReadMode.Slow, GmosAmpGain.High) => 0
    case (GmosAmpReadMode.Fast, GmosAmpGain.Low)  => 10
    case (GmosAmpReadMode.Fast, GmosAmpGain.High) => 0
  }
}

object GmosSouthControllerEpics {
  def apply[F[_]: Async: Logger](sys: => GmosEpics[F]): GmosController[F, GmosSite.South.type] = {
    implicit val encoders = GmosSouthEncoders
    GmosControllerEpics[F, GmosSite.South.type](sys)
  }
}
