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
import controllers.Report
import scala.io.Source
import java.io.FileInputStream
import java.io.File

object PdfCreator2 extends PdfExtras {
  def createPdf(report: Report) = {
    val reader = new PdfReader(new FileInputStream(new File("resources/pdfform.pdf")))
    val output = new ByteArrayOutputStream
    val stamper = new PdfStamper(reader, output)
    
    import scala.collection.JavaConversions._
    val fields = stamper.getAcroFields
    fields.setField("konsult", report.consultant)
    val content = stamper.getOverContent(1)
    println("Here1")
    writeTable(stamper, createTable(report))
    println("Here2")
    stamper.close
    reader.close
    println("Here3")
    output.toByteArray
  }
}