package controllers

import oauth.{FbUser, GoogleUser, LinkedinUser}
import play.api.libs.json.JsObject
import utils._
import play.api.libs.json.Writes

/** This trait exists so that we can do oauth provider specific queries
 *  For example, to see that we have facebook auth info, we must look for {facebook_id: {$exists: true}}
 *  We have a insert method to insert user specific data into the users' document
 */
trait UserFinder[T] {
  /** Key that T-specifc oauth-param is saved on in user object */
  def key: String
  
  /** What value the id key should be mapped to */
  def keyValue(t: T): String
  
  /** Create a lookup query with T, we match on the key being equal to keyValue(t) */
  def toQuery(t: T): JsObject = JsObj(key -> keyValue(t))
  
  /** Insertion query for type T, dummy implementation*/
  def insert(t: T)(implicit w: Writes[T]): JsObject = JsObj("$set" -> t.toJson)
  
  /** Check wether the email has any T-login info, we check that the user document with `email' contains key `key' */
  def hasData(email: String) = JsObj(
    "email" -> email, 
    key -> JsObj("$exists" -> true)
  )
}


object UserFinder {
  implicit object Fb extends UserFinder[FbUser] {
    def key = "facebook_id"
    def keyValue(u: FbUser) = u.facebook_id
  }
  implicit object Google extends UserFinder[GoogleUser] {
    def key = "google_id"
    def keyValue(u: GoogleUser) = u.google_id
  }
  implicit object Linkedin extends UserFinder[LinkedinUser] {
    def key = "linkedin_id"
    def keyValue(u: LinkedinUser) = u.linkedin_id
  }
}