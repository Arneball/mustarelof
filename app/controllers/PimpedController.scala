package controllers

import play.api.mvc.Controller
import play.api.libs.json.JsObject
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.SimpleResult
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import utils._
import scala.collection.concurrent.RestartException
trait PimpedController extends Controller {
    /** This method will extract an json object from the request, provide a function JsObject => Req => Res */
  def JsAction(fun: JsObject => Request[AnyContent] => Future[SimpleResult] ) = Action.async{ req =>
    val jsonvalueopt = req.body.asJson
    val jsonobjectopt = jsonvalueopt.flatMap{ _.asOpt[JsObject] }
    val fut = for {
      obj <- jsonobjectopt.future(RestException("No parcelable json found"))
      res <- fun(obj - "id")(req)
    } yield res
    fut.recover{
      case RestException(mess) => BadRequest(mess)
    }
  }
}

case class RestException(message: String) extends Exception