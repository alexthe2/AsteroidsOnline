package nl.rug.aoop
package Client

import akka.actor.typed.ActorSystem
import nl.rug.aoop.Client.controllers.frame.GameManager
import nl.rug.aoop.Client.graphics.Loader

/**
 * The Client
 */
object Client {

  def main(args: Array[String]): Unit = {
    Loader.loadIntoARE()

    val berryMain = ActorSystem[GameManager.Command](GameManager(), "game")
  }
}
