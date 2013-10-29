package controllers
import scala.concurrent.ExecutionContext.Implicits.global
import oauth._
import play.api.Play.current
import utils._
import play.api._
import play.api.mvc._
import java.io.ByteArrayInputStream
import utils.OurGeoDecoder
import utils.ExcelParser
import play.api.libs.json.JsArray
import utils._
import java.io.FileInputStream
import scala.io.Source
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import scala.concurrent.{future, Future}
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.Files.TemporaryFile
import java.io.File
import views.html.form
import play.api.libs.json.JsNumber
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsString
import scala.collection.concurrent.TrieMap
import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import controllers2._
//import net.sf.ehcache.Cache
import play.api.libs.json.JsNull
object Application extends PimpedController {
  def index = Action{ r =>
    Ok(views.html.form()).withSignedCookies(Cookie("apa", "svin"))
  }
  def konsult = Action{
    Ok(views.html.konsult())
  }
  def route = Action{
    Ok(views.html.route_manager())
  }
  def time = Action{
    Ok(views.html.time_report())
  }
  object Car {
    implicit val format: Format[Car] = Json.format[Car]
  }
  case class Car(manufacturer: String, cubics: Int, maxSpeed: Int)
  val cars = Action{
    val cars = List(Car("Saab", 1400, 160), Car("Volvo", 2200, 190))
    val jsoncars = cars.map{ _.toJson }
    val responsedata = JsArray(jsoncars)
    Ok(responsedata)
  }
 
  
  /** Handy method to create a AsyncAction pimped with files
   *  Used like 
   *  {{{
   *  def postHandler = AsynchAttachmentAction{ files => request => 
   *    Ok(files.keys.mkString) 
   *  }
   *  }}}
   */
  private def AsyncAttachmentAction(fun: Map[String, File] => Request[MultipartFormData[TemporaryFile]] => SimpleResult) = {
    Action.async(parse.multipartFormData){ requestPimpedWithFiles => 
      scala.concurrent.future{
        val files = requestPimpedWithFiles.body.files.map{ fileref => fileref.key -> fileref.ref.file }.toMap
        println(s"Files $files")
        fun(files)(requestPimpedWithFiles)
      }
    }
  }
  
  /** Used by angular js webservice client */
  val testJson = Action{
    val res = JsArray((1 to 10).map{ int => JsNumber(int) })
    Ok(res)
  }
  
  val post = AsyncAttachmentAction{ files => request =>
    val myaddress: AddressWithLocation = request.body.asFormUrlEncoded("myaddress").map{ OurGeoDecoder.decode }.head
    Logger.debug(s"Files ${request.body.file("file")}")
    
    val orders = ExcelParser.parse(new FileInputStream(files("file")))
    val travelorder = OurGeoDecoder.travelling_salesmen(myaddress, orders.toSet)
    
    type PimpedType = (AddressWithLocation, Option[Double])
    val pimped: Seq[PimpedType] = OurGeoDecoder.pimped_with_distance(travelorder)
    
    /** A Json serializer for Tuple2[AddressWithLocation, Option[Double]] */
    implicit object W extends Writes[PimpedType]{
      def advrites(a: AddressWithLocation) = implicitly[Writes[AddressWithLocation]].writes(a).asInstanceOf[JsObject]
      def writes(t: (AddressWithLocation, Option[Double])) = t match {
        case (a, Some(b)) => advrites(a) + ("distanceToNext" -> b.toJson)
        case (a, _)       => advrites(a)
      }
    }
    
    Ok{
      JsObj(
        "rådata excel" -> (orders: JsArray), // implicitly converts Seq[_<%JsValue] => JsArray
        "min location" -> myaddress.toJson, 
        "travelorder" -> (travelorder: JsArray), // implicitly converts Seq[_<%JsValue] => JsArray
        "pimped with distance" -> pimped.toJson)
    }
  }
  
  def fblogin(email: String) = Action.async{ implicit r =>
    asyncOauthRegistrer[FbUser](Some(email))
  }
  
  def dummy(email: String) = Action.async {
    val res = for {
      lasterror <- MongoAdapter.addDummy(email)
    } yield Ok(JsObj(
        "success" -> lasterror.ok, 
        "errtext" -> lasterror.errMsg.map{ new JsString(_)}.getOrElse(JsNull)
      ))
    res.recover{
      case e: Exception => Ok(JsObj("success" -> false, "errtext" -> e.getMessage))
    }
  }
  
  /** Generic oauth registrating handler
   *  @param email. If None then login, if Some(email) then map user to provider data
   */  
  def asyncOauthRegistrer[T : UserFinder : Writes](emailOpt: Option[String])(implicit decoder: Decoder[T], r: Request[AnyContent]): Future[SimpleResult] = {
  
    /** Will check in db for user 
     * @param code is the code given the oauth callback
     */
    def tryFetchUserData(code: String) = for {
      user_dataOpt <- decoder.getUserData(None, code)
      user_data <- user_dataOpt.future(RestException("No user data found"))
      dbResultOpt <- MongoAdapter.getUser(user_data)
      dbResult <- dbResultOpt.future(RestException("No Database hit"))
    } yield Ok("Oauth login success").withSignedCookies(Decoder.toCookie(user_data))
    
    /** Tries to fill user with provider specific data
     *  @param res used to show success page
     */
    def tryInitUserData(code: String) = for {
      cookie <- decoder.initUserData(emailOpt.get, code) // .get must be safe since we pattern matched before
    } yield Ok("Oauth data initialized").withSignedCookies(cookie)
    
    val queryString = r.queryString
    val futureRes = (queryString.get("code"), emailOpt) match {
      // this is login, no email <-> provider data mapping
      case (Some(Seq(code)), None | Some("nostate")) =>  tryFetchUserData(code)
      // this is email <-> provider user data mapping
      case (Some(Seq(code)), _) => tryInitUserData(code)
      // default case
      case _ => throw new RestException("Stop the world")
    }
    futureRes.recover{
      case RestException(message) => InternalServerError(message)
    }
  }
  
  private def getState(implicit r: Request[AnyContent]) = r.queryString.get("state").flatMap{ _.headOption} 
  
  /** Linkedin login handler, uses the generic asyncOauthRegistrer */
  val linkedinlogin = Action.async{ implicit r => 
    asyncOauthRegistrer[LinkedinUser](getState)
  }
  
  /** Gmail login handler, uses the generic asyncOauthRegistrer */
  val gmaillogin = Action.async{ implicit r =>
    asyncOauthRegistrer[GoogleUser](getState)
  }
  
  /** Check if the user email is asociated with any provider specific data */
  def hasData(email: String, provider: String) = Action.async{ r =>
    def has[T : UserFinder] = MongoAdapter.emailHas(email)
    val futBool = provider match { 
      case "fb" => has[FbUser]
      case "google" => has[GoogleUser]
      case "linkedin" => has[LinkedinUser]
    }
    futBool.map{ res => 
      Ok(JsObj("provider" -> provider, "user_has" -> res))
    }
  }
  
  object CookE {
    def unapply(c: Cookie) = if(c.hasValidSign) Some(c.name -> c.value.unsign) else None
  }
  
  def funnyCookies(r: Request[AnyContent]): Future[Option[JsObject]] = r.cookies.filter{ _.hasValidSign }.headOption match {
    case Some(CookE("google", id)) => MongoAdapter.getUser(GoogleUser.withId(id))
    case Some(CookE("fb", id)) => MongoAdapter.getUser(FbUser.withId(id))
    case Some(CookE("linkedin", id)) => MongoAdapter.getUser(LinkedinUser.withId(id))
    case _ => future { None }
  }
  
  val logout = Action{ 
    def dc(name: String) = DiscardingCookie(name=name)
    Redirect("/konsult#/login").discardingCookies(dc("fb"), dc("google"))
  }
  
  def SecureActionWithUser(fun: Request[AnyContent] => JsObject => Future[SimpleResult]) = Action.async{ req =>
    funnyCookies(req).flatMap{
      case Some(user) => fun(req)(user)
      case _ => future{ Redirect("/konsult#/login") }
    }
  }
  
  def SecureAction(fun: Request[AnyContent] => Future[SimpleResult]) = Action.async { r =>
    funnyCookies(r).flatMap{
      case None => future{ Redirect("/konsult#/login") }
      case _ => fun(r)
    }
  }
  
  val protectedContent = SecureActionWithUser{ r => user =>
    future {
      Ok(JsObj("our user is " -> user))
    }
  }
}