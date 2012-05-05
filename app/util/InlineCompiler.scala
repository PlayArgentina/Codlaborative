package util

object InlineCompiler {

  def result(code: String): String = {
    import com.twitter.util._
    val eval = new Eval

    try {
      eval(code).toString

    } catch {
      case e: Exception => e.toString
    }
  }
}
