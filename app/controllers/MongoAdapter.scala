package controllers

import play.api.Play.current
import reactivemongo.api.MongoDriver
import reactivemongo.api.MongoConnection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsObject
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import scala.concurrent.{ Future, future}
import utils._
import play.api.libs.json.JsValue
import reactivemongo.bson.BSONDocument
import oauth.FbUser

import reactivemongo.core.commands.LastError
object MongoAdapter {
  type FutureList = Future[List[JsValue]]
  private val connection: MongoConnection = {
    val driver = new MongoDriver()
    val nodes = List("ec2-54-229-139-146.eu-west-1.compute.amazonaws.com")
    driver.connection(nodes=nodes, authentications=Nil)
  }
  private val db = connection.db("test")
  private def collection(name: String) = db.collection(name)
  
  private def reports = collection("reports")
  def reports(user_id: String): FutureList = reports.find(JsObj("user_id" -> user_id)).cursor.collect[List]()
  
  def report(user_id: String, report_id: String): Future[Option[JsObject]] = 
    reports.find(JsObj("user_id" -> user_id, "_id" -> getId(report_id))).one
  
  def addReport(user_id: String, report: JsObject) = reports.insert(report + ("user_id", user_id))
  
  def updateReport(user_id: String, report_id: String, report: JsObject) = {
    val query = JsObj("_id" -> getId(report_id), "user_id" -> user_id)
    val newobj = report ++ query
    reports.update(selector=query, update=newobj)
  }
  
  def setFb(email: String, fbinfo: FbUser): Future[LastError] = {
    val sel = JsObj("email" -> email)
    val update = JsObj("$set" -> fbinfo.toJson)
    collection("users").update(selector=sel, update=update)
  }
  
  /** Returns true if user
   *  a) exists in play cache ( maybe we will use redis instead)
   *  b) exists in mongodb
   */
  def userExists[T](userData: T)(implicit userFinder: UserFinder[T]): Future[Boolean] = {
    val cacheOpt = play.api.cache.Cache.get(userFinder.toCacheKey(userData))
    cacheOpt.map{ _ => future{ true } }.getOrElse {
      collection("users").find{ userFinder.toQuery(userData) }.one.map{ _.isDefined }
    }
  }
}

trait UserFinder[T] {
  def toQuery(t: T): JsObject
  def toCacheKey(t: T): String
}

object UserFinder {
  implicit object Fb extends UserFinder[FbUser] {
    def toQuery(fbuser: FbUser) = JsObj("facebook_id" -> fbuser.facebook_id)
    def toCacheKey(fbuser: FbUser) = s"fb:${fbuser.facebook_id}"
  }
}