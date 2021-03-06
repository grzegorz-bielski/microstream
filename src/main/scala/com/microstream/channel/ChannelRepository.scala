package com.microstream.channel

import slick.basic.BasicProfile
import scala.concurrent.ExecutionContext
import akka.Done
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import java.sql.Timestamp
import java.time.ZonedDateTime

class ChannelRepository(val dbConfig: DatabaseConfig[PostgresProfile]) {
  import dbConfig.profile.api._
  import slick.generated.Tables.{ChannelRow, Channel => channel}

  def updateChannel(name: String)(implicit ec: ExecutionContext) = {
    val timestamp = Timestamp.valueOf(ZonedDateTime.now.toLocalDateTime)

    channel.insertOrUpdate(ChannelRow(0, name, timestamp)) map (_ => Done)
  }

  // todo: use pagination
  def getChannels() = {
    channel.result
  }

}
