import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Action
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results
import utils._
import play.api.mvc.SimpleResult
import scala.concurrent.Future
import play.api.mvc.Cookie
package object controllers2 {
  
  /** Convenience cache */
  val internalServerError = Results.InternalServerError(JsObj("failure"->true))
  
  /** Catches all throwables and returns internalservererror */
  val genericErrorHandler = {
    case _: Throwable => internalServerError
  }: PartialFunction[Throwable, SimpleResult]
  
//  def AuthAction(f: Request[AnyContent] => Future[SimpleResult]) = Action.async {r =>
//    
//  }
  
  implicit class ResponseWrapper(val r: SimpleResult) extends AnyVal {
    def withSignedCookies(cookies: Cookie*) = {
      val secureCookies = cookies.map{ _.sign }
      r.withCookies(secureCookies: _*)
    }
  }
  
  implicit class CookieWrapper(val c: Cookie) extends AnyVal {
    def sign = c.copy(value=s"${c.value}.${c.value.sign}")
    
    def hasValidSign: Boolean = {
      val cval: String = c.value
      val (value, signature) = cval.splitAt(cval.lastIndexOf("."))
      value.sign == signature.substring(1) 
    }
  }
}