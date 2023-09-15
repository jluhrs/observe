// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.server.gmos

import cats.Show
import cats.effect.{Async, Ref, Temporal}
import cats.syntax.all.*
import fs2.Stream
import monocle.Optional
import monocle.Focus
import monocle.syntax.all.*
import monocle.std.option.some
import observe.model.GmosParameters.NsCycles
import observe.model.dhs.ImageFileId
import observe.model.enums.NodAndShuffleStage.*
import observe.model.enums.ObserveCommandResult
import observe.model.{NSSubexposure, ObserveStage}
import observe.server.InstrumentSystem.ElapsedTime
import observe.server.RemainingTime
import observe.server.ProgressUtil.countdown
import observe.server.gmos.GmosController.Config.NSConfig
import observe.server.gmos.GmosController.GmosConfig
import observe.server.{InstrumentControllerSim, ObsProgress, Progress}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Keep track of the current execution state
 */
final case class NSCurrent(
  fileId:        ImageFileId,
  totalCycles:   Int,
  exposureCount: Int,
  expTime:       FiniteDuration
) {
  def lastSubexposure: Boolean =
    (exposureCount + 1) === totalCycles * NsSequence.length

  def firstSubexposure: Boolean = exposureCount === 0

  val cycle: Int = exposureCount / NsSequence.length

  val stageIndex: Int = exposureCount % NsSequence.length
}

object NSCurrent {
  given Show[NSCurrent] = Show.show { a =>
    s"NS State: file=${a.fileId}, cycle=${a.cycle + 1}, stage=${NsSequence.toList
        .lift(a.exposureCount % NsSequence.length)
        .getOrElse("Unknown")}/${(a.exposureCount % NsSequence.length) + 1}, subexposure=${a.exposureCount + 1}, expTime=${a.expTime}"
  }
}

/**
 * Used to keep the internal state of NS
 */
final case class NSObsState(config: NSConfig, current: Option[NSCurrent])

object NSObsState {
  def fromConfig(c: NSConfig): NSObsState =
    NSObsState(c, None)

  val Zero: NSObsState = NSObsState(NSConfig.NoNodAndShuffle, None)

  val fileId: Optional[NSObsState, ImageFileId] =
    Focus[NSObsState](_.current).andThen(some[NSCurrent]).andThen(Focus[NSCurrent](_.fileId))

  val exposureCount: Optional[NSObsState, Int] =
    Focus[NSObsState](_.current).andThen(some[NSCurrent]).andThen(Focus[NSCurrent](_.exposureCount))

  val expTime: Optional[NSObsState, FiniteDuration] =
    Focus[NSObsState](_.current).andThen(some[NSCurrent]).andThen(Focus[NSCurrent](_.expTime))

}

object GmosControllerSim {
  def apply[F[_]: Temporal, T <: GmosController.GmosSite](
    sim:      InstrumentControllerSim[F],
    nsConfig: Ref[F, NSObsState]
  ): GmosController[F, T] =
    new GmosController[F, T] {
      override def observe(
        fileId:  ImageFileId,
        expTime: FiniteDuration
      ): F[ObserveCommandResult] =
        nsConfig.modify {
          case s @ NSObsState(NSConfig.NoNodAndShuffle, _)                =>
            (s, s)
          case s @ NSObsState(NSConfig.NodAndShuffle(cycles, _, _, _), _) =>
            // Initialize the current state
            val update =
              s.focus(_.current).replace(NSCurrent(fileId, cycles.value, 0, expTime).some)
            (update, update)
        } >>= {
          case NSObsState(NSConfig.NodAndShuffle(_, _, _, _), Some(curr)) =>
            sim.log(s"Simulate Gmos N&S observation ${curr.show}") *>
              // Initial N&S obs
              sim.observe(fileId, expTime).as(ObserveCommandResult.Paused)
          case NSObsState(_, _)                                           =>
            sim.observe(fileId, expTime) // Regular observation
        }

      override def applyConfig(config: GmosConfig[T]): F[Unit] =
        nsConfig.set(NSObsState.fromConfig(config.ns)) *> // Keep the state of NS Config
          sim.applyConfig(config)

      override def stopObserve: F[Unit] = sim.stopObserve

      override def abortObserve: F[Unit] = sim.abortObserve

      override def endObserve: F[Unit] = sim.endObserve

      override def pauseObserve: F[Unit] = sim.pauseObserve

      override def resumePaused(expTime: FiniteDuration): F[ObserveCommandResult] =
        nsConfig.modify {
          case s @ NSObsState(NSConfig.NodAndShuffle(_, _, _, _), Some(curr))
              if !curr.lastSubexposure =>
            // We should keep track of where on a N&S Sequence are we
            // Let's just increase the exposure counter
            val upd = NSObsState.exposureCount.modify(_ + 1)
            (upd(s), upd(s))
          case s =>
            (s, s)
        } >>= {
          case NSObsState(NSConfig.NodAndShuffle(_, _, _, _), Some(curr))
              if !curr.lastSubexposure =>
            sim.log(s"Next Nod ${curr.show}") *>
              sim.observe(curr.fileId, expTime).as(ObserveCommandResult.Paused)
          case NSObsState(NSConfig.NodAndShuffle(_, _, _, _), Some(curr)) if curr.lastSubexposure =>
            sim.log(s"Final Nod ${curr.show}") *>
              sim.observe(curr.fileId, expTime)
          case _                                                                                  =>
            // Regular observation
            sim.resumePaused
        }

      override def stopPaused: F[ObserveCommandResult] = sim.stopPaused

      override def abortPaused: F[ObserveCommandResult] = sim.abortPaused

      private def classicObserveProgress(
        total:   FiniteDuration,
        elapsed: ElapsedTime
      ): Stream[F, Progress] = sim.observeCountdown(total, elapsed)

      private def nsObserveProgress(
        total:   FiniteDuration,
        elapsed: ElapsedTime,
        curr:    NSCurrent
      ): Stream[F, Progress] = (
        if (curr.firstSubexposure)
          Stream.emit(ObsProgress(total, RemainingTime(total), ObserveStage.Preparing)) ++
            countdown[F](total, elapsed.self)
        else if (curr.lastSubexposure)
          countdown[F](total, elapsed.self) ++
            Stream.emit(ObsProgress(total, RemainingTime(Duration.Zero), ObserveStage.ReadingOut))
        else countdown[F](total, elapsed.self)
      ).map { p =>
        val sub = NSSubexposure(NsCycles(curr.totalCycles), NsCycles(curr.cycle), curr.stageIndex)
        p.toNSProgress(sub.getOrElse(NSSubexposure.Zero))
      }

      override def observeProgress(
        total:   FiniteDuration,
        elapsed: ElapsedTime
      ): Stream[F, Progress] =
        Stream.eval(nsConfig.get).flatMap {
          case NSObsState(NSConfig.NodAndShuffle(_, _, _, _), Some(curr)) =>
            nsObserveProgress(total, elapsed, curr)
          case _                                                          => classicObserveProgress(total, elapsed)
        }

      override def nsCount: F[Int] = nsConfig.get.map(_.current.foldMap(_.exposureCount))
    }

  def south[F[_]: Async: Logger]: F[GmosController[F, GmosController.GmosSite.South.type]] =
    (Ref.of(NSObsState.Zero), InstrumentControllerSim[F](s"GMOS South")).mapN { (nsConfig, sim) =>
      GmosControllerSim[F, GmosController.GmosSite.South.type](sim, nsConfig)
    }

  def north[F[_]: Async: Logger]: F[GmosController[F, GmosController.GmosSite.North.type]] =
    (Ref.of(NSObsState.Zero), InstrumentControllerSim[F](s"GMOS North")).mapN { (nsConfig, sim) =>
      GmosControllerSim[F, GmosController.GmosSite.North.type](sim, nsConfig)
    }
}