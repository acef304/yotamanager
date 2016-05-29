package org.acef304.yotamanager

import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

/**
  * Created by acef304 on 5/27/16.
  */

object ObservableTest {
  var subs: Subscription = null
  val seconds = Observable.interval(1000 millis) map (_ => NetworkStatus.getNetworkInfo())

  var secondSpeed = 0L
  var minuteSpeed = 0L

  seconds.slidingBuffer(2, 1).subscribe{ t => secondSpeed = t.last.rx_bytes - t.head.rx_bytes}
  seconds.slidingBuffer(61, 1).subscribe{ t => minuteSpeed = (t.last.rx_bytes - t.head.rx_bytes) / 60}

  val daemon = new Thread(new Runnable {
    def run = {
      while (true) {
        println(s"tick speed: $secondSpeed \t\tsecond speed: $minuteSpeed")
        Thread.sleep(200)
      }
    }
  })

  def stop = subs.unsubscribe()
}
