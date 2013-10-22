import play.api.libs.json._
import scala.concurrent.Future
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Base64
import play.Configuration
import scala.util.Try
package object utils {
  type FutureList = scala.concurrent.Future[List[JsValue]]
  
  implicit object ListWrites extends Writes[List[JsValue]]{
    def writes(value: List[JsValue]) = JsArray(value.map{ _.toJson })
  }
  
  implicit def jsseqshit[T](s: Seq[T])(implicit writes: Writes[T]) = {
    val items = s.map{ _.toJson.replace_id }
    JsArray(items)
  }
  
  implicit class JsValueWrapper(val v: JsValue) extends AnyVal {
    def replace_id: JsValue = v match {
      case o: JsObject => o.replace_id
      case e => e
    }
    def \/(label: String) = (v \ label).asOpt[JsValue] 
  }
  
  implicit def jsArray2Traversable(js: JsArray): Traversable[JsValue] = js.value
  
  implicit class JsObjectWrapper(val o: JsObject) extends AnyVal {
    def replace_id: JsObject = (for {
      JsOid(idstring) <- o \/ "_id"
    } yield o - "_id" + ("id" -> idstring)).getOrElse(o)
    
    def \/(label: String) = (o \ label).asOpt[JsValue]
  }
  
  object JsOid {
    def unapply(o: JsObject) = for {
      JsString(le_val) <- o \/ "$oid"
    } yield le_val
  }
  
  /** implicits that make scala values become JsValues */
  implicit def str2JsString(str: String): JsString = new JsString(str)
  implicit def int2JsInt(i: Int): JsNumber = new JsNumber(i)
  implicit def bool2JsBool(b: Boolean): JsBoolean = new JsBoolean(b)
  object JsObj {
    def apply(vals: (String, JsValue)*) = JsObject(vals.toSeq)
    def unapply(jsobject: JsObject) = Some(jsobject)
  }
  object JsArr {
    def apply(stuff: JsValue*) = JsArray(stuff.toSeq)
  }
  
  implicit class AnyW[T](val t: T) extends AnyVal {
    def toJson(implicit writes: Writes[T]) = writes.writes(t)
  }
  type AddressWithLocation = Address with Location
  implicit object AddrLocWriter extends Writes[AddressWithLocation]{
    def writes(t: AddressWithLocation) = JsObj(
      "address string" -> t.address_string, 
      "location" -> t.location.toJson,
      "description" -> t.description
    )
  }
  
  def getId(id: String) = JsObj("$oid" -> id)
  
  implicit class OptFut[T](val o: Option[T]) extends AnyVal {
    def future(e: => Exception): Future[T] = o.map{ Future.successful }.getOrElse(Future.failed(e))
    def future: Future[T] = future(new Exception)
  }
  
  implicit class PipeOp[T](val t: T) extends AnyVal {
    def |>[U](f: T => U) = f(t)
  }
  
  lazy val secretKey = play.api.Play.current.configuration.getString("application.secret").get
  
  implicit class StringWrapper(val str: String) extends AnyVal {
    def parse: Option[JsValue] = Try {
      Json.parse(str)
    }.toOption
    
    /** Parse a string to Option[T] */
    def fromJson[T](implicit reads: Reads[T]): Option[T] = {
      reads.reads(Json.parse(str)).asOpt
    }
    /** Returns a SHA1 signature of the string */
    def sign = {
      val signingKey = new SecretKeySpec(secretKey.getBytes, "HmacSHA1");
      val mac = Mac.getInstance("HmacSHA1");
      mac.init(signingKey);
      val rawHmac = mac.doFinal(str.getBytes)
      Base64.encodeBase64String(rawHmac);    
    }
  }
}