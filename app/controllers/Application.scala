package controllers

import play.api._
import libs.json.JsValue
import play.api.mvc._
import models.CodePad

object Application extends Controller {
  
  def index = Action {
    Ok("")
  }

  def join(coder: String) = WebSocket.async[JsValue] { request  =>
    CodePad.join(coder)
  }
  
}