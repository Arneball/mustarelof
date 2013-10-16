package utils
import java.io.FileOutputStream
import java.util.Date
import com.itextpdf.text.{ List => _, _}
import com.itextpdf.text.pdf._
import java.net.URL
import com.itextpdf.text.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import play.api.Logger
import controllers._


object PdfCreator extends PdfExtras {
  def createPdf(implicit report: Report) = {
    
    val doc = new Document(PageSize.A4, 36, 36, 54, 54)
    val output = new ByteArrayOutputStream()
    val writer = PdfWriter.getInstance(doc, output)
    writer.setPageEvent(new Footer)
    doc.open()
    doc.add(createLogoTable(report))
    doc.add(createTable(report))
    doc.add(createTotal(report))
    doc.close()
    output.toByteArray
  }
  
  def main(args: Array[String]): Unit = {
    createPdf(Report("Arne", "http://f.food-supply.se/21o80k0cnjor68sb.jpg", List(Line(3, 540, "Ericsson CPG"), Line(40, 540, "Ericsson GGSN"))))
  }
}
class Footer extends PdfPageEventHelper {
  override def onEndPage(writer: PdfWriter, document: Document) = {
    Logger.debug(s"In here")
    val table = new PdfPTable(3)
    table.setWidths(Array(24, 24, 2))
    table.setTotalWidth(527);
    table.setLockedWidth(true);
    table.getDefaultCell().setFixedHeight(20);
    table.getDefaultCell().setBorder(Rectangle.BOTTOM);
    table.addCell("Batik");
    table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell("Page %d of".format(writer.getPageNumber()));
    table.addCell("")
    table.writeSelectedRows(0, -1, 34, 803, writer.getDirectContent);
  }
}

trait PdfExtras {
  def createTable(report: Report) = {
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
  def createTotal(report: Report) = {
    val paragraph = new Paragraph
    paragraph.setAlignment(Element.ALIGN_RIGHT)
    val total = report.lines.foldLeft(0.0){ _ + _.totalprice }
    paragraph.add(s"Grand total: ${ total } sek")
    paragraph
  }
  def createLogoTable(report: Report) = {
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
  
  implicit class StampWrapper(val stamper: PdfStamper) {
    def maxWidth = (1 to pageCount).map{ page => reader.getPageSize(page).getWidth }.max
    def reader = stamper.getReader
    def pageCount = reader.getNumberOfPages
  }
  
  def writeTable(stamper: PdfStamper, table: PdfPTable) = {
    try {
      val pageWidth = stamper.maxWidth
      val List(left, tablewidth) = List(.1f, .8f).map{ pageWidth * _ }
      println(s"Left $left, tableVidth: $tablewidth, pageWidth: $pageWidth")
      table.setTotalWidth(tablewidth)
      table.writeSelectedRows(0, -1, left, 600, stamper.getOverContent(1))
    }catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}