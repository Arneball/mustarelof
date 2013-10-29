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
import play.api.libs.json.Writes
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
  
  /** Returns true if user
   *  a) exists in play cache ( maybe we will use redis instead)
   *  b) exists in mongodb
   */
  def userExists[T](userData: T)(implicit uf: UserFinder[T]): Future[Boolean] = {
    collection("users").find{ uf.toQuery(userData) }.one.map{ _.isDefined }
  }
  
  def addOauth[T : Writes](email: String, oauthInfo: T)(implicit uf: UserFinder[T]): Future[LastError] = {
    val sel = JsObj("email" -> email)
    val update = uf.insert(oauthInfo)
    collection("users").update(selector=sel, update=update, upsert=false)
  }
  
  /** Check if email is mapped to any Oauth data
   *  UserFinder[T] has method hasData(String) that returns a search query used in collection("colname").find( _ )
   *  .one returns an Future[Option[JsObject]]
   *  .map { _.isDefined } returns Future[Boolean]
   */
  def emailHas[T](email: String)(implicit uf: UserFinder[T]): Future[Boolean] = {
    val res = collection("users").find{ uf.hasData(email) }
    val futOptJsObj = res.one
    futOptJsObj.map { _.isDefined }
  }
  
  /** Collection "users" has uniquness on property "email", so an exception will be thrown if
   *  We attempt to enter an equal email
   */
  def addDummy(email: String): Future[LastError] = collection("users").insert(JsObj("email" -> email))
  
  /** Find an user given some provider specific properties
   *  For example if we do getUser(FbUser) then it will find something in userdb where { facebook_id: FbUser.facebook_id }
   */
  def getUser[T](t: T)(implicit uf: UserFinder[T]): Future[Option[JsObject]] = {
    val query = uf.toQuery(t)
    collection("users").find(query).one
  }
}
