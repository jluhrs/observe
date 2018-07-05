// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.server

import seqexec.model.dhs.ImageFileId
import seqexec.server.keywords._

/**
 * Typeclass for types that can send keywords
 */
trait HeaderProvider[A] {
  // Name of the instrument to be sent to the DHS
  def name(a: A): String
  // Client to send keywords to an appropriate server
  def keywordsClient(a: A): KeywordsClient
}

object HeaderProvider {
  def apply[A](implicit ev: HeaderProvider[A]): HeaderProvider[A] = ev

  final class HeaderProviderOps[A: HeaderProvider](val a: A) {
    def name: String = HeaderProvider[A].name(a)
    def keywordsClient: KeywordsClient = HeaderProvider[A].keywordsClient(a)
  }

  implicit def ToHeaderProviderOps[A: HeaderProvider](a: A): HeaderProviderOps[A] = new HeaderProviderOps(a)
}

/**
 * Header implementations know what headers sent before and after an observation
 */
trait Header {
  def sendBefore(id: ImageFileId): SeqAction[Unit]
  def sendAfter(id: ImageFileId): SeqAction[Unit]
}

object Header {
  import HeaderProvider._

  // Default values for FITS headers
  val IntDefault: Int = -9999
  val DoubleDefault: Double = -9999.0
  val StrDefault: String = "No Value"
  val BooleanDefault: Boolean = false

  def buildKeyword[A](get: SeqAction[A], name: String, f: (String, A) => Keyword[A]): KeywordBag => SeqAction[KeywordBag] =
    k => get.map(x => k.add(f(name, x)))
  def buildInt8(get: SeqAction[Byte], name: String ): KeywordBag => SeqAction[KeywordBag]       = buildKeyword(get, name, Int8Keyword)
  def buildInt16(get: SeqAction[Short], name: String ): KeywordBag => SeqAction[KeywordBag]     = buildKeyword(get, name, Int16Keyword)
  def buildInt32(get: SeqAction[Int], name: String ): KeywordBag => SeqAction[KeywordBag]       = buildKeyword(get, name, Int32Keyword)
  def buildFloat(get: SeqAction[Float], name: String ): KeywordBag => SeqAction[KeywordBag]     = buildKeyword(get, name, FloatKeyword)
  def buildDouble(get: SeqAction[Double], name: String ): KeywordBag => SeqAction[KeywordBag]   = buildKeyword(get, name, DoubleKeyword)
  def buildBoolean(get: SeqAction[Boolean], name: String ): KeywordBag => SeqAction[KeywordBag] = buildKeyword(get, name, BooleanKeyword)
  def buildString(get: SeqAction[String], name: String ): KeywordBag => SeqAction[KeywordBag]   = buildKeyword(get, name, StringKeyword)

  private def bundleKeywords[A: HeaderProvider](inst: A, ks: Seq[KeywordBag => SeqAction[KeywordBag]]): SeqAction[KeywordBag] = {
    val z = SeqAction(KeywordBag(StringKeyword("instrument", inst.name)))
    ks.foldLeft(z) { case (a,b) => a.flatMap(b) }
  }

  def sendKeywords[A: HeaderProvider](id: ImageFileId, inst: A, b: Seq[KeywordBag => SeqAction[KeywordBag]]): SeqAction[Unit] = for {
    bag <- bundleKeywords(inst, b)
    _   <- inst.keywordsClient.setKeywords(id, bag, finalFlag = false)
  } yield ()


  object Implicits {
    // A simple typeclass to encapsulate default values
    trait DefaultValue[A] {
      def default: A
    }
    implicit class DefaultValueOps[A](a: Option[A])(implicit d: DefaultValue[A]) {
      def orDefault: A = a.getOrElse(d.default)
    }
    implicit val IntDefaultValue: DefaultValue[Int] = new DefaultValue[Int] {
      val default: Int = IntDefault
    }
    implicit val DoubleDefaultValue: DefaultValue[Double] = new DefaultValue[Double] {
      val default: Double = DoubleDefault
    }
    implicit val StrDefaultValue: DefaultValue[String] = new DefaultValue[String] {
      val default: String = StrDefault
    }

    implicit class A2SeqAction[A: DefaultValue](val v: Option[A]) {
      def toSeqAction: SeqAction[A] = SeqAction(v.orDefault)
    }

    implicit class SeqActionOption2SeqAction[A: DefaultValue](val v: SeqAction[Option[A]]) {
      def orDefault: SeqAction[A] = v.map(_.orDefault)
    }
  }
}
