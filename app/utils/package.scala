import play.api.libs.json._
package object utils {
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
  
  implicit def jsseqshit[T](s: Seq[T])(implicit writes: Writes[T]) = JsArr(s.map{_.toJson})
  
  implicit def str2json(str: String): JsString = JsString(str)
  object JsObj {
    def apply(vals: (String, JsValue)*) = JsObject(Seq(vals: _*))
  }
  object JsArr {
    def apply(stuff: JsValue*) = JsArray(Seq(stuff: _*))
    def apply[T](stuff: T*)(implicit writes: Writes[T]) = JsArray(Seq(stuff.map{_.toJson}: _*))
  }
}