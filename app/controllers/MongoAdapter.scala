package controllers

import reactivemongo.api.MongoDriver
import reactivemongo.api.MongoConnection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsObject
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import scala.concurrent.Future
import utils._
import play.api.libs.json.JsValue
import reactivemongo.bson.BSONDocument
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
}