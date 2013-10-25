package oauth

import play.api.libs.json._
import utils._

object GoogleUser {
  implicit val reads = new EasyReads[GoogleUser]{
    def reads(js: Option[JsObject]): Option[GoogleUser] = for {
      obj <- js
      JsString(id) <- obj \/ "id"
      JsString(pic) <- obj \/ "picture"
      JsString(gender) <- obj \/ "gender"
    } yield GoogleUser(google_id=id, google_gender=gender, google_picture=pic)
  }
  implicit val writes = Json.writes[GoogleUser]
  
  def withId(id: String) = GoogleUser(id, null, null)
}
case class GoogleUser(google_id: String, google_gender: String, google_picture: String)