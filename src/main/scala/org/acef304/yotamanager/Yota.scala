package org.acef304.yotamanager

/**
  * Created by acef304 on 29.05.16.
  */
object Yota {
  val propertyPattern = "(.*?)=(.*?)".r
  private var statusResponse: Map[String, String] = Map()

  val path = "~/.yota".replace("~", System.getProperty("user.home"))

  lazy val settings = scala.io.Source.fromFile(path).getLines.flatMap{
    case propertyPattern(key, value) => Some((key, value))
  }.toMap

  lazy val login = settings("login")
  lazy val password = settings("password")

  def tariffs = Map(
    "320" -> "POS-MA6-0002",
    "1.0" -> "POS-MA6-0008",
    "max" -> "POS-MA6-0024")

  def miniCurl(postParam: Option[String], url: String, storeCookie: Boolean = false) = {
    import sys.process._
    val t = s"""curl -${if (storeCookie) "c" else "b"} cook.txt  -s -k -L ${postParam.map(p => "-d " + p).getOrElse("")} $url"""
    println(t)
    t !!
  }

  def changeTariff(speed: String) = {
    import sys.process._
    val tariffCode = tariffs(speed)

    val loginAttempt = miniCurl(
      postParam = Some(s"IDToken1=$login&IDToken2=$password&goto=https%3A%2F%2Fmy.yota.ru%3A443%2Fselfcare%2FloginSuccess&gotoOnFail=https%3A%2F%2Fmy.yota.ru%3A443%2Fselfcare%2FloginError&old-token=&org=customer"),
      url = "https://login.yota.ru/UI/Login",
      storeCookie = true)

    val devices = """curl -b cook.txt  -s -k -L https://my.yota.ru/selfcare/devices""" !!
    val deviceStringPattern = """name="product" value="([^"]+)"""".r

    deviceStringPattern.findAllIn(devices).matchData.map( t => t.group(1)).toList.headOption match {
      case Some(deviceId) =>
        s"""curl -b cook.txt  -s -k -L -d "product=$deviceId&offerCode=$tariffCode&homeOfferCode=&areOffersAvailable=false&period=&status=custom&autoprolong=0&isSlot=false&resourceId=&currentDevice=1&username=&isDisablingAutoprolong=false" https://my.yota.ru/selfcare/devices/changeOffer""" !!
      case None => {}
    }
  }
}
