package com.utils

import java.sql.{Connection, PreparedStatement, Statement}
import java.util.Properties

import com.data.JedisConnectionPool
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.streaming.StreamingContext

object ResultUtils {

  def Result01(row: RDD[(Double, String, String, String)]) = {

    row.foreach(x => {

        val success: Int = if (x._2 == "0000") 1 else 0

        val start_time = x._3.toDouble
        val end_time = x._4.toDouble

        val time = (end_time - start_time) / 1000

        val jedis = JedisConnectionPool.getConnection()
        jedis.hincrBy("res1", "orider_sum", 1)
        jedis.hincrByFloat("res1", "money_sum", x._1)
        jedis.hincrBy("res1", "success_sum", success)
        jedis.hincrByFloat("res1", "time", time)
        println(("insert success"))
        jedis.close()

      })
  }

  def result02(row: RDD[(String, String, String)]) = {

    var order_sum = List[Int]()
    var success_sum = List[Int]()

    val tup: RDD[((String, String), List[Int], List[Int])] = row.map(x => {

      val pro: String = x._3
      val hour: String = x._2

      val success: Int = if (x._1.equals("0000")) 1 else 0

      order_sum :+= 1
      success_sum :+= success

      ((pro, hour), order_sum, success_sum)
    })


    val res: RDD[(String, String, Int)] = tup.map(x => (
      x._1._1,
      x._1._2,
      x._2.reduce((x, y) => x + y),
      x._3.reduce((x, y) => x + y)
    )).map(x => (
      x._1,
      x._2,
      x._3 - x._4
    ))

    res.foreachPartition(x => {

      //  获取连接
      val connection: Connection = ConnectPoolUtils.getConnections()

      //  将数据存储到Mysql
      x.foreach(x => {
        val sql = "insert into t1 (province, time, fail_sum) " +
          "values(?,?,?) ON DUPLICATE KEY UPDATE " +
          "province = VALUES(province), time = VALUES(time), fail_sum " +
          "= fail_sum + VALUES(fail_sum)"

        val statement: PreparedStatement = connection.prepareStatement(sql)

        statement.setString(1, x._1)

        statement.setString(2, x._2)

        statement.setInt(3, x._3)

        statement.executeUpdate()

        println("insert success")

        statement.close()

      })
      ConnectPoolUtils.resultConn(connection)
    })

  }

  def result03(row: RDD[(String, String)]) = {

    var order_sum = List[Int]()
    var success_sum = List[Int]()

    val tup: RDD[(String, List[Int], List[Int])] = row.map(x => {

      val province = x._2
      val success: Int = if (x._1.equals("0000")) 1 else 0

      order_sum :+= 1
      success_sum :+= success

      (province, order_sum, success_sum)
    })


    val res: RDD[(String, Int, Int)] = tup.map(x => (
      x._1,
      x._2.reduce((x, y) => x + y),
      x._3.reduce((x, y) => x + y)
    ))

    res.foreach(x => {

      //  获取连接
      val connection: Connection = ConnectPoolUtils.getConnections()

      //  将数据存储到Mysql
      val sql = "insert into t2 (province, order_sum, success_sum, success_rate) " +
        "values(?,?,?,?) ON DUPLICATE KEY UPDATE " +
        "province = VALUES(province), order_sum = (order_sum + VALUES(order_sum)), " +
        "success_sum = (success_sum + values(success_sum)), " +
        "success_rate = success_sum / order_sum"

      val statement: PreparedStatement = connection.prepareStatement(sql)

      val rate = x._3 / x._2

      statement.setString(1, x._1)
      statement.setInt(2, x._2)
      statement.setInt(3, x._3)
      statement.setDouble(4, rate)
      statement.executeUpdate()

      println("insert success")
      statement.close()
      ConnectPoolUtils.resultConn(connection)
    })
  }

  def result04(row: RDD[(Double, String)]) = {

    row.foreach(x =>{

      //  获取连接
      val connection: Connection = ConnectPoolUtils.getConnections()

      //  将数据存入数据库
      val sql = "insert into t3 (hour, money) values(?, ?) " +
        "on duplicate key update " +
        "hour = values(hour), money = money + values(money)"

      val statement: PreparedStatement = connection.prepareStatement(sql)

      statement.setString(1, x._2)
      statement.setDouble(2, x._1)
      statement.executeUpdate()

      println("insert success")
      statement.close()
      ConnectPoolUtils.resultConn(connection)

    })
  }
}
