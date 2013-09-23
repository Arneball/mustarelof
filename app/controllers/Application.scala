package controllers
import scala.concurrent.ExecutionContext.Implicits.global
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
object Application extends Controller {
  def index = Action{
    Ok(views.html.form())
  }
  private def AsyncAttachmentAction(f: Map[String, File] => Request[MultipartFormData[TemporaryFile]] => Result) = {
    Action(parse.multipartFormData){ r => 
      Async{
        scala.concurrent.future{
          val files = r.body.files.map{ fileref => fileref.key -> fileref.ref.file }.toMap
          println(s"Files $files")
          f(files)(r)
        }
      }
    }
  }
  
  val post = AsyncAttachmentAction{ files => implicit r =>
    val myaddress: AddressWithLocation = r.body.asFormUrlEncoded("myaddress").map{ OurGeoDecoder.decode }.head
    Logger.debug(s"Files ${r.body.file("file")}")
    
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

object AttachmentShit extends BodyParser[Array[Byte]]{
  def apply(req: RequestHeader): Iteratee[Array[Byte], Either[play.api.mvc.Result, Array[Byte]]] = ???
}