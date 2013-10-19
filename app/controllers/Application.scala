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
import play.api.libs.json.JsNull
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

  def fblogin(email: String) = Action.async{ r =>
    val map = r.queryString
    (map.get("code"), map.get("error")) match {
      case (Some(Seq(code)), _) => 
        val futRes = for {
          cookie <- oauth.FacebookDecoder.initUserData(email=email, code=code)
        } yield Ok("All good fb").withSignedCookies(cookie)
        futRes
      case _ => 
        future{ Ok(r.queryString.toJson) }
    }
  }
  
  def dummy(email: String) = Action.async {
    val res = for {
      lasterror <- MongoAdapter.addDummy(email)
    } yield Ok(JsObj(
        "success" -> lasterror.ok, 
        "errtext" -> lasterror.errMsg.map{ new JsString(_)}.getOrElse(JsNull)
      ))
    res.recover{
      case e: Exception => Ok(JsObj("success" -> false, "errtext" -> e.getMessage))
    }
  }
  
  def hasData(email: String, provider: String) = Action.async{ r =>
    val futBoolean = provider match { 
      case "fb" => MongoAdapter.emailHas[FbUser](email)
      case _ => ???
    }
    futBoolean.map{ res => 
      Ok(JsObj("provider" -> provider, "user_has" -> res))
    }
  }
  def SecureAction(fun: Request[AnyContent] => Future[SimpleResult]) = Action.async { r =>
    val futureResult = for {
      fbCookie <- r.cookies.get("fb").future(new Exception("No such cookie"))
      cacheHit <- MongoAdapter.userExists(FbUser.withId(fbCookie.value))
      result <- fun(r)
    } yield result
    futureResult.recover{ 
      case _: Throwable => Redirect("/konsult#/login")
    }
  }
  
  val protectedContent = SecureAction{ r =>
    future { Ok("Secret content") }
  }
}