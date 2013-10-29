package oauth

import play.api.mvc._
import controllers2._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._
import utils._
import utils.WebService._
import play.api.mvc.Cookie
import controllers.UserFinder
import play.api.Logger
import play.api.libs.json._
import persistance.RedisController
import controllers._
import persistance.MongoAdapter

trait Decoder[T] {
  /** Name of the cookie that maps T specific credentials and cookie sent to user */
  def cookieName: String
  
  /** Given Email and code provided by oauth provider, return userData and expire of token */
  def getUserData(email: Option[String], code: String): Future[(Option[T], Int, String)]
  
  def login(email: String, code: String)(implicit uf: UserFinder[T], writes: Writes[T]): Future[Cookie] = for {
    (userOpt, timeout, access_key) <- getUserData(Some(email), code)
    user <- userOpt.future(new RestException("No user found", NoUser))
    cookie <- setRedisStuff(user, access_key, timeout)
  } yield cookie
  
  private def setRedisStuff(user: T, access_key: String, expires: Int)(implicit uf: UserFinder[T]) = for {
    redisOk <- RedisController(access_key) = uf.keyValue(user)
    expiresOk <- RedisController.expire(access_key, expires)
    if redisOk && expiresOk
  } yield Cookie(name=cookieName, value=access_key, maxAge=Some(expires))
  
  /** Returns the cookie that is set when init is done
   *  Needs a Writes[T] because MongoAdapter.addOauth needs to serialize T => Json
   *  Needs a UserFinder[T] because MongoAdapter needs to create an update query (which then uses the Writes[T])  
   */
  def initUserData(email: String, code: String)(implicit uf: UserFinder[T], w: Writes[T]): Future[Cookie] = for {
    (Some(user), timeout, access_key) <- getUserData(Some(email), code)
    lasterror <- MongoAdapter.addOauth(email, user)
    cookie <- setRedisStuff(user, access_key, timeout)
  } yield cookie
}

object Decoder {
  
  implicit object FacebookDecoder extends Decoder[FbUser] {
    override def cookieName = "fb"
      
    override def getUserData(email: Option[String], code: String) = {
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
      } yield (userData.fromJson[FbUser], expires, accesskey)
    }
  }
  
  implicit object GoogleDecoder extends Decoder[GoogleUser] {
    override def cookieName = "google"
    
    override def getUserData(email: Option[String], code: String) = {
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
      } yield (userData.fromJson[GoogleUser], expires, accesskey)
    }
  }
  
  implicit object LinkedinDecoder extends Decoder[LinkedinUser] {
    override def cookieName= "linkedin"
    override def getUserData(email: Option[String], code: String) = {
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
      } yield {
        val user = for {
          userDataXml <- userDataXmlStr.fromXml
          fname = Option(userDataXml("//first-name"))
          lname = Option(userDataXml("//last-name"))
          url = userDataXml("//site-standard-profile-request/url")
          user_id <- raw"id=(\d+)&".r.findFirstMatchIn(url).map{ _.group(1) }
          _ = Logger.debug(s"Fname: $fname, Lname: $lname, whole: $userDataXmlStr, user_id: $user_id")
        } yield LinkedinUser(user_id, fname, lname)
        (user, expires, access_token)
      }
    }
  }
}