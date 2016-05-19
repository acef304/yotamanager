package org.acef304.yotamanager

import scala.collection.mutable.ArrayBuffer

/**
  * Created by acef304 on 5/19/16.
  */
class CircularBuffer[A](maxLength: Int) extends ArrayBuffer[A](maxLength){
  var lastPos = -1
  var isFull: Boolean = false

  override def +=(elem: A): this.type = {
    isFull = isFull || (lastPos + 1 == maxLength)
    lastPos = (lastPos + 1) % maxLength
    if (isFull) {
      this.update(lastPos, elem)
      this
    }
    else
      super.+=(elem)
  }
}
