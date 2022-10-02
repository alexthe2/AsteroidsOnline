package nl.rug.aoop
package Client.controllers

import Server.Server

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

/**
 * The HostController is the highest Instance a Game can be, it starts a Server and the Client and connects both of them
 */
object HostController {

  /**
   * Create a HostController Actor
   *
   * @param playerName  The name of the player
   * @param multiplayer Whether ot not this is a multiplayer game
   * @return A HostController Actor
   */
  def apply(playerName: String, multiplayer: Boolean = false): Behavior[Command] =
    Behaviors.setup(ctx => new HostController(ctx, playerName, multiplayer))

  /**
   * The Command trait for this Actor
   */
  sealed trait Command

  /**
   * Set the port of the host and start the client
   *
   * @param port The port
   */
  final case class SetHostPort(port: Int) extends Command

  /**
   * Start the process
   */
  final case class Start() extends Command

  /**
   * Start a client
   *
   * @param port The port to start on
   */
  final case class StartClient(port: Int) extends Command

  /**
   * Start the Server
   */
  final case class StartServer() extends Command
}

class HostController(context: ActorContext[HostController.Command], playerName: String, multiplayer: Boolean) extends AbstractBehavior[HostController.Command](context) {
  context.log.info("Created HostController")

  import HostController._

  override def onMessage(msg: HostController.Command): Behavior[HostController.Command] = {
    msg match {
      case Start() => {
        context.log.info("Hosting Game")
        context.self ! StartServer()

        this
      }

      case StartServer() => {
        val server = context.spawn(Server(), "server")
        server ! Server.StartHostServer(context.self)

        this
      }

      case SetHostPort(port) => {
        context.self ! StartClient(port)
        this
      }

      case StartClient(port) => {
        context.log.info("Starting host client, receiver runs on port: {}", port)
        val client = context.spawn(ClientController("localhost", port.toString, host = true), "client")
        client ! ClientController.Start(playerName, multiplayer)

        this
      }
    }
  }
}
