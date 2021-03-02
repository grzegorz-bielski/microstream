package com.microstream.channel

import slick.basic.BasicProfile
import scala.concurrent.ExecutionContext
import akka.Done
import slick.basic.DatabaseConfig
import slick.driver.JdbcProfile

class ChannelRepository(val dbConfig: DatabaseConfig[JdbcProfile]) {
  import dbConfig.profile.api._
  import slick.generated.Tables.{ChannelRow, Channel => channel}

  def updateChannel(name: String)(implicit ec: ExecutionContext) = {
    channel.insertOrUpdate(ChannelRow(0, name, None)) map (_ => Done)
  }

}
