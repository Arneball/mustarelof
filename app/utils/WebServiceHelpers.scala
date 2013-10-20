package utils

import scala.io.Source

object UrlDecoder {
  def unapply(body: String): Option[Map[String, String]] = {
    println(s"Body $body")
    val pairs = for {
      pair <- body.split("&")
      splitted = pair.split("=")
      if splitted.length == 2
      Array(key, value) = splitted
    } yield key -> value
    val map = pairs.toMap
    if(map.isEmpty) None else Some(map)
  }
}

object AccessTokenBody {
  def unapply(body: String): Option[(String, Int)] = for {
    map <- UrlDecoder.unapply(body)
    access_token <- map.get("access_token")
    expires <- map.get("expires")
  } yield access_token -> expires.toInt
}

object WebService {
  def getExternalWs(url: String, params: (String, String)*) = {
    val parsedParams = encodeUrl(params: _*) 
    Source.fromURL(url + parsedParams).mkString match {
      case null | "" => None
      case content => Some(content)
    }
  }
  
  def encodeUrl(pairs: (String, String)*): String = {
    def encode(str: String) = java.net.URLEncoder.encode(str, "utf-8")
    pairs.map{ case (k, v)=> s"$k=${encode(v)}"}.mkString("&")
  } 
}