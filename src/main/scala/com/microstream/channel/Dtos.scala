package com.microstream.channel

case class CreateChannelDto(name: String)
object CreateChannelDto {
  // auto & semi-auto derivation doesn't work in Scala 3 yet
  import io.circe.{Decoder, Encoder}
  implicit val d: Decoder[CreateChannelDto] =
    Decoder.forProduct1("name")(CreateChannelDto.apply)
  implicit val e: Encoder[CreateChannelDto] =
    Encoder.forProduct1("name")(n => n.name)
}
