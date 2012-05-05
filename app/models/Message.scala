package models

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue

trait Message

trait Command extends Message {
  def username: String
}

case class Join(coder: String) extends Message

case class NewCoder(coder: String) extends Message

case class Quit(coder: String) extends Message

case class Connected(enumerator: Enumerator[JsValue])

case class Append(username: String, code: String) extends Command

case class Edit(username: String, code: String) extends Command

