package utils
import scala.io.Source
import play.api.Logger
import java.net.URLConnection
import java.net.HttpURLConnection
import play.api.libs.json.Json
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber

object UrlDecoder {
  def unapply(body: String): Option[Map[String, String]] = {
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

object JsonAccessTokenBody {
  def unapply(body: String): Option[(String, Int)] = for {
    js <- body.parse
    JsString(accTok) <- js \/ "access_token"
    JsNumber(exp) <- js \/ "expires_in"
  } yield accTok -> exp.toInt
}
object AccessTokenBody {
  def unapply(body: String): Option[(String, Int)] = for {
    map <- UrlDecoder.unapply(body)
    access_token <- map.get("access_token")
    expires <- map.get("expires")
  } yield access_token -> expires.toInt
}

object WebService {
  import scala.concurrent.ExecutionContext.Implicits._
  import dispatch._
  def getExternalWs(purl: String, params: (String, String)*): Future[String] =
    Http(url(purl).GET << params).map{ _.getResponseBody }
  
  def postExternalWs(purl: String, params: (String, String)*): Future[String] =
    Http(url(purl).POST << params).map{ _.getResponseBody }
  
}