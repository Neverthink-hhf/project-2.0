package com.Rpt

import java.util.Properties

import com.typesafe.config.ConfigFactory
import com.utils.RptUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, Dataset, SQLContext, SaveMode, SparkSession}

object LocationRpt {
  def main(args: Array[String]): Unit = {

    // 1.判断路径是否正确
    if(args.length != 2){
      println("目录参数不正确，退出程序")
      sys.exit()
    }

    // 2.创建一个集合保存输入和输出的目录
    val Array(inputPath, outputPath) = args
    val conf = new SparkConf()
      .setAppName(this.getClass.getName)
      .setMaster("local[*]")
     // 3.设置序列化方式，采用Kyro方式，比默认序列化方式高
      .set("spark.serializer","org.apache.spark.serializer.KryoSerializer")

    // 4.创建执行入口

    val spark = SparkSession.builder().config(conf).getOrCreate()

    // 5.获取数据
    val logs: DataFrame = spark.read.parquet(inputPath)

    import spark.implicits._

    // 6.数据处理统计各个指标
    val tup: Dataset[((String, String), List[Double])] = logs.map(row => {

      // 6.1.把需要的字段全部取到
      val requestmode: Int = row.getAs[Int]("requestmode")
      val processnode: Int = row.getAs[Int]("processnode")
      val iseffective: Int = row.getAs[Int]("iseffective")
      val isbilling: Int = row.getAs[Int]("isbilling")
      val isbid: Int = row.getAs[Int]("isbid")
      val iswin: Int = row.getAs[Int]("iswin")
      val adorderid: Int = row.getAs[Int]("adorderid")
      val winprice: Double = row.getAs[Double]("winprice")
      val adpayment: Double = row.getAs[Double]("adpayment")

      // 6.2.取key值，地域的省市

      val provincename: String = row.getAs[String]("provincename")
      val cityname: String = row.getAs[String]("cityname")

      //  根据指标的不同，需要创建三个对应的方法来处理九个指标

      //  此方法处理请求数
      val list1: List[Double] = RptUtils.request(requestmode, processnode)

      //  此方法处理展示点击数
      val list2: List[Double] = RptUtils.click(requestmode, iseffective)

      //  此方法处理竞价操作
      val list3: List[Double] = RptUtils.Ad(iseffective, isbilling, isbid, iswin, adorderid, winprice, adpayment)


      val list4: List[Double] = list1 ++ list2 ++ list3
      // 6.3.返回元组
      ((provincename, cityname), list4)
    })

    val tup1: RDD[(String, String, Double, Double, Double, Double, Double, Double, Double, Double, Double)] =
      tup.rdd.groupByKey
      .map(x => (x._1, x._2.toList.reduce((x, y) => x.zip(y).map(x => x._1 + x._2))))
      .map(x => (x._1._1, x._1._2, x._2(0), x._2(1), x._2(2),
        x._2(3), x._2(4), x._2(5), x._2(6), x._2(7), x._2(8)))

    val res: RDD[(String, String, Double, Double, Double, Double, Double, Double, Double, Double, Double)] =
      tup1.map(x => (x._1, x._2, x._3, x._4, x._5, x._8, x._9, x._6, x._7, x._10, x._11))


    val df: DataFrame = res.toDF("provincename", "cityname", "org_num", "val_num", "ad_num",
      "bid_num", "bidwin_num", "show_num", "click_num","ad_consume", "ad_cost")

    df.show()

//    val load = ConfigFactory.load()
//    val prop = new Properties()
//    prop.setProperty("user", load.getString("jdbc.user"))
//    prop.setProperty("password", load.getString("jdbc.password"))
//    df.write.mode(SaveMode.Overwrite).jdbc(load.getString("jdbc.url"), "LocationRpt", prop)

    spark.stop()
  }
}
