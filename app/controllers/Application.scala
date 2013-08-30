package controllers

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
object Application extends Controller {
  
  def index = Action {
    Ok(views.html.form())
  }
  
  val post = Action(parse.multipartFormData){ implicit r=>
    val le_file = r.body.file("file").get
    val myaddress = r.body.asFormUrlEncoded("myaddress").map{ OurGeoDecoder.decode }.head
    Logger.debug(s"Files ${r.body.file("file")}")
    val orders = ExcelParser.parse(new FileInputStream(le_file.ref.file))
    val ordersJson = JsArr(orders.map{_.toJson})
    val travelorder = OurGeoDecoder.travelling_salesmen(myaddress, orders.toSet)
    Ok{
      JsObj("rådata excel" -> ordersJson, "min location" -> myaddress.toJson, "travelorder" -> JsArr(travelorder.map{_.toJson}:_*))
    }
  }
}

object AttachmentShit extends BodyParser[Array[Byte]]{
  def apply(req: RequestHeader): Iteratee[Array[Byte], Either[play.api.mvc.Result, Array[Byte]]] = ???
}