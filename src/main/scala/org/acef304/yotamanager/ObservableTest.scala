package org.acef304.yotamanager

import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

/**
  * Created by acef304 on 5/27/16.
  */

object ObservableTest {
  var subs: Subscription = null
  val o = Observable.interval(10000 millis) map ((_, System.currentTimeMillis()))

  def start(param: String) = subs = o.subscribe( n => println(s"$param - $n")) 

  def stop = subs.unsubscribe()
}
