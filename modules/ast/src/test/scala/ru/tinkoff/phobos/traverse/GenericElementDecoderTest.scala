package phobos.traverse

import cats.syntax.either._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import com.softwaremill.diffx.generic.auto._
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import phobos.ast.XmlLeaf
import phobos.decoding.{DecodingError, ElementDecoder, XmlDecoder}

class GenericElementDecoderTest extends AnyWordSpec with Matchers with DiffShouldMatcher with EitherValues {
  import GenericElementDecoderTest._
  "GenericElementDecoder" should {
    "work correctly with immutable accumulators" in {
      implicit val decodeAllAttributes: ElementDecoder[Acc] = GenericElementDecoder(ImmutableTraversalLogic)
      val xmlDecoder = XmlDecoder
        .fromElementDecoder[Acc]("ast")

      val sampleXml =
        """<?xml version='1.0' encoding='UTF-8'?><ans1:ast xmlns:ans1="https://example.org" foo="5"><bar>bazz</bar><array foo2="true" foo3="false"><elem>11111111111111</elem><elem>11111111111112</elem></array><nested x="2.0"><scala>2.13</scala><dotty>0.13</dotty><scala-4/></nested></ans1:ast>"""

      val expectedResult0 = Acc(
        Map(
          "foo"  -> "5",
          "foo2" -> "true",
          "foo3" -> "false",
          "x"    -> "2.0",
        ),
      )

      xmlDecoder.decode(sampleXml) shouldMatchTo (expectedResult0.asRight[DecodingError])

      val xmlWithoutAttrs =
        """<?xml version='1.0' encoding='UTF-8'?><ans1:ast xmlns:ans1="https://example.org"><bar>bazz</bar><array><elem>11111111111111</elem><elem>11111111111112</elem></array><nested><scala>2.13</scala><dotty>0.13</dotty><scala-4/></nested></ans1:ast>"""

      val expectedResult1 = Acc(Map.empty)

      xmlDecoder.decode(xmlWithoutAttrs) shouldMatchTo (expectedResult1.asRight[DecodingError])
    }
  }
}

object GenericElementDecoderTest {
  case class Acc(attributes: Map[String, String])

  object ImmutableTraversalLogic extends DecodingTraversalLogic[Acc, Acc] {
    override def newAcc(): Acc = Acc(Map.empty)

    override def onFinish(acc: Acc): Acc = acc

    override def onAttributes(acc: Acc, attributes: List[(String, XmlLeaf)]): Acc = {
      acc.copy(
        attributes = acc.attributes ++ attributes.map { case (name, leaf) => name -> leaf.value.toString },
      )
    }

    override def combine(acc: Acc, field: String, intermediateResult: Acc): Acc = {
      acc.copy(attributes = acc.attributes ++ intermediateResult.attributes)
    }
  }
}
