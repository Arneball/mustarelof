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
import play.api.Logger


trait Decoder[T] {
  type Req = Request[AnyContent]
  def cookieName: String
  def cookieValue(t: T): String
  
  /** Looks in MongoDb for a user with T-specific credentials */
  def validUser[T : UserFinder](t: T): Future[Boolean] = MongoAdapter.userExists(t)
  
  
  private def getCookieValue(r: Req): Option[String] = {
    r.cookies.find(_.name == cookieName).filter{ _.hasValidSign }.map{ _.value }
  }
  
  /** Returns the cookie that is set when init is done */
  def initUserData(email: String, code: String)(implicit uf: UserFinder[T]): Future[Cookie] = for {
    Some(user) <- getUserData(email, code)
    lasterror <- MongoAdapter.addOauth(email, user)
    if lasterror.ok
  } yield Cookie(cookieName, cookieValue(user))
  
  /** Abstract that returns a future oauthuser */
  def getUserData(email: String, code: String): Future[Option[T]]
}

object FacebookDecoder extends Decoder[FbUser] {
  def cookieName = "fb"
    
  def cookieValue(f: FbUser) = f.facebook_id
  
  def getUserData(email: String, code: String): Future[Option[FbUser]] = {
    val redirect_uri = s"http://skandal.dyndns.tv:9000/users/$email/fblogin"
    val params = List("redirect_uri" -> redirect_uri,
      "client_secret" -> "55093193de6f163ddf4825f0a81de170",
      "client_id" -> "184407735081979",
      "scope" -> "user_friends",
      "code" -> code)
    for {
      // first get access token
      AccessTokenBody(accesskey, expires) <- getExternalWs(s"https://graph.facebook.com/oauth/access_token", params: _*)
      _ = Logger.debug(s"have access token $accesskey")
      // then get userData
      userData <- getExternalWs(s"https://graph.facebook.com/me", "access_token" -> accesskey)
      _ = Logger.debug(s"Userdata: $userData")
      // then parse the json to an FbUser
    } yield for {
      fbUser <- userData.fromJson[FbUser]
    } yield fbUser
  }
}

object GoogleDecoder extends Decoder[GoogleUser] {
  def cookieName = "google"
  def cookieValue(user: GoogleUser) = user.google_id
  
  def getUserData(email: String, code: String): Future[Option[GoogleUser]] = {
    val redirect_uri = s"http://skandal.dyndns.tv:9000/gmaillogin"
    val params = List(
    	"redirect_uri" -> redirect_uri,
      "client_secret" -> "nPmNeCtO09UfMrBKwRFzMB2H",
      "client_id" -> "311906667213.apps.googleusercontent.com",
      "scope" -> "https://www.googleapis.com/auth/userinfo.profile",  
      "code" -> code,
      "grant_type" -> "authorization_code"
    )
    for {
      // first get accesstoken
      JsonAccessTokenBody(accesskey, expires) <- postExternalWs("https://accounts.google.com/o/oauth2/token", params: _*) 
    } yield Some(GoogleUser(accesskey))
  }
}