package oauth

import play.api.libs.json.Json

object GoogleUser {
  implicit val format = Json.format[GoogleUser]
}
case class GoogleUser(google_id: String)