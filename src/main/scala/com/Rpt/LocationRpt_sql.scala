package com.Rpt

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SQLContext}

object LocationRpt_sql {
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

    val sc = new SparkContext(conf)
    val sQLContext = new SQLContext(sc)

    // 5.获取数据
    val logs: DataFrame = sQLContext.read.parquet(inputPath)

    // 6.数据处理统计各个指标
    val tup: RDD[(String, String, Int, Int, Int, Int, Int, Int, Int, Double, Double)] = logs.map(row => {

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

      (provincename, cityname, requestmode,
        processnode, iseffective, isbilling,
        isbid, iswin, adorderid, winprice, adpayment)
    })
    tup

    val df: DataFrame = sQLContext.createDataFrame(tup)
      .toDF("provincename", "cityname",
        "requestmode", "processnode",
        "iseffective", "isbilling",
        "isbid", "iswin", "adorderid", "winprice", "adpayment")

    df.registerTempTable("tmp")

//    sQLContext.sql("select * from tmp").show()

    sQLContext.sql("select provincename, cityname, " +
      "sum(case when requestmode = 1 and processnode >= 1 then 1 else 0 end) org_num, " +
      "sum(case when requestmode = 1 and processnode >= 2 then 1 else 0 end) val_num, " +
      "sum(case when requestmode = 1 and processnode = 3 then 1 else 0 end) ad_num, " +
      "sum(case when iseffective = 1 and isbilling = 1 and isbid = 1 then 1 else 0 end) bid_num, " +
      "sum(case when iseffective = 1 and isbilling = 1 and iswin = 1 and adorderid != 0 then 1 else 0 end) bidwin_num, " +
      "sum(case when requestmode = 2 and iseffective = 1 then 1 else 0 end) show_num, " +
      "sum(case when requestmode = 3 and iseffective = 1 then 1 else 0 end) click_num, " +
      "sum(case when iseffective = 1 and isbilling = 1 and iswin = 1 then winprice else 0 end)/1000 ad_consume, " +
      "sum(case when iseffective = 1 and isbilling = 1 and iswin = 1 then adpayment else 0 end)/1000 ad_cost " +
      "from tmp group by provincename, cityname").show()


    sc.stop()
  }
}
