import play.api.libs.json._
package object utils {
  type FutureList = scala.concurrent.Future[List[JsValue]]
  
  implicit object ListWrites extends Writes[List[JsValue]]{
    def writes(value: List[JsValue]) = JsArray(value.map{ _.toJson })
  }
  
  implicit def jsseqshit[T](s: Seq[T])(implicit writes: Writes[T]) = {
    val items = s.map{ _.toJson }
    JsArray(items)
  }
  
  
  /** implicits that make scala values become JsValues */
  implicit def str2JsString(str: String): JsString = new JsString(str)
  implicit def int2JsInt(i: Int): JsNumber = new JsNumber(i)
  implicit def bool2JsBool(b: Boolean): JsBoolean = new JsBoolean(b)
  object JsObj {
    def apply(vals: (String, JsValue)*) = JsObject(vals.toSeq)
  }
  object JsArr {
    def apply(stuff: JsValue*) = JsArray(stuff.toSeq)
    def apply[T](stuff: T*)(implicit writes: Writes[T]) = JsArray(stuff.map{_.toJson}.toSeq)
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
}