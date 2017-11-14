// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.seqexec.server.gcal

import edu.gemini.seqexec.server.gcal.GcalController.GcalConfig
import edu.gemini.seqexec.server.{SeqAction, TrySeq}
import org.log4s.getLogger

/**
  * Created by jluhrs on 3/15/17.
  */
object GcalControllerSim extends GcalController {
  private val Log = getLogger

  override def getConfig: SeqAction[GcalConfig] = SeqAction(GcalController.GcalConfig.allOff)

  def printConfig(config: GcalConfig): List[String] = List(
    s"lampAr = ${config.lampAr}",
    s"lampCuar = ${config.lampCuAr}",
    s"lampQH = ${config.lampQh}",
    s"lampThAr = ${config.lampThAr}",
    s"lampXe = ${config.lampXe}",
    s"lampIr = ${config.lampIr}",
    s"shutter = ${config.shutter}",
    s"filter = ${config.filter}",
    s"diffuser = ${config.diffuser}")

  override def applyConfig(config: GcalConfig): SeqAction[Unit] = SeqAction.either {
    Log.debug("applyConfig: config is\n" + printConfig(config).mkString("\n"))
    TrySeq(())
  }
}