package oauth

import play.api.libs.json.Json

object LinkedinUser {
  implicit val format = Json.format[LinkedinUser]
  def withId(id: String) = LinkedinUser(id)
}
case class LinkedinUser(
  linkedin_id: String, 
  linkedin_first_name: Option[String]=None, 
  linkedin_last_name: Option[String]=None)