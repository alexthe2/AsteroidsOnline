package nl.rug.aoop
package Server

import Client.controllers.HostController
import nl.rug.aoop.Server.game_handling._
import nl.rug.aoop.Server.ticks.TickServer

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

/**
 * The CherryServer represents the Server of the Asteroids Game
 */
object Server {
  /**
   * Create a new Server as an Actor
   *
   * @return The Server Actor
   */
  def apply(): Behavior[ServerCommand] =
    Behaviors.setup[ServerCommand](ctx => new Server(ctx))

  /**
   * The trait for this Actor
   */
  sealed trait ServerCommand

  /**
   * Start the server and inform the host of the port the TCP receiver runs on
   *
   * @param host a Reference to the HostController
   */
  final case class StartHostServer(host: ActorRef[HostController.Command]) extends ServerCommand

  /**
   * Terminate the Server
   */
  final case class Suicide() extends ServerCommand
}

class Server(context: ActorContext[Server.ServerCommand]) extends AbstractBehavior[Server.ServerCommand](context) {
  context.log.info("Server started")
  private val TICK_RATE = 50

  import Server._

  /**
   * The TickServer for this Game
   */
  private val tickServer = context.spawn(TickServer(TICK_RATE), "tickServer")

  /**
   * The Game
   */
  private val game = context.spawn(GameSupervisor(tickServer, context.self), "mainGame")

  override def onMessage(msg: Server.ServerCommand): Behavior[Server.ServerCommand] = {

    msg match {
      case StartHostServer(host) => {
        game ! GameSupervisor.InformHost(host)
        this
      }

      case Suicide() => {
        context.log.info("Server committed suicide")
        Behaviors.stopped
      }
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Server.ServerCommand]] = {
    case PostStop =>
      context.log.info("Server stopped")
      this
  }
}
