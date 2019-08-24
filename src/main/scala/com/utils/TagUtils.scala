package com.utils

import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.Row

/**
  * 标签工具类
  */
object TagUtils {

    //  过滤需要的字段
    val OneUserId=
      """
        | imei !='' or mac !='' or openudid !='' or androidid !='' or idfa !='' or
        | imeimd5 !='' or macmd5 !='' or openudidmd5 !='' or androididmd5 !='' or idfamd5 !='' or
        | imeisha1 !='' or macsha1 !='' or openudidsha1 !='' or androididsha1 !='' or idfasha1 !=''
      """.stripMargin

    //  去除唯一不为空的Id
    def getOneUserId(row: Row): String ={
      row match {
        case v if StringUtils.isNotBlank(v.getAs[String]("imei")) => "IM: "+v.getAs[String]("imei")
        case v if StringUtils.isNotBlank(v.getAs[String]("mac")) => "MAC: "+v.getAs[String]("mac")
        case v if StringUtils.isNotBlank(v.getAs[String]("openudid")) => "OP: "+v.getAs[String]("openudid")
        case v if StringUtils.isNotBlank(v.getAs[String]("androidid")) => "Android: "+v.getAs[String]("androidid")
        case v if StringUtils.isNotBlank(v.getAs[String]("idfa")) => "IDFA: "+v.getAs[String]("idfa")

        case v if StringUtils.isNotBlank(v.getAs[String]("imeimd5")) => "IM5: "+v.getAs[String]("imeimd5")
        case v if StringUtils.isNotBlank(v.getAs[String]("macmd5")) => "MAC5: "+v.getAs[String]("macmd5")
        case v if StringUtils.isNotBlank(v.getAs[String]("openudidmd5")) => "OP5: "+v.getAs[String]("openudidmd5")
        case v if StringUtils.isNotBlank(v.getAs[String]("androididmd5")) => "Android5: "+v.getAs[String]("androididmd5")
        case v if StringUtils.isNotBlank(v.getAs[String]("idfamd5")) => "IDFA5: "+v.getAs[String]("idfamd5")

        case v if StringUtils.isNotBlank(v.getAs[String]("imeisha1")) => "IMsha1: "+v.getAs[String]("imeisha1")
        case v if StringUtils.isNotBlank(v.getAs[String]("macsha1")) => "MACsha1: "+v.getAs[String]("macsha1")
        case v if StringUtils.isNotBlank(v.getAs[String]("openudidsha1")) => "OPsha1: "+v.getAs[String]("openudidsha1")
        case v if StringUtils.isNotBlank(v.getAs[String]("androididsha1")) => "Androidsha1: "+v.getAs[String]("androididsha1")
        case v if StringUtils.isNotBlank(v.getAs[String]("idfasha1")) => "IDFAsha1: "+v.getAs[String]("idfasha1")
      }
    }
}
