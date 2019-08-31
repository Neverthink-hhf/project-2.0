package test

import java.lang

import com.alibaba.fastjson.{JSON, JSONObject}
import com.data.{JedisConnectionPool, JedisOffset}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis


/**
  * Redis管理Offset
  */
object test {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("offset").setMaster("local[*]")
      // 设置没秒钟每个分区拉取kafka的速率
      .set("spark.streaming.kafka.maxRatePerPartition","100")
      // 设置序列化机制
      .set("spark.serlizer","org.apache.spark.serializer.KryoSerializer")
    val ssc = new StreamingContext(conf,Seconds(3))
    // 配置参数
    // 配置基本参数
    // 组名
    val groupId = "JsonData"
    // topic
    val topic = "JsonData"
    // 指定Kafka的broker地址（SparkStreaming程序消费过程中，需要和Kafka的分区对应）
    val brokerList = "hadoop01:9092, hadoop02:9092, hadoop02:9092"
    // 编写Kafka的配置参数
    val kafkas = Map[String,Object](
      "bootstrap.servers"->brokerList,
      // kafka的Key和values解码方式
      "key.deserializer"-> classOf[StringDeserializer],
      "value.deserializer"-> classOf[StringDeserializer],
      "group.id"->groupId,
      // 从头消费
      "auto.offset.reset"-> "earliest",
      // 不需要程序自动提交Offset
      "enable.auto.commit"-> (false:lang.Boolean)
    )
    // 创建topic集合，可能会消费多个Topic
    val topics = Set(topic)
    // 第一步获取Offset
    // 第二步通过Offset获取Kafka数据
    // 第三步提交更新Offset
    // 获取Offset
    var fromOffset:Map[TopicPartition,Long] = JedisOffset(groupId)
    // 判断一下有没数据
    val stream :InputDStream[ConsumerRecord[String,String]] =
      if(fromOffset.size == 0){
        KafkaUtils.createDirectStream(ssc,
          // 本地策略
          // 将数据均匀的分配到各个Executor上面
          LocationStrategies.PreferConsistent,
          // 消费者策略
          // 可以动态增加分区
          ConsumerStrategies.Subscribe[String,String](topics,kafkas)
        )
      }else{
        // 不是第一次消费
        KafkaUtils.createDirectStream(
          ssc,
          LocationStrategies.PreferConsistent,
          ConsumerStrategies.Assign[String,String](fromOffset.keys,kafkas,fromOffset)
        )
      }
    stream.foreachRDD({
      rdd=>
        val offestRange = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        // 业务处理
        val tup: RDD[(String, Double, (String, String))] = rdd.map(_.value()).map(x => {
          val json: JSONObject = JSON.parseObject(x)

          //  天
          val day: String = json.getString("date").substring(0, 10)

          //  月
          val month: String = json.getString("date").substring(0, 7)

          //  金额
          val money: Double = json.getString("money").toDouble

          //  手机号
          val phoneNum: String = json.getString("phoneNum")

          (day, money, (month, phoneNum))
        })

//        tup.foreach(x => {
//          val jedis: Jedis = JedisConnectionPool.getConnection()
//
//          jedis.hincrByFloat("money_sum", x._1, x._2)
//          println("insert success")
//
//          jedis.close()
//
//        })

//        tup.map(x => (x._3, x._2)).foreach(x => {
//          val jedis: Jedis = JedisConnectionPool.getConnection()
//
//          jedis.hincrByFloat("avg_month", x._1.toString(), x._2)
//          println("insert success")
//
//          jedis.close()
//        })


        // 将偏移量进行更新
        val jedis = JedisConnectionPool.getConnection()
        for (or<-offestRange){
          jedis.hset(groupId,or.topic+"-"+or.partition,or.untilOffset.toString)
        }
        jedis.close()
    })
    // 启动
    ssc.start()
    ssc.awaitTermination()




  }
}
