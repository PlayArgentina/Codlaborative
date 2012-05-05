package models

import play.api.libs.concurrent._
import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.Play.current
import akka.util.Timeout
import akka.util.duration._
import play.api.libs.json.{JsArray, JsString, JsObject, JsValue}
import akka.actor.{ActorRef, Props, Actor}

class CodePad extends Actor {

  var code: String = ""
  var output: String = ""

  var coders = Map.empty[String, PushEnumerator[JsValue]]
  var compiler : ActorRef = null

  override def preStart() {
    compiler = context.actorOf(Props[Compiler])
  }

  protected def receive = {

    // Join code pad
    case Join(coder) =>
      println("Join(" + coder + ")")
      val channel = Enumerator.imperative[JsValue](onStart = self ! NewCoder(coder))
      coders = coders + (coder -> channel)

      sender ! Connected(channel)

    case NewCoder(coder) =>
      publishUpdate(code, output, coder)

    // Edit code
    case Edit(coder, newCode) =>
      println("Edit(" + coder + "," + newCode + ")")

      code = newCode
      publishUpdate(code, output, coder)

      compiler ! Compile(code)

    // Append code
    case Append(coder, newCode) =>
      println("Append(" + coder + "," + newCode + ")")
      code += newCode
      publishUpdate(code, output, coder)

      compiler ! Compile(code)

    case Quit(coder) =>
      coders = coders - coder
      publishUpdate(code, output, coder)

    case CompileResult(out) =>
      output = out
      publishUpdate(code, output, "compiler")
  }

  def publishUpdate(code: String, output: String, author: String) {
    val message = JsObject(
      Seq(
        "code" -> JsString(code),
        "output" -> JsString(output),
        "author" -> JsString(author),
        "coders" -> JsArray(
          coders.keySet.toList.map(JsString)
        )
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
    println("messageForEvent(" + event + "," + username + ")")
    val command = (event \ "command").as[String]
    val code = (event \ "code").as[String]

    // Create akka message
    val message = command match {
      case "append" =>
        Append(username, code)
      case "edit" =>
        Edit(username, code)
    }

    message
  }
}

class Compiler extends Actor {
  protected def receive = {
    case Compile(code) => {
      println("compiling ..." + code)
      sender ! CompileResult(util.InlineCompiler.result(code))
    }
  }
}