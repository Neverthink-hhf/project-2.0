package com.utils

import org.apache.spark.rdd.RDD
import redis.clients.jedis.{Jedis, JedisPool}

object RedisUtils {

  val tableName = this.getClass.getName

  def write(log: RDD[(String, String)])= {

    val jedis = new Jedis("192.168.11.66",6379)

    log.collectAsMap().foreach(x => jedis.hset(tableName,x._1, x._2))

    jedis.close()
  }

  def read = {

    val jedis = new Jedis("192.168.11.66",6379)

    jedis
  }
}
