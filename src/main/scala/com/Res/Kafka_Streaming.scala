package com.Res

import java.lang

import com.alibaba.fastjson.{JSON, JSONObject}
import com.data.{JedisConnectionPool, JedisOffset}
import com.utils.ResultUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import redis.clients.jedis.Jedis

object Kafka_Streaming {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("offset").setMaster("local[*]")
      // 设置没秒钟每个分区拉取kafka的速率
      .set("spark.streaming.kafka.maxRatePerPartition", "100")
      // 设置序列化机制
      .set("spark.serlizer", "org.apache.spark.serializer.KryoSerializer")
    val ssc = new StreamingContext(conf, Seconds(3))

    val city: RDD[String] = ssc.sparkContext
      .textFile("E:\\= =\\项目\\充值平台实时统计分析\\city.txt")

    val city_map: collection.Map[String, String] = city
      .map(x => x.split(" ")).map(x => (x(0), x(1))).collectAsMap()

    val broadcast = ssc.sparkContext.broadcast(city_map)

    // 配置参数
    // 配置基本参数
    // 组名
    val groupId = "group01"
    // topic
    val topic = "test"
    // 指定Kafka的broker地址（SparkStreaming程序消费过程中，需要和Kafka的分区对应）
    val brokerList = "hadoop01:9092, hadoop02:9092, hadoop02:9092"
    // 编写Kafka的配置参数
    val kafkas = Map[String, Object](
      "bootstrap.servers" -> brokerList,
      // kafka的Key和values解码方式
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> groupId,
      // 从头消费
      "auto.offset.reset" -> "earliest",
      // 不需要程序自动提交Offset
      "enable.auto.commit" -> (false: lang.Boolean)
    )
    // 创建topic集合，可能会消费多个Topic
    val topics = Set(topic)
    // 第一步获取Offset
    // 第二步通过Offset获取Kafka数据
    // 第三步提交更新Offset
    // 获取Offset
    var fromOffset: Map[TopicPartition, Long] = JedisOffset(groupId)
    // 判断一下有没数据
    val stream: InputDStream[ConsumerRecord[String, String]] =
      if (fromOffset.size == 0) {
        KafkaUtils.createDirectStream(ssc,
          // 本地策略
          // 将数据均匀的分配到各个Executor上面
          LocationStrategies.PreferConsistent,
          // 消费者策略
          // 可以动态增加分区
          ConsumerStrategies.Subscribe[String, String](topics, kafkas)
        )
      } else {
        // 不是第一次消费
        KafkaUtils.createDirectStream(
          ssc,
          LocationStrategies.PreferConsistent,
          ConsumerStrategies.Assign[String, String](fromOffset.keys, kafkas, fromOffset)
        )
      }
    stream.foreachRDD({
      rdd =>
        val offestRange = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        // 业务处理
        val value: RDD[(Double, String, String, String, String, String)] =
          rdd.map(_.value()).map(x => JSON.parseObject(x))
          //  充值通知
          //  过滤掉非充值字段
          .filter(x => x.getString("serviceName").equals("reChargeNotifyReq"))
          .map(x => {
            //  接口服务名
            val chargefee: Double = x.getString("chargefee").toDouble

            //   bussinessRst 业务结果
            //    0000 成功，其它返回错误编码
            val bussinessRst: String = x.getString("bussinessRst")

            //  统计生成订单总时长

            //    结束充值时间
            val receiveNotifyTime: String = x.getString("receiveNotifyTime")

            //    开始充值时间,也就是业务流水号生成的时间
            val requestId: String = x.getString("requestId").substring(0, 17)

            //  获取小时数
            val hour: String = requestId.substring(0, 10)

            //  获取城市
            val provinceCode: String = x.getString("provinceCode")
            val province: String = broadcast.value.get(provinceCode).get



            (chargefee, bussinessRst, requestId, receiveNotifyTime, hour, province)
          })

        //  指标01
//        ResultUtils.Result01(value.map(x => (x._1, x._2, x._3, x._4)))

        //  指标02
//        ResultUtils.result02(value.map(x => (x._2, x._5, x._6)))

        //  指标03
//        ResultUtils.result03(value.map(x => (x._2, x._6)))

        //  指标04
        ResultUtils.result04(value.map(x => (x._1, x._5)))

        // 将偏移量进行更新
        val jedis = JedisConnectionPool.getConnection()
        for (or <- offestRange) {
          jedis.hset(groupId, or.topic + "-" + or.partition, or.untilOffset.toString)
        }
        jedis.close()
    })
    // 启动
    ssc.start()
    ssc.awaitTermination()


  }
}
