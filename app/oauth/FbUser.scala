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

case class FbUser(first_name: String, last_name: String, facebook_id: String)
object FbUser {
  implicit object reads extends EasyReads[FbUser] {
    def reads(obj : Option[JsObject]) = for {
      jsobj <- obj
      JsString(fname) <- jsobj \/ "first_name"
      JsString(lname) <- jsobj \/ "last_name"
      JsString(id) <- jsobj \/ "id"
    } yield FbUser(fname, lname, id)
  }
  implicit val writes = Json.writes[FbUser]
}

trait EasyReads[T] extends Reads[T] {
  def reads(obj: JsValue): JsResult[T] = reads(obj.asOpt[JsObject]).map{ res => new JsSuccess(res) }.getOrElse(new JsError(Nil))
  def reads(obj: Option[JsObject]): Option[T]
}