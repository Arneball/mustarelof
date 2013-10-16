package utils
import java.io.InputStream
import play.api.libs.json.Json
import scala.util.Try
import play.api.Logger
import scala.collection.JavaConversions._
import java.io.{FileInputStream, File}
import org.apache.poi.ss.usermodel.{WorkbookFactory, Cell, Row}
import org.apache.poi.ss.usermodel.Sheet
case class Order(name: String, address_string: String, location: LongLat, raw: String) extends Address with Location{
  def description = name
}

object Order {
  implicit val format = Json.format[Order]
  Json.format[Order]
}

object ParserHelpers {
  implicit class RowWrapper(val row: Row) extends AnyVal {
    def cell(index: Int): Option[Cell] = Option(row.getCell(index))
  }
    /** Pimps Sheet with methods */
  implicit class SheetWrapper(val sheet: Sheet) extends AnyVal {
    /** Translates a sheet to an traversable of rows */
    def rows: Seq[Row] = (sheet.getFirstRowNum to sheet.getLastRowNum).map{ sheet.getRow }
  }
  implicit class CellWrapper(val cell: Cell) extends AnyVal {
    def stringValue: Option[String] = Try{
      cell.getStringCellValue // this might throw exception if no string is parcelable
    }.toOption
  }
}

object ExcelParser {
  import ParserHelpers._
  def parse(stream: InputStream): Seq[Order] = {
    val sheet = WorkbookFactory.create(stream).getSheetAt(0)
    val headerRow = sheet.rows.head 
    val headersWithIndex = headerRow.zipWithIndex.map{ case (cell, index) => cell.getStringCellValue -> index}
    var adressI = 0
    var nameI = 0
    headersWithIndex.foreach{
      case ("adress", i) => adressI = i
      case ("name", i) => nameI = i
      case _ => 
    }
    val parser = new RowParser(adressIndex=adressI, nameIndex=nameI)
    val parsedOrders = sheet.rows.drop(1).flatMap{ parser.unapply } // drop 1 since first row is for labels
    Logger.debug(s"$parsedOrders")
    parsedOrders
  }
}


class RowParser(adressIndex: Int, nameIndex: Int){
  def parse(row: Row) = {
    val raw = row.getCell(adressIndex).getStringCellValue
    val address = OurGeoDecoder.decode(raw)
    val name = row.getCell(nameIndex).getStringCellValue
    Order(name=name, address_string=address.address_string, location=address.location, raw=raw)
  }
  
  import ParserHelpers._
  /** Safely parses the rows for data, returning None if failure */
  def unapply(row: Row): Option[Order] = for {
    rawcell <- row.cell(adressIndex)
    rawaddress <- rawcell.stringValue
    namecell <- row.cell(nameIndex)
    name <- namecell.stringValue
    parsedaddress = OurGeoDecoder.decode(rawaddress)
  } yield Order(name=name, address_string=parsedaddress.address_string, location=parsedaddress.location, raw=rawaddress)
}

