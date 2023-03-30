// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.model.enums

import cats.syntax.all._
import lucuma.core.util.Enumerated

sealed abstract class ImageQuality(val tag: String, val toInt: Option[Int], val label: String)
    extends Product
    with Serializable

object ImageQuality {

  case object Unknown   extends ImageQuality("Unknown", none, "Unknown")
  case object Percent20 extends ImageQuality("Percent20", 20.some, "20%/Best")
  case object Percent70 extends ImageQuality("Percent70", 70.some, "70%/Good")
  case object Percent85 extends ImageQuality("Percent85", 85.some, "85%/Poor")
  case object Any       extends ImageQuality("Any", 100.some, "Any")

  /** @group Typeclass Instances */
  implicit val ImageQualityEnumerated: Enumerated[ImageQuality] =
    Enumerated.from(Unknown, Percent20, Percent70, Percent85, Any).withTag(_.tag)
}
