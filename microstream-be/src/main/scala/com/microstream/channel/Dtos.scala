package com.microstream.channel

// auto & semi-auto derivation doesn't work in Scala 3 yet
import io.circe.{Decoder, Encoder}
import java.sql.Timestamp
import scala.util.Try

case class CreateChannelDto(name: String)
object CreateChannelDto {
  implicit val d: Decoder[CreateChannelDto] =
    Decoder.forProduct1("name")(CreateChannelDto.apply)
  implicit val e: Encoder[CreateChannelDto] =
    Encoder.forProduct1("name")(n => n.name)
}

case class ChannelCreationSummaryDto(id: Channel.Id)
object ChannelCreationSummaryDto {
  implicit val d: Decoder[ChannelCreationSummaryDto] =
    Decoder.forProduct1("id")(ChannelCreationSummaryDto.apply)

  implicit val e: Encoder[ChannelCreationSummaryDto] =
    Encoder.forProduct1("id")(n => n.id)
}

case class ChannelQueryDto(id: Long, name: String, createdAt: Timestamp)
object ChannelQueryDto {
  import TimestampFormat._

  implicit val d: Decoder[ChannelQueryDto] =
    Decoder.forProduct3("id", "name", "createdAt")(ChannelQueryDto.apply)

  implicit val e: Encoder[ChannelQueryDto] =
    Encoder.forProduct3("id", "name", "createdAt")(n => (n.id, n.name, n.createdAt))
}

object TimestampFormat {
  implicit val dt: Decoder[Timestamp] =
    Decoder.decodeString.emapTry(str => Try(Timestamp.valueOf(str)))

  implicit val et: Encoder[Timestamp] =
    Encoder.encodeString.contramap(_.toString)
}
