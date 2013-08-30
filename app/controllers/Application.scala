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
object Application extends Controller {
  
  def index = Action {
    Ok(views.html.form())
  }
  
  val post = Action(parse.multipartFormData){ implicit r=>
    Async{
      scala.concurrent.future{
        val le_file = r.body.file("file").get
        val myaddress = r.body.asFormUrlEncoded("myaddress").map{ OurGeoDecoder.decode }.head
        val myadressjson = myaddress.toJson
        Logger.debug(s"Files ${r.body.file("file")}")
        val orders = ExcelParser.parse(new FileInputStream(le_file.ref.file))
        val ordersJson = JsArr(orders.map{_.toJson})
        val travelorder = OurGeoDecoder.travelling_salesmen(myaddress, orders.toSet)
        val travelorderjson = JsArr(travelorder.map{_.toJson}:_*)
        val pimped = OurGeoDecoder.pimped_with_distance(travelorder)
        implicit object W extends Writes[(AddressWithLocation, Option[Double])]{
          def advrites(a: AddressWithLocation) = implicitly[Writes[AddressWithLocation]].writes(a).asInstanceOf[JsObject]
          def writes(t: (AddressWithLocation, Option[Double])) = t match {
            case (a, Some(b)) => advrites(a) + ("distanceToNext" -> b.toJson)
            case (a, _)       => advrites(a)
          }
        }
        Ok{
          JsObj(
            "rådata excel" -> ordersJson, 
            "min location" -> myadressjson, 
            "travelorder" -> travelorderjson,
            "pimped with distance" -> pimped.toJson)
        }
      }
    }
  }
}

object AttachmentShit extends BodyParser[Array[Byte]]{
  def apply(req: RequestHeader): Iteratee[Array[Byte], Either[play.api.mvc.Result, Array[Byte]]] = ???
}