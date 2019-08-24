package com.utils

/**
  * 指标方法
  */
object RptUtils {

  // 此方法处理请求数

  def request(requestmode: Int, processnode: Int):List[Double]={

    var org_num = 0
    var val_num = 0
    var ad_num = 0

    if(requestmode == 1 && processnode >= 1){
      org_num += 1
    }

    if(requestmode == 1 && processnode >= 2){
      val_num += 1
    }

    if(requestmode == 1 && processnode == 3){
      ad_num += 1
    }

    List(org_num, val_num, ad_num)
  }

  // 此方法处理展示点击数

  def click(requestmode: Int, iseffective: Int):List[Double]={

    var show_num = 0
    var click_num = 0

    if(requestmode == 2 && iseffective == 1){
      show_num += 1
    }

    if(requestmode == 3 && iseffective == 1){
      click_num += 1
    }

    List(show_num, click_num)
  }

  // 此方法处理竞价操作

  def Ad(iseffective: Int, isbilling: Int, isbid:Int, iswin: Int, adorderid: Int,
         winprice: Double, adpayment: Double):List[Double]={

    var bid_num = 0
    var bidwin_num = 0
    var ad_consume = 0.0
    var ad_cost = 0.0

    if(iseffective == 1 && isbilling == 1 && isbid == 1){
      bid_num += 1
    }

    if(iseffective == 1 && isbilling == 1 && iswin == 1 && adorderid != 0){
      bidwin_num += 1
    }

    if(iseffective == 1 && isbilling == 1 && iswin == 1){
      ad_consume = winprice/1000
    }

    if(iseffective == 1 && isbilling == 1 && iswin == 1){
      ad_cost = adpayment/1000
    }

    List(bid_num, bidwin_num, ad_consume, ad_cost)
  }

}
