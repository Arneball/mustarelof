package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import utils._
import controllers2._
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.core.commands.LastError
import play.api.libs.json.JsObject
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString
import play.api.Logger
import play.api.libs.json.JsArray

object ReportsController extends PimpedController {
  def getReports(user_id: String) = Action{
    val res = for {
      extracted_future: List[JsValue] <- MongoAdapter.reports(user_id)
    } yield Ok(extracted_future: JsValue)
    Async {
      res
    }
  }
  
  def postReport(user_id: String) = JsAction{ report => request =>
    // dbresult is an Future[Result], might also be empty
    val dbresult = for {
      postres: LastError <- MongoAdapter.addReport(user_id, report)
      if postres.ok
    } yield Ok(JsObj("success"->true))
    
    Async {
      dbresult.recover(genericErrorHandler)
    }
  }
  
  def putReport(user_id: String, report_id: String) = JsAction{ report => request =>
    val futureResult = for {
      lasterror <- MongoAdapter.updateReport(user_id, report_id, report)
      if lasterror.ok
    } yield Ok(JsObj("success"->true))
    
    Async {
      futureResult.recover(genericErrorHandler)
    }
  }
  
  def pdf(user_id: String, report_id: String) = Action{
    val futureReport = for {
      Some(report) <- MongoAdapter.report(user_id, report_id)
      _ = Logger.debug(s"Rapport: $report")
      lines = for {
        JsArray(lines) <- (report \/ "lines").toList
        JsObj(line) <- lines
        JsNumber(hours) <- line \/ "hours"
        JsString(customer) <- line \/ "customer"
      } yield Line(hours=hours.toInt, customer=customer, price=540)
    } yield new Report(consultant=user_id, logo_url="http://f.food-supply.se/21o80k0cnjor68sb.jpg", lines=lines)
    Async {
      futureReport.map{ report =>
        val bytes = PdfCreator.createPdf(report)
        Ok(bytes).withHeaders("Content-Type"->"application/pdf")
      }.recover(genericErrorHandler)
    }
  }
}