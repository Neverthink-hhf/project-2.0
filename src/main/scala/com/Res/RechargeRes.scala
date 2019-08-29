package com.Res

import com.data.JedisConnectionPool

object RechargeRes {
  def main(args: Array[String]): Unit = {

    val jedis = JedisConnectionPool.getConnection()

    while (true) {
      val orderId: String = jedis.hget("RechargeRedis", "orderId")
      val chargefee: String = jedis.hget("RechargeRedis", "chargefee")
      val success_sum: String = jedis.hget("RechargeRedis", "success_sum")

      println(s"orderId: ${orderId}, chargefee: ${chargefee}, success_sum: ${success_sum}")

      Thread.sleep(60000)
    }
  }
}
