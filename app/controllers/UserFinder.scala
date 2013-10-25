package controllers

import oauth.FbUser
import play.api.libs.json.JsObject
import utils._
import oauth.GoogleUser


trait UserFinder[T] {
  /** Key that T-specifc oauth-param is saved on in user object */
  def key: String
  
  /** Create a lookup query with T */
  def toQuery(t: T): JsObject
  
  /** Insertion query for type T */
  def insert(t: T): JsObject 
  
  /** Create a cache lookup key for T */
  def toCacheKey(t: T): String
  
  /** Check wether the email has any T-login info */
  def hasData(email: String) = JsObj(
    "email" -> email, 
    key -> JsObj("$exists" -> true)
  )
}


object UserFinder {
  implicit object Fb extends UserFinder[FbUser] {
    def key = "facebook_id"
    def toQuery(fbuser: FbUser) = JsObj(key -> fbuser.facebook_id)
    def toCacheKey(fbuser: FbUser) = s"fb:${fbuser.facebook_id}"
    def insert(fbuser: FbUser) = JsObj("$set" -> fbuser.toJson)
  }
  implicit object Google extends UserFinder[GoogleUser] {
    def key = "google_id"
    def toQuery(u: GoogleUser) = JsObj(key -> u.google_id)
    def toCacheKey(u: GoogleUser) = s"google:${u.google_id}"
    def insert(u: GoogleUser) = JsObj("$set" -> u.toJson)
  }
}