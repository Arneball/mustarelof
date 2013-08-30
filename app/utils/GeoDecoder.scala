package utils

import com.google.code.geocoder.Geocoder
import com.google.code.geocoder.model.GeocoderRequest
import com.google.code.geocoder.GeocoderRequestBuilder
import scala.collection.JavaConversions._
import com.google.code.geocoder.model.{LatLng => GoogLatLng}
import play.api.libs.json._
import com.google.code.geocoder.model.LatLng
object OurGeoDecoder {
  private val geocoder = new Geocoder
  def main(args: Array[String]): Unit = {
    println{
      val loc1 = decode("Tingsvägen 6 Skene").location
      print(loc1)
      val loc2 = decode("Hebergsvägen 110 Vallda").location
      print(" " + loc2)
      distance(loc1, loc2)
    }
  }
  def decode(possible_gay_adress: String): Address = {
    val req = new GeocoderRequestBuilder().setAddress(possible_gay_adress).getGeocoderRequest
    geocoder.geocode(req).getResults.map{ res => new Address(res.getFormattedAddress, res.getGeometry.getLocation) }.head
  }
  def distance(a1: WithAddress, a2: WithAddress): Double = distance(a1.address.location, a2.address.location)
  def distance(l1: LongLat, l2: LongLat) = {
    import scala.math._
    def toRad(d: BigDecimal) = d * Pi / 180
    val R = 6371
    implicit def b2d(b: BigDecimal): Double = b.toDouble
    val LongLat(long1, lat1) = l1
    val LongLat(long2, lat2) = l2
    
    val lonDistance: Double = toRad(long2 - long1)
    val latDistance: Double = toRad(lat2 - lat1)
    val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    R * c// (where R is the radius of the Earth) 
  } 
  def travelling_salesmen(myloc: WithAddress, seq: Set[WithAddress]): List[WithAddress] = seq match {
    case empty if empty.isEmpty => Nil
    case something => 
      val min = something.minBy{ distance(myloc, _)}
      min::travelling_salesmen(min, something - min)
  }
}

case class LongLat(long: BigDecimal, lat: BigDecimal)
object LongLat {
  implicit def goog2mine(latlon: GoogLatLng): LongLat = new LongLat(latlon.getLng, latlon.getLat)
  implicit val format = Json.format[LongLat]
}
case class Address(address_str: String, location: LongLat) extends WithAddress{
  def address = this
}
object Address {
  implicit val format = Json.format[Address]
}
trait WithAddress {
  def address: Address
}
object WithAddress {
  implicit val format = new Writes[WithAddress]{
    def writes(wa: WithAddress) = wa.address.toJson
  }
}

