package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import utils._
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.core.commands.LastError
import play.api.libs.json.JsObject
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result

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
    // dbresult is an Future[LastError]
    val dbresult = for {
      postres: LastError <- MongoAdapter.addReport(user_id, report)
    } yield postres
    
    Async {
      // mapResult is a Future[Result]
      val mapResult = dbresult.map{ dbres =>
        if(dbres.ok) Ok(JsObj("success"->true))
        else InternalServerError("failure")
      }
      mapResult
    }
  }
  
  def putReport(user_id: String, report_id: String) = JsAction{ report => request =>
    val res = for {
      lasterror <- MongoAdapter.updateReport(user_id, report_id, report)
    } yield lasterror
    
    Async {
      res.map{ r => 
        if(r.ok) Ok(JsObj("success"->true))
        else InternalServerError(JsObj("failure"->true))
      }
    }
  }
}