package phobos.annotations

import scala.annotation.nowarn
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.reflect.macros.blackbox

import phobos.configured.ElementCodecConfig

@compileTimeOnly("enable macro paradise to expand macro annotations")
final class ElementCodec(config: ElementCodecConfig = ElementCodecConfig.default) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ElementCodecImpl.impl
}

private final class ElementCodecImpl(ctx: blackbox.Context) extends CodecAnnotation(ctx) {
  import c.universe._

  def instances(typ: Tree): Seq[Tree] = {
    val pkg = q"phobos"

    @nowarn("msg=not.*?exhaustive")
    val config = c.prefix.tree match {
      case q"new ElementCodec"          => defaultConfig.tree
      case q"new ElementCodec($config)" => config
    }

    Seq(
      q"""
          implicit val ${TermName(c.freshName("elementEncoder"))}: $pkg.encoding.ElementEncoder[$typ] =
            $pkg.derivation.semiauto.deriveElementEncoderConfigured[$typ]($config)
       """,
      q"""
          implicit val ${TermName(c.freshName("elementDecoder"))}: $pkg.decoding.ElementDecoder[$typ] =
            $pkg.derivation.semiauto.deriveElementDecoderConfigured[$typ]($config)
       """,
    )
  }

}
