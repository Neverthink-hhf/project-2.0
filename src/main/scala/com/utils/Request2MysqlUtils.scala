package com.utils

import java.sql.{Connection, PreparedStatement}

import org.apache.spark.rdd.RDD

object Request2MysqlUtils {


  def result01(row: RDD[(String, String)]) = {

    var success_sum = List[Int]()
    var order_sum = List[Int]()

    val tup: RDD[(String, List[Int], List[Int])] = row.map(x => {

      val pro = x._1

      val success: Int = if (x._2.equals("0000")) 1 else 0

      success_sum :+= success
      order_sum :+= 1
      (pro, success_sum, order_sum)
    })

    val res: RDD[(String, Int, Int)] = tup.map(x => {

      val success: Int = x._2.reduce((x, y) => x + y)
      val order: Int = x._2.reduce((x, y) => x + y)

      val fail = order - success


      (x._1, order,fail)
    })

    res.foreach(x => {

      //  获取Mysql连接
      val connection: Connection = ConnectPoolUtils.getConnections()

      val sql = "insert into t4 (province, order_sum, fail_sum, fail_rate) " +
        "values(?, ?, ?, ?) on duplicate key update " +
        "province = values(province), " +
        "order_sum = order_sum + values(order_sum), " +
        "fail_sum = fail_sum + values(fail_sum), " +
        "fail_rate = fail_sum / order_sum"

      val statement: PreparedStatement = connection.prepareStatement(sql)

      val rate = x._3 / x._2
      statement.setString(1, x._1)
      statement.setInt(2,x._2)
      statement.setInt(3,x._3)
      statement.setDouble(4, rate)

      statement.executeUpdate()

      println("insert success")
      statement.close()
      ConnectPoolUtils.resultConn(connection)
    })
  }

  def result02(row: RDD[(String, String, Double, String)]) = {

    val tup: RDD[(String, String, Double, Int)] = row.map(x => {

      val pro = x._2
      val money = x._3
      val min = x._4

      val success: Int = if (x._1.equals("0000")) 1 else 0

      (pro, min, money, success)
    })

    tup.foreach(x => {
      val connection: Connection = ConnectPoolUtils.getConnections()

      val sql = "insert into t5 (province, min, money, success_sum) " +
        "values(?, ?, ?, ?) on duplicate key update " +
        "province = values(province), " +
        "min = values(min), " +
        "money = money + values(money), " +
        "success_sum = success_sum +  values(success_sum)"

      val statement: PreparedStatement = connection.prepareStatement(sql)

      statement.setString(1, x._1)
      statement.setString(2,x._2)
      statement.setDouble(3,x._3)
      statement.setInt(4, x._4)

      statement.executeUpdate()

      println("insert success")
      statement.close()
      ConnectPoolUtils.resultConn(connection)
    })
  }
}
