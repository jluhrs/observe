// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.ui.services

import cats.effect.IO
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Encoder
import io.circe.*
import io.circe.syntax.*
import lucuma.core.enums.CloudExtinction
import lucuma.core.enums.ImageQuality
import lucuma.core.enums.SkyBackground
import lucuma.core.enums.WaterVapor
import observe.model.ClientId
import org.http4s.Method
import org.http4s.Uri
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.*
import org.http4s.headers.Authorization

case class ConfigApiImpl(client: Client[IO], baseUri: Uri, token: NonEmptyString)
    extends ConfigApi[IO]:
  private def request[T: Encoder](path: String, data: T): IO[Unit] = // TODO: Retries
    client.expect[Unit](
      Request(Method.POST, baseUri.addPath(path))
        .withHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        .withEntity(data.asJson)
    )

  override def setImageQuality(iq:    ImageQuality): IO[Unit]    = request("/iq", iq)
  override def setCloudExtinction(ce: CloudExtinction): IO[Unit] = request("/ce", ce)
  override def setWaterVapor(wv:      WaterVapor): IO[Unit]      = request("/wv", wv)
  override def setSkyBackground(sb:   SkyBackground): IO[Unit]   = request("/sb", sb)
  override def refresh(clientId:      ClientId): IO[Unit]        = request("/refresh", clientId)
