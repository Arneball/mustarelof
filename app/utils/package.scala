import play.api.libs.json._
package object utils {
  implicit class AnyW[T](val t: T) extends AnyVal {
    def toJson(implicit writes: Writes[T]) = writes.writes(t)
  }
  
  object JsObj {
    def apply(vals: (String, JsValue)*) = JsObject(Seq(vals: _*))
  }
  object JsArr {
    def apply(stuff: JsValue*) = JsArray(Seq(stuff: _*))
    def apply[T](stuff: T*)(implicit writes: Writes[T]) = JsArray(Seq(stuff.map{_.toJson}: _*))
  }
}