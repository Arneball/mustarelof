package controllers
import scala.concurrent.ExecutionContext.Implicits.global
import oauth._
import play.api.Play.current
import utils._
import play.api._
import play.api.mvc._
import java.io.ByteArrayInputStream
import utils.OurGeoDecoder
import utils.ExcelParser
import play.api.libs.json.JsArray
import utils._
import java.io.FileInputStream
import scala.io.Source
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import scala.concurrent.{future, Future}
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.Files.TemporaryFile
import java.io.File
import views.html.form
import play.api.libs.json.JsNumber
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsString
import scala.collection.concurrent.TrieMap
import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import controllers2._
import net.sf.ehcache.Cache
object Application extends PimpedController {
  def index = Action{ r =>
    Logger.debug(s"Valid sign: ${r.cookies("apa").hasValidSign}")
    Ok(views.html.form()).withSignedCookies(Cookie("apa", "svin"))
  }
  def konsult = Action{
    Ok(views.html.konsult())
  }
  object Car {
    implicit val format: Format[Car] = Json.format[Car]
  }
  case class Car(manufacturer: String, cubics: Int, maxSpeed: Int)
  val cars = Action{
    val cars = List(Car("Saab", 1400, 160), Car("Volvo", 2200, 190))
    val jsoncars = cars.map{ _.toJson }
    val responsedata = JsArray(jsoncars)
    Ok(responsedata)
  }
  import utils.JsObjectWrapper
  
  private val urlcache = new TrieMap[String, (String, String)]
  def minify = JsAction{ obj => r =>
    val urls = for {
      JsArray(urlarray) <- (obj \/ "urls").toSeq
      JsString(url) <- urlarray
    } yield url
    val minified = MinifierService.minify(urls: _*)
    
    //save it
    def str = util.Random.alphanumeric.take(10).mkString
    val etag = str
    val url = str
    urlcache += url -> (etag, minified)
    scala.concurrent.future{
      Ok(JsObj("url" -> url))
    }
  }
  
  def getJs(url: String) = Action{ r =>
    (r.headers.get("ETAG"), urlcache.get(url)) match {
      case (Some(etag), Some((content, savedEtag))) if etag == savedEtag => NotModified
      case (None, Some((etag, content))) => Ok(content).withHeaders("ETAG" -> etag)
      case _ => NotFound("bohoo")
    } 
  }
  
  /** Handy method to create a AsyncAction pimped with files
   *  Used like 
   *  {{{
   *  def postHandler = AsynchAttachmentAction{ files => request => 
   *    Ok(files.keys.mkString) 
   *  }
   *  }}}
   */
  private def AsyncAttachmentAction(fun: Map[String, File] => Request[MultipartFormData[TemporaryFile]] => SimpleResult) = {
    Action.async(parse.multipartFormData){ requestPimpedWithFiles => 
      scala.concurrent.future{
        val files = requestPimpedWithFiles.body.files.map{ fileref => fileref.key -> fileref.ref.file }.toMap
        println(s"Files $files")
        fun(files)(requestPimpedWithFiles)
      }
    }
  }
  
  /** Used by angular js webservice client */
  val testJson = Action{
    val res = JsArray((1 to 10).map{ int => JsNumber(int) })
    Ok(res)
  }
  
  val post = AsyncAttachmentAction{ files => request =>
    val myaddress: AddressWithLocation = request.body.asFormUrlEncoded("myaddress").map{ OurGeoDecoder.decode }.head
    Logger.debug(s"Files ${request.body.file("file")}")
    
    val orders = ExcelParser.parse(new FileInputStream(files("file")))
    val travelorder = OurGeoDecoder.travelling_salesmen(myaddress, orders.toSet)
    
    type PimpedType = (AddressWithLocation, Option[Double])
    val pimped: Seq[PimpedType] = OurGeoDecoder.pimped_with_distance(travelorder)
    
    /** A Json serializer for Tuple2[AddressWithLocation, Option[Double]] */
    implicit object W extends Writes[PimpedType]{
      def advrites(a: AddressWithLocation) = implicitly[Writes[AddressWithLocation]].writes(a).asInstanceOf[JsObject]
      def writes(t: (AddressWithLocation, Option[Double])) = t match {
        case (a, Some(b)) => advrites(a) + ("distanceToNext" -> b.toJson)
        case (a, _)       => advrites(a)
      }
    }
    
    Ok{
      JsObj(
        "rådata excel" -> (orders: JsArray), // implicitly converts Seq[_<%JsValue] => JsArray
        "min location" -> myaddress.toJson, 
        "travelorder" -> (travelorder: JsArray), // implicitly converts Seq[_<%JsValue] => JsArray
        "pimped with distance" -> pimped.toJson)
    }
  }
  implicit object StringMap extends Writes[Map[String, Seq[String]]] {
    def writes(map: Map[String, Seq[String]]) = {
      map.foldLeft(JsObject(Nil)) { 
        case (acc, (key, Seq(value))) => acc + (key, value)
        case (acc, (key, values))     => acc + (key, values.map{ new JsString(_) })
      }
    }
  }

  def encodeUrl(pairs: (String, String)*): String = {
    def encode(str: String) = java.net.URLEncoder.encode(str, "utf-8")
    pairs.map{ case (k, v)=> s"$k=${encode(v)}"}.mkString("&")
  } 
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
  
  def getExternalWs(url: String, params: (String, String)*) = {
    val parsedParams = encodeUrl(params: _*) 
    Source.fromURL(url + parsedParams).mkString
  }
  
  def fblogin(email: String) = Action.async{ r =>
    val map = r.queryString
    (map.get("code"), map.get("error")) match {
      case (Some(Seq(code)), _) => 
        val futRes = for {
          user <- initFbUser(email=email, code=code)
        } yield Ok(user.toJson).withSignedCookies(Cookie("fb", user.facebook_id))
        futRes
      case _ => 
        future{ Ok(r.queryString.toJson) }
    }
  }
  
  def userExists(fb: String): Future[Boolean] = { 
    val tmp = play.api.cache.Cache.getAs[String](fb)
    if(tmp.isDefined) { 
      future{ true } 
    } else {
      MongoAdapter.fbUserExists(fb)
    }
  }
  
  val protectedContent = Action.async{ r =>
    val res = for {
      fbid <- r.cookies.get("fb").future(new Exception("No such cookie"))
      cacheHit <- userExists(fbid.value) 
    } yield {
      Ok("If u can see this then you are pro")
    }
    res.recover{ 
      case _: Throwable => Redirect("/konsult#/login")
    }
  } 
  
  
  private def initFbUser(email: String, code: String) = for {
    Some(fbuser) <- getFbData(code, s"http://skandal.dyndns.tv:9000/users/$email/fblogin")
    lasterror <- MongoAdapter.setFb(email=email, fbuser)
    if lasterror.ok
  } yield fbuser
  
  private def getFbData(code: String, redirect_uri: String): Future[Option[FbUser]] = {
    future{
      val params = List("redirect_uri" -> redirect_uri,
        "client_secret" -> "55093193de6f163ddf4825f0a81de170",
        "client_id" -> "184407735081979",
        "scope" -> "user_friends",
        "code" -> code)
      
      getExternalWs(s"https://graph.facebook.com/oauth/access_token?", params: _*) match {
        case AccessTokenBody(accesskey, expires) =>
          getExternalWs(s"https://graph.facebook.com/me?", "access_token" -> accesskey).fromJson[FbUser]
        case a => None
      }
    }
  }
}