package utils
import java.io.InputStream
import play.api.libs.json.Json
import scala.util.Try
import play.api.Logger
import scala.collection.JavaConversions._
import java.io.FileInputStream
import java.io.File
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
case class Order(name: String, address: Address, raw: String) extends WithAddress
object Order {
  implicit val format = Json.format[Order]
}

object ExcelParser {
  def parse(stream: InputStream) = {
    
    val sheet = WorkbookFactory.create(stream).getSheetAt(0)
    val headerRow = sheet.getRow(0)
    val headersWithIndex = headerRow.zipWithIndex.map{ case (cell, index) => cell.getStringCellValue -> index}
    var adressI = 0
    var nameI = 0
    headersWithIndex.foreach{
      case ("adress", i) => adressI = i
      case ("name", i) => nameI = i
      case _ => 
    }
    val parser = new RowParser(adressI, nameI)
    val parsedRows = 1 to sheet.getLastRowNum map { rowIndex =>
      parser.parse(sheet.getRow(rowIndex))
    }
    Logger.debug(s"$parsedRows")
    parsedRows
  }
  private implicit def row2iterable(r: Row): List[Cell] = r.cellIterator.toList
  def main(args: Array[String]): Unit = {
    println{
      JsArr{
        parse(new FileInputStream(new File("/Users/raulbache/Downloads/namnlost.xlsx"))): _*
      }
    }
  }
}
class RowParser(adressIndex: Int, nameIndex: Int){
  def parse(row: Row) = {
    val raw = row.getCell(adressIndex).getStringCellValue
    val address = OurGeoDecoder.decode(raw)
    val name = row.getCell(nameIndex).getStringCellValue
    Order(name, address, raw)
  }
}

