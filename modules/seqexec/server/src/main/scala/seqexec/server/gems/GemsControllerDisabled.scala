package seqexec.server.gems

import seqexec.server.gems.Gems.GemsWfsState
import cats.implicits._
import cats.Applicative
import io.chrisdavenport.log4cats.Logger
import seqexec.server.SystemOverrides.overrideLogMessage
import seqexec.server.tcs.Gaos
import seqexec.server.tcs.Gaos.PauseResume

class GemsControllerDisabled[F[_]: Logger: Applicative] extends GemsController[F] {
  override def pauseResume(pauseReasons: Gaos.PauseConditionSet,
                           resumeReasons: Gaos.ResumeConditionSet
                          )(cfg: GemsController.GemsConfig): F[Gaos.PauseResume[F]] =
    PauseResume(
      overrideLogMessage("GeMS", "pause AO loops").some,
      overrideLogMessage("GeMS", "resume AO loops").some
    ).pure[F]

  override val stateGetter: GemsWfsState[F] = GemsWfsState.allOff
}
