package com.ETL

import java.util.Properties

import com.utils.SchemaUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}

object Parquet2json {

  def main(args: Array[String]): Unit = {

    val Array(inputPath, outputPath) = args

    val conf = new SparkConf()
      .setAppName(this.getClass.getName)
      .setMaster("local[*]")

    val sc = new SparkContext(conf)

    val spark = SparkSession.builder()
      .config(conf).enableHiveSupport()
      .getOrCreate()

    val df: DataFrame = spark.read.parquet(inputPath)

    val tup: RDD[(String, String)] = df.rdd.map(arr => {
      val provincename = arr(24).toString
      val cityname = arr(25).toString

      (provincename, cityname)
    })

    val res: RDD[(Int, String, String)] = tup.map(x => (x, 1)).groupByKey
      .map(x => (x._1, x._2.size))
      .map(x => (x._2, x._1._1, x._1._2))

    import spark.implicits._

    val df1: DataFrame = res.toDF("ct", "provincename", "cityname")

    val prop = new Properties()
    prop.put("user", "root")
    prop.put("password", "123456")
    val url = "jdbc:mysql://localhost:3306/exercise?useSSL=false&characterEncoding=utf8"

//    df1.write.mode(SaveMode.Overwrite).jdbc(url, "city_info_22", prop)

//    df1.write.json(outputPath)

    df1.write.mode(SaveMode.Overwrite)
      .partitionBy("provincename", "cityname")
      .json("hdfs://hadoop02:8020/city-info")
  }
}
