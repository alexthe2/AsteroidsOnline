package nl.rug.aoop
package Server

import akka.actor.typed.ActorSystem

/**
 * CherryMain - Internal Easter Egg, if for some reason you would want to start a Server separately
 */
object CherryMain {
  def main(args: Array[String]): Unit = {
    ActorSystem[Server.ServerCommand](Server(), "server")
  }
}
