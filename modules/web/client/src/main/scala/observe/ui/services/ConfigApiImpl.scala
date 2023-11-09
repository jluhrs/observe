// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.services

import cats.effect.IO
import cats.effect.Resource
import crystal.react.View
import crystal.react.*
import io.circe.Encoder
import io.circe.*
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.core.enums.CloudExtinction
import lucuma.core.enums.ImageQuality
import lucuma.core.enums.SkyBackground
import lucuma.core.enums.WaterVapor
import observe.model.ClientId
import observe.ui.model.enums.ApiStatus
import org.http4s.Uri
import org.http4s.*
import org.typelevel.log4cats.Logger
import observe.model.Operator
import observe.model.Observer
import org.http4s.Uri.Path
import cats.syntax.all.*
import lucuma.core.model.Observation

case class ConfigApiImpl(
  client:    ApiClient,
  apiStatus: View[ApiStatus],
  latch:     Resource[IO, Unit]
)(using Logger[IO])
    extends ConfigApi[IO]:
  private def request[T: Encoder](path: Path, data: T): IO[Unit] =
    latch.use: _ =>
      apiStatus.async.set(ApiStatus.Busy) >>
        client
          .post(path, data)
          .flatTap(_ => apiStatus.async.set(ApiStatus.Idle))

  override def setImageQuality(iq: ImageQuality): IO[Unit]       =
    request(Uri.Path.empty / client.clientId.value / "iq", iq)
  override def setCloudExtinction(ce: CloudExtinction): IO[Unit] =
    request(Uri.Path.empty / client.clientId.value / "ce", ce)
  override def setWaterVapor(wv: WaterVapor): IO[Unit]           =
    request(Uri.Path.empty / client.clientId.value / "wv", wv)
  override def setSkyBackground(sb: SkyBackground): IO[Unit]     =
    request(Uri.Path.empty / client.clientId.value / "sb", sb)

  override def setOperator(operator: Option[Operator]): IO[Unit]                        =
    request(
      Uri.Path.empty / client.clientId.value / "operator" / operator.map(_.toString).orEmpty,
      ""
    )
  override def setObserver(obsId: Observation.Id, observer: Option[Observer]): IO[Unit] =
    request(
      Uri.Path.empty / obsId.toString / client.clientId.value / "observer" /
        observer.map(_.toString).orEmpty,
      ""
    )

  override def isBlocked: Boolean = apiStatus.get == ApiStatus.Busy
