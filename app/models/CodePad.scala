package models

import akka.actor.{Props, Actor}
import play.api.libs.concurrent._
import akka.pattern.ask
import play.api.libs.json.{JsString, JsObject, JsValue}
import play.api.libs.iteratee._
import play.api.Play.current
import akka.util.Timeout
import akka.util.duration._

class CodePad extends Actor {

  var code: String = ""
  var coders = Map.empty[String, PushEnumerator[JsValue]]

  protected def receive = {

    // Join code pad
    case Join(coder) =>
      val channel = Enumerator.imperative[JsValue]()
      coders = coders + (coder -> channel)

      sender ! Connected(channel)

    // Edit code
    case Edit(coder, newCode) =>
      code = newCode
      publishUpdate(newCode)

    // Append code
    case Append(coder, newCode) =>
      code += newCode
      publishUpdate(coder)

  }

  def publishUpdate(code: String) {
    val message = JsObject(
      Seq(
        "code" -> JsString(code)
      )
    )
    coders.foreach {
      case (_, coder) => coder.push(message)

    }
  }
}

object CodePad {

  lazy val actor = Akka.system.actorOf(Props[CodePad])
  implicit val timeout = Timeout(1 second)


  /**
   * Join to default CodePad
   */
  def join(username: String) = {

    (actor ? Join(username)).asPromise.map {

      case Connected(enumerator) =>
        val iteratee = createIteratee(username)

        (iteratee, enumerator)
    }
  }

  private def createIteratee(username: String): Iteratee[JsValue, Unit] = {

    Iteratee.foreach[JsValue] {
      event =>
        actor ! messageForEvent(event, username)
    }.mapDone {
      _ =>
        actor ! Quit(username)
    }

  }

  private def messageForEvent(event: JsValue, username: String): Message = {
    // Extract parameters
    val command = (event \ "command").as[String]
    val code = (event \ "code").as[String]

    // Create akka message
    val message = command match {
      case "append" =>
        Append(username, code)
    }

    message
  }
}
