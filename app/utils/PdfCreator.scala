package utils
import java.io.FileOutputStream
import java.util.Date
import com.itextpdf.text.Anchor
import com.itextpdf.text.BadElementException
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chapter
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.ListItem
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Section
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.net.URL
import com.itextpdf.text.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object PdfCreator {
  def createPdf(report: Report) = {
    def createTable = {
      val table = new PdfPTable(4)
      
      List("Customer", "Hours", "á price", "total").foreach{ label =>
        val cell = new PdfPCell(new Phrase(label))
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell)
      }
  
      report.lines.foreach{ case Line(hours, price, customer) =>
        table.addCell(customer)
        table.addCell(hours.toString)
        table.addCell(price.toString)
        table.addCell(s"${hours * price} sek")
      }
      table
    }
    def createTotal = {
      val paragraph = new Paragraph
      paragraph.setAlignment(Element.ALIGN_RIGHT)
      val total = report.lines.foldLeft(0.0){ _ + _.totalprice }
      paragraph.add(s"Grand total: ${ total } sek")
      paragraph
    }
    def createLogoTable = {
      val table = new PdfPTable(1)
      val img = Image.getInstance(new URL(report.logo_url))
      img.scaleToFit(100, 100)
//      img.setAlignment(Element.ALIGN_RIGHT)
      val textCell = new Paragraph(report.consultant)
      textCell.setAlignment(Element.ALIGN_RIGHT)
      table.addCell(textCell)
      table.addCell(img)
      table
    }
    
    val doc = new Document
    val output = new ByteArrayOutputStream()
    PdfWriter.getInstance(doc, output)
    doc.open()
    doc.add(createLogoTable)
    doc.add(createTable)
    doc.add(createTotal)
    doc.close()
    output.toByteArray
  }
  
  def main(args: Array[String]): Unit = {
    createPdf(Report("Arne", "http://f.food-supply.se/21o80k0cnjor68sb.jpg", List(Line(3, 540, "Ericsson CPG"), Line(40, 540, "Ericsson GGSN"))))
  }
}

case class Line(hours: Double, price: Int, customer: String) {
  def totalprice = hours * price
}
case class Report(consultant: String, logo_url: String, lines: List[Line])