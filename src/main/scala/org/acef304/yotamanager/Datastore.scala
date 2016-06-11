package org.acef304.yotamanager

import java.sql.Timestamp

import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by acef304 on 09.05.16.
  */

object Datastore {
  import slick.driver.H2Driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  val db = Database.forURL("jdbc:h2:file:./data/db", driver="org.h2.Driver", keepAliveConnection = true)

  class Stat(tag: Tag) extends Table[(Long, String)](tag, "TEST") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = (id, name)
  }
  val statistics = TableQuery[Stat]

  class Sessions(tag: Tag) extends Table[ModemSession](tag, "STAT") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sessionId = column[Long]("SessionID")
    def sentBytes = column[Long]("SentBytes")
    def receivedBytes = column[Long]("ReceivedBytes")
    def connectedTime = column[Long]("ConnectedTime")
    def maxDownloadSpeed = column[Long]("MaxDownlinkThroughput")
    def maxUploadSpeed = column[Long]("MaxUplinkThroughput")
    def currentDownloadSpeed = column[Long]("CurDownlinkThroughput")
    def currentUploadSpeed = column[Long]("CurUplinkThroughput")
    def updated = column[Timestamp]("updated")

    def * =
      (
        id.?, sessionId, sentBytes, receivedBytes, connectedTime,
        maxDownloadSpeed, maxUploadSpeed, currentDownloadSpeed, currentUploadSpeed, updated
        ) <> (ModemSession.tupled, ModemSession.unapply)
  }
  val sessions = TableQuery[Sessions]

  class InterfaceStats(tag: Tag) extends Table[NetworkStatus.NetworkStat](tag, "INTERFACES") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def time = column[Long]("time")
    def tx_bytes = column[Long]("tx_bytes")
    def tx_packets = column[Long]("tx_packets")
    def rx_bytes = column[Long]("rx_bytes")
    def rx_packets = column[Long]("rx_packets")

    def * = (
      id.?, time, tx_bytes, tx_packets, rx_bytes, rx_packets
      ) <> (NetworkStatus.NetworkStat.tupled, NetworkStatus.NetworkStat.unapply)
  }
  val interfaceStats = TableQuery[InterfaceStats]

  val schema = statistics.schema ++ sessions.schema ++ interfaceStats.schema

  def create() = {
    db.run(MTable.getTables("STAT").map{
      statTable =>
        if (statTable.isEmpty) {
          println("create tables")
          db.run(schema.create)
        }
    })
  }

  def insert() = { db.run(statistics += (0, "Fuck")) }
  def select() = {
    db.run(statistics.result).map(_.foreach{
      case (id, name) => println(s"id:$id, name:$name")
    })
    db.run(sessions.result).map({_.foreach(println)})
    println(s"statistics:${Await.result(db.run(statistics.length.result), 10 second)}")
    println(s"sessions:${Await.result(db.run(sessions.length.result), 10 second)}")
  }

  def updateSession(session: ModemSession) = {
    db.run(sessions.sortBy(_.updated.desc).take(1).result.headOption.map{
      case Some(lastSession) =>
        if (session.connectedTime < lastSession.connectedTime)
          db.run(sessions += session)
        else
          db.run(sessions insertOrUpdate session.copy(id = lastSession.id))
      case None => db.run(sessions += session)
    })
    //db.run(sessions insertOrUpdate session)
  }
  def printSessions() = {
    db.run(sessions.result).map({_.foreach(println)})
  }
}
