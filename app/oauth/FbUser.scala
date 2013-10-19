package oauth

import play.api.libs.json.Json
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import utils._
import play.api.libs.json.JsString
import play.api.libs.json.Reads
import play.api.libs.json.Writes

case class FbUser(first_name: Option[String], last_name: Option[String], facebook_id: String)
object FbUser {
  def withId(id: String) = new FbUser(first_name=None, last_name=None, facebook_id=id)
  
  /** Read from Json */
  implicit object reads extends EasyReads[FbUser] {
    def reads(obj : Option[JsObject]) = for {
      jsobj <- obj
      fname = (jsobj \ "first_name").asOpt[String]
      lname = (jsobj \ "last_name").asOpt[String]
      JsString(id) <- jsobj \/ "id"
    } yield FbUser(fname, lname, id)
  }
  
  /** Write to json */ 
  implicit val writes = Json.writes[FbUser]
}

trait EasyReads[T] extends Reads[T] {
  def reads(obj: JsValue): JsResult[T] = reads(obj.asOpt[JsObject]).map{ res => new JsSuccess(res) }.getOrElse(new JsError(Nil))
  def reads(obj: Option[JsObject]): Option[T]
}