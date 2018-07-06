// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.server.keywords

import seqexec.model.dhs.ImageFileId
import seqexec.server.SeqAction
import org.http4s.client.Client

/**
  * Gemini Data service client
  */
final case class GDSClient[F[_]](client: Client[F]) extends KeywordsClient {
  /**
    * Set the keywords for an image
    */
  def setKeywords(id: ImageFileId, keywords: KeywordBag, finalFlag: Boolean): SeqAction[Unit] = {
    SeqAction.void
  }
}