package phobos.monix.ops

import javax.xml.stream.XMLStreamConstants
import monix.eval.Task
import monix.reactive.Observable
import phobos.decoding.{Cursor, ElementDecoder, XmlDecoder, XmlStreamReader}

private[phobos] trait MonixOps {
  implicit def DecoderOps[A](xmlDecoder: XmlDecoder[A]): DecoderOps[A] = new DecoderOps[A](xmlDecoder)
}

class DecoderOps[A](private val xmlDecoder: XmlDecoder[A]) extends AnyVal {
  def decodeFromObservable(observable: Observable[Array[Byte]], charset: String = "UTF-8"): Task[A] = {
    val sr: XmlStreamReader = XmlDecoder.createStreamReader(charset)
    val cursor              = new Cursor(sr)

    observable
      .foldLeftL[ElementDecoder[A]](xmlDecoder.elementdecoder) { (decoder, bytes) =>
        sr.getInputFeeder.feedInput(bytes, 0, bytes.length)
        cursor.next()
        while (
          cursor.getEventType == XMLStreamConstants.DTD || cursor.getEventType == XMLStreamConstants.START_DOCUMENT
        ) {
          cursor.next()
        }

        if (decoder.result(cursor.history).isRight) {
          decoder
        } else {
          decoder.decodeAsElement(cursor, xmlDecoder.localname, xmlDecoder.namespaceuri)
        }
      }
      .flatMap { a =>
        sr.getInputFeeder.endOfInput()
        Task.fromEither(a.result(cursor.history))
      }
  }
}
