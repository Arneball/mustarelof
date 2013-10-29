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
import play.api.libs.json._


trait Decoder[T ] {
  type Req = Request[AnyContent]
  def cookieName: String
  def cookieValue(t: T): String
  def getUserData(email: Option[String], code: String): Future[Option[T]]
  
  /** Looks in MongoDb for a user with T-specific credentials */
  def validUser(t: T)(implicit uf: UserFinder[T]): Future[Boolean] = MongoAdapter.userExists(t)
  
  /** Returns the cookie that is set when init is done */
  def initUserData(email: String, code: String)(implicit uf: UserFinder[T], w: Writes[T]): Future[Cookie] = for {
    Some(user) <- getUserData(Some(email), code)
    lasterror <- MongoAdapter.addOauth(email, user)
    if lasterror.ok
  } yield Decoder.toCookie(user)(this)
  
}

object Decoder {
  def toCookie[T](t: T)(implicit d: Decoder[T]): Cookie = Cookie(name=d.cookieName, value=d.cookieValue(t))
  
  implicit object FacebookDecoder extends Decoder[FbUser] {
    def cookieName = "fb"
      
    def cookieValue(f: FbUser) = f.facebook_id
      
    def getUserData(email: Option[String], code: String): Future[Option[FbUser]] = {
    val redirect_uri = s"http://skandal.dyndns.tv:9000/users/${email.get}/fblogin"
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
      } yield userData.fromJson[FbUser]
    }
  }
  
  implicit object GoogleDecoder extends Decoder[GoogleUser] {
    def cookieName = "google"
    def cookieValue(user: GoogleUser) = user.google_id
    
    def getUserData(email: Option[String], code: String): Future[Option[GoogleUser]] = {
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
        
        // then get the stuff
        userData <- postExternalWsHeaders("https://www.googleapis.com/oauth2/v2/userinfo", "Authorization" -> s"Bearer $accesskey")
        _ = Logger.debug("Google user data " + userData)
      } yield userData.fromJson[GoogleUser]
    }
  }
  
  implicit object LinkedinDecoder extends Decoder[LinkedinUser] {
    def cookieName= "linkedin"
    def cookieValue(u: LinkedinUser) = u.linkedin_id
    def getUserData(email: Option[String], code: String): Future[Option[LinkedinUser]] = {
      val redirect_uri = "http://skandal.dyndns.tv:9000/linkedinlogin"
      val params = List(
        "code" -> code,
        "redirect_uri" -> redirect_uri,
        "client_id" -> "or5btja04vjl",
        "client_secret" -> "GssQdekuoWhU35lQ",
        "grant_type" -> "authorization_code"
      )
      val url = "https://www.linkedin.com/uas/oauth2/accessToken"
      for {
        JsonAccessTokenBody(access_token, expires) <- postWithQstring(url, params: _*)
        _ = Logger.debug(s"Got access key $access_token")
        userDataXmlStr <- getExternalWs("https://api.linkedin.com/v1/people/~", "oauth2_access_token" -> access_token)
      } yield for {
        userDataXml <- userDataXmlStr.fromXml
        fname = Option(userDataXml("//first-name"))
        lname = Option(userDataXml("//last-name"))
        url = userDataXml("//site-standard-profile-request/url")
        user_id <- raw"id=(\d+)&".r.findFirstMatchIn(url).map{ _.group(1) }
        _ = Logger.debug(s"Fname: $fname, Lname: $lname, whole: $userDataXmlStr, user_id: $user_id")
      } yield LinkedinUser(user_id, fname, lname)
    }
  }
}