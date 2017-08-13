package sharry.common

import java.nio.file.{Paths, Path}
import java.time.{Instant, Duration}
import io.circe._

/** Codecs for some java/base types.*/
trait JsonCodec {
  implicit val _instantDec: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
  implicit val _instantEnc: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

  implicit val _durationDec: Decoder[Duration] = Decoder.decodeString.map(Duration.parse)
  implicit val _durationEnc: Encoder[Duration] = Encoder.encodeString.contramap[Duration](_.toString)

  implicit val _pathDec: Decoder[Path] = Decoder.decodeString.map(Paths.get(_))
  implicit val _pathEnc: Encoder[Path] = Encoder.encodeString.contramap(_.toString)

}

object JsonCodec extends JsonCodec
