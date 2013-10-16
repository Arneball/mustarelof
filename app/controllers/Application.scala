package controllers
import scala.concurrent.ExecutionContext.Implicits.global
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
import scala.concurrent.Future
import play.api.libs.Files.TemporaryFile
import java.io.File
import views.html.form
import play.api.libs.json.JsNumber
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsString
import scala.collection.concurrent.TrieMap
object Application extends PimpedController {
  def index = Action{
    Ok(views.html.form())
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
}