package controllers

import play.api.mvc.Controller
import play.api.libs.json.JsObject
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent

trait PimpedController extends Controller {
    /** This method will extract an json object from the request, provide a function JsObject => Req => Res */
  def JsAction(fun: JsObject => Request[AnyContent] => Result ) = Action{ req =>
    val jsonvalueopt = req.body.asJson
    val jsonobjectopt = jsonvalueopt.flatMap{ _.asOpt[JsObject] }
    jsonobjectopt.map{ obj => fun(obj - "id")(req) }.getOrElse(BadRequest("No parcelable json object found"))
  }
}