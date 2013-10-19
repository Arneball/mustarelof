package oauth

import play.api.mvc._
import controllers2._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import controllers.MongoAdapter
import utils._
import utils.WebService._
import play.api.mvc.Cookie
import controllers.UserFinder


trait Decoder[T] {
  type Req = Request[AnyContent]
  def cookieName: String
  
  /** Looks in MongoDb for a user with T-specific credentials */
  def validUser[T : UserFinder](t: T): Future[Boolean] = MongoAdapter.userExists(t)
  
  
  private def getCookieValue(r: Req): Option[String] = {
    r.cookies.find(_.name == cookieName).filter{ _.hasValidSign }.map{ _.value }
  }
  
  /** Returns the cookie that is set when init is done */
  def initUserData(email: String, code: String): Future[Cookie]
}

object FacebookDecoder extends Decoder[FbUser] {
  def cookieName = "fb"
  
  def initUserData(email: String, code: String) = for { 
    Some(fbuser) <- getFbData(code, s"http://skandal.dyndns.tv:9000/users/$email/fblogin")
    lasterror <- MongoAdapter.setFb(email=email, fbuser)
    if lasterror.ok
  } yield Cookie(cookieName, fbuser.facebook_id)

  private def getFbData(code: String, redirect_uri: String): Future[Option[FbUser]] = future {
    val params = List("redirect_uri" -> redirect_uri,
      "client_secret" -> "55093193de6f163ddf4825f0a81de170",
      "client_id" -> "184407735081979",
      "scope" -> "user_friends",
      "code" -> code)
    for {
      AccessTokenBody(accesskey, expires) <- getExternalWs(s"https://graph.facebook.com/oauth/access_token?", params: _*)
      userData <- getExternalWs(s"https://graph.facebook.com/me?", "access_token" -> accesskey) 
      fbUser <- userData.fromJson[FbUser]
    } yield fbUser
  }

}