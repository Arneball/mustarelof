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
      loc1.distanceTo(loc2)
    }
  }
  def decode(possible_gay_adress: String): AddressWithLocation = {
    val req = new GeocoderRequestBuilder().setAddress(possible_gay_adress).getGeocoderRequest
    geocoder.geocode(req).getResults.map{ res => 
      new ConcreteAddress(possible_gay_adress, res.getFormattedAddress, res.getGeometry.getLocation) 
    }.head
  }
  
  def pimped_with_distance(myloc: Seq[AddressWithLocation]): Seq[(AddressWithLocation, Option[Double])] = myloc.length match {
    case 0 => Nil
    case 1 => (myloc.head -> None)::Nil
    case n =>
      myloc.zip(myloc.tail).map{
        case (ena, andra) => ena -> Some(ena.distanceTo(andra)) 
      } :+ myloc.last -> None
  } 
  def travelling_salesmen(myloc: AddressWithLocation, seq: Set[AddressWithLocation]): List[AddressWithLocation] = seq match {
    case empty if empty.isEmpty => Nil
    case something => 
      val min = something.minBy{ myloc.distanceTo }
      min::travelling_salesmen(min, something - min)
  }
}

case class LongLat(long: BigDecimal, lat: BigDecimal){
  def distanceTo(that: LongLat) = {
    import scala.math._
    def toRad(d: BigDecimal) = d * Pi / 180
    val R = 6371
    implicit def b2d(b: BigDecimal): Double = b.toDouble
    val LongLat(long1, lat1) = this
    val LongLat(long2, lat2) = that
    
    val lonDistance: Double = toRad(long2 - long1)
    val latDistance: Double = toRad(lat2 - lat1)
    val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    R * c// (where R is the radius of the Earth) 
  }
}
object LongLat {
  implicit def goog2mine(latlon: GoogLatLng): LongLat = new LongLat(latlon.getLng, latlon.getLat)
  implicit val format = Json.format[LongLat]
}
case class ConcreteAddress(description: String, address_string: String, location: LongLat) extends Address with Location

trait Address {
  def address_string: String
  def description: String
}
trait Location {
  def location: LongLat
  def distanceTo(that: Location) = location.distanceTo(that.location)
}
