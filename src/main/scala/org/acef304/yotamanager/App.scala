package org.acef304.yotamanager

import java.sql.Timestamp

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.Await

object Props {
  def yota_source = scala.io.Source.fromURL("http://10.0.0.1/status")
}

object Manager extends App {
  override def main(args: Array[String]): Unit = console(args.mkString(" "))

  def console(arg: String = scala.io.StdIn.readLine(">")) {
    arg match {
      case null => exit()
      case x if x startsWith "daemon" => x.split(" ")(1) match {
        case "start" => daemon.start()
        case "suspend" => daemon.suspend()
        case _ => println("Usage: daemon [start|suspend] " + x.split(" ")(1))
      }
      case x if x startsWith "info" => Datastore.printSessions()
      case x if x startsWith "exit" => exit
      case x if x isEmpty => {}
      case _ => println("command not found")
    }
    console()
  }

  def exit() = {
    println("Exiting")
    Datastore.db.close()
    System.exit(0)
  }

  lazy val daemon = new Thread(new Runnable {
    def run() = {
      try {
        println("daemon started")
        Await.result(Datastore.create(), 2 second)
        while (true) {
          YotaStatus.refresh()
          YotaStatus.sessionInfo map Datastore.updateSession
          Thread.sleep(500)
        }
      }
      catch {
        case ex: Exception =>
          ex.printStackTrace()
          run()
      }

    }
  })
}

object NetworkStatus {
  case class NetworkStat(time: Long, tx_bytes: Long, tx_packets: Long, rx_bytes: Long, rx_packets: Long)

  def getNetworkInfo() = {
    val tx_bytes = scala.io.Source.fromFile("/sys/class/net/eth1/statistics/tx_bytes").getLines().mkString("").toLong
    val tx_packets = scala.io.Source.fromFile("/sys/class/net/eth1/statistics/tx_packets").getLines().mkString("").toLong
    val rx_bytes = scala.io.Source.fromFile("/sys/class/net/eth1/statistics/rx_bytes").getLines().mkString("").toLong
    val rx_packets = scala.io.Source.fromFile("/sys/class/net/eth1/statistics/rx_packets").getLines().mkString("").toLong
    NetworkStat(System.currentTimeMillis(), tx_bytes, tx_packets, rx_bytes, rx_packets)
  }

  val stats = ArrayBuffer.empty[NetworkStat]

  lazy val daemon = new Thread(new Runnable {
    def internalRun: Unit = {
      try{
        while (true) {
          stats += getNetworkInfo()
          Thread.sleep(10)
        }
      }
      catch {
        case ex: Exception =>
          ex.printStackTrace()
          internalRun
      }
    }

    def run() = internalRun
  })


}

object YotaStatus {
  val propertyPattern = "(.*?)=(.*?)".r
  private var statusResponse: Map[String, String] = Map()

  def statusMap = {
    if (statusResponse.isEmpty)
      try {
        statusResponse = Props.yota_source.getLines().flatMap{
          case propertyPattern(key, value) => Some((key, value))
        }.toMap
      }
    catch {
      case ex: Exception => ex.printStackTrace()
    }
    statusResponse
  }

  def refresh() = statusResponse = statusResponse.empty

  def isConnected = statusMap("State").equals("Connected")

  def sessionInfo = if (isConnected)
    Some(ModemSession(
      None,
      statusMap("SessionID").toLong,
      statusMap("SentBytes").toLong,
      statusMap("ReceivedBytes").toLong,
      statusMap("ConnectedTime").toLong,
      statusMap("MaxDownlinkThroughput").toLong,
      statusMap("MaxUplinkThroughput").toLong,
      statusMap("CurDownlinkThroughput").toLong,
      statusMap("CurUplinkThroughput").toLong,
      new Timestamp(System.currentTimeMillis)
    ))
  else
    None
}

case class ModemSession(id: Option[Long], sessionId: Long, sentBytes: Long, receivedBytes: Long,
                        connectedTime: Long, maxDownloadSpeed: Long, maxUploadSpeed: Long,
                        currentDownloadSpeed: Long, currentUploadSpeed: Long,
                        updated: Timestamp)
