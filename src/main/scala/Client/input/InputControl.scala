package nl.rug.aoop
package Client.input

import org.slf4j.LoggerFactory

/**
 * InputControl keeps track of the current input, and allows for conversion into the format that the server knows
 *
 * It's a Singleton and thus only an object
 */
object InputControl {
  private val logger = LoggerFactory.getLogger("InputControl")

  private var W = "0"
  private var A = "0"
  private var D = "0"

  private var space = "0"

  /**
   * Set a new key
   *
   * @param keyCode The KeyCode of the Key
   * @param pressed Whether it's pressed or not
   */
  def newInput(keyCode: Int, pressed: Boolean): Unit = {
    val state = if (pressed) "1" else "0"

    keyCode match {
      case 32 => space = state
      case 65 => A = state
      case 68 => D = state
      case 87 => W = state
      case _ =>
    }
  }

  override def toString: String = W + A + D + space
}
