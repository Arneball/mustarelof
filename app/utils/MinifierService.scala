package utils

import com.yahoo.platform.yui.compressor.YUICompressor
import com.yahoo.platform.yui.compressor.JavaScriptCompressor
import scala.io.Source
import org.mozilla.javascript.ErrorReporter
import java.io.StringReader
import java.io.StringWriter

object MinifierService {
  def minify(urls: String*) = {
    val wholeString = urls.par.map{ url => Source.fromURL(url).getLines.mkString("\n") }.mkString("\n")
    val err = new ErrorReporter {
      def error(x1: String,x2: String,x3: Int,x4: String,x5: Int): Unit = println(s"$x1, $x2, $x3, $x4, $x5") 
      def runtimeError(x$1: String,x$2: String,x$3: Int,x$4: String,x$5: Int): org.mozilla.javascript.EvaluatorException = ??? 
      def warning(x1: String,x2: String,x3: Int,x4: String,x5: Int): Unit = println(s"$x1, $x2, $x3, $x4, $x5")
    }
    val compressor = new JavaScriptCompressor(new StringReader(wholeString), err)
    val strW = new StringWriter
    compressor.compress(strW, -1, true, true, true, false)
    strW.toString
  }
  
  def main(args: Array[String]): Unit = {
    minify("http://code.angularjs.org/1.0.8/angular-resource.js", "http://cdn.jsdelivr.net/restangular/latest/restangular.js")
  }
}