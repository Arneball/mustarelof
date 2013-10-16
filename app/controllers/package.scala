import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Action
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results
import utils._
import play.api.mvc.SimpleResult
package object controllers2 {
  
  /** Convenience cache */
  val internalServerError = Results.InternalServerError(JsObj("failure"->true))
  
  val genericErrorHandler = {
    case _: Throwable => internalServerError
  }: PartialFunction[Throwable, SimpleResult]
}