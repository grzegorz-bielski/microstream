package com.microstream.channel

// auto & semi-auto derivation doesn't work in Scala 3 yet
import io.circe.{Decoder, Encoder}

case class CreateChannelDto(name: String)
object CreateChannelDto {
  implicit val d: Decoder[CreateChannelDto] =
    Decoder.forProduct1("name")(CreateChannelDto.apply)
  implicit val e: Encoder[CreateChannelDto] =
    Encoder.forProduct1("name")(n => n.name)
}

case class ChannelSummaryDto(id: Channel.Id)
object ChannelSummaryDto {
  implicit val d: Decoder[ChannelSummaryDto] =
    Decoder.forProduct1("id")(ChannelSummaryDto.apply)

  implicit val e: Encoder[ChannelSummaryDto] =
    Encoder.forProduct1("id")(n => n.id)
}
