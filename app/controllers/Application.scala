package controllers

import play.api._
import libs.json.JsValue
import play.api.data.Forms._
import data.Form
import play.api.mvc._
import models.CodePad

object Application extends Controller {
  val loginForm = Form( "username" -> nonEmptyText)

  def index = Action {
    implicit request =>
    Ok(views.html.index())
  }

  def signIn = Action { implicit request =>
    loginForm.bindFromRequest().fold (
      formWithErrors => Redirect(routes.Application.index()).flashing("error" -> "Enter your name"),
      user => Redirect( routes.Application.editor(user))
    )
  }

  def editor(username: String) = Action {
    request => Ok(views.html.editor(username))
  }

  def join(coder: String) = WebSocket.async[JsValue] { request  =>
    CodePad.join(coder)
  }
  
}