package utils
import securesocial.core.UserServicePlugin
import securesocial.core.EventListener
import securesocial.core.LogoutEvent
import securesocial.core.PasswordChangeEvent
import securesocial.core.PasswordResetEvent
import securesocial.core.LoginEvent
import securesocial.core.SignUpEvent
import play.api.mvc.RequestHeader
import securesocial.core.Event
import play.api.Logger
import play.api.mvc.Session
class UserService(app: play.api.Application) extends UserServicePlugin(app){
 /** As seen from class UserService, the missing signatures are as follows.  
  *  For convenience, these are usable as stub implementations.  
  */   
  def deleteExpiredTokens(): Unit = ???   
  def deleteToken(uuid: String): Unit = ???   
  def find(id: securesocial.core.IdentityId): Option[securesocial.core.Identity] = ???   
  def findByEmailAndProvider(email: String, providerId: String): Option[securesocial.core.Identity] = ???   
  def findToken(token: String): Option[securesocial.core.providers.Token] = ???   
  def save(token: securesocial.core.providers.Token): Unit = ???   
  def save(user: securesocial.core.Identity): securesocial.core.Identity = ???
}

class MyEventListener(app: play.api.Application) extends EventListener {
  override def id: String = "my_event_listener"

  def onEvent(event: Event, request: RequestHeader, session: Session): Option[Session] = {
    val eventName = event match {
      case e: LoginEvent => "login"
      case e: LogoutEvent => "logout"
      case e: SignUpEvent => "signup"
      case e: PasswordResetEvent => "password reset"
      case e: PasswordChangeEvent => "password change"
    }

    Logger.info("traced %s event for user %s".format(eventName, event.user.fullName))
    // Not changing the session so just return None
    // if you wanted to change the session then you'd do something like
    // Some(session + ("your_key" -> "your_value"))
    None
  }
}

