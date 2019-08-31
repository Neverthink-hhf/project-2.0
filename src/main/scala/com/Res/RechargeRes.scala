package com.Res

import com.data.JedisConnectionPool

object RechargeRes {
  def main(args: Array[String]): Unit = {

    val jedis = JedisConnectionPool.getConnection()

    while (true) {
      val orderId: String = jedis.hget("res1", "orider_sum")
      val chargefee: String = jedis.hget("res1", "money_sum")
      val success_sum: String = jedis.hget("res1", "success_sum")
      val time: String = jedis.hget("res1", "time")

      println(s"orderId: ${orderId}, chargefee: ${chargefee}, success_sum: ${success_sum}, time: ${time}")

      Thread.sleep(60000)
    }
  }
}
