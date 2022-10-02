package nl.rug.aoop
package Server.communication

import Client.controllers.HostController
import Server.game_handling.PlayerSupervisor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.net.ServerSocket

/**
 * The MasterRegister is the ServerSided TCP Master, handling all new clients
 */
object MasterRegister {

  /**
   * Create a new MasterRegister as an actor
   * @param port The port it should run on
   * @param master The UDP Sender of the server
   * @param supervisor The PlayerSupervisor it should report to
   * @return The MasterRegister as an Actor
   */
  def apply(port: Int, master: ActorRef[MasterSpammer.Command], supervisor: ActorRef[PlayerSupervisor.Command]): Behavior[Command] = Behaviors.setup(ctx => new MasterRegister(ctx, port, master, supervisor))

  /**
   * The command trait
   */
  sealed trait Command

  /**
   * Accept a new incoming registration and pass it to a Butler
   */
  final case class AcceptRegistration() extends Command

  /**
   * Set the port the UDP Spammer runs on
   * @param port The port
   */
  final case class SetReceiverPort(port: Int) extends Command

  /**
   * Inform the host of the port the TCP receiver runs on
   * @param host a Reference to the HostController
   */
  final case class InformHost(host: ActorRef[HostController.Command]) extends Command
}

class MasterRegister(context: ActorContext[MasterRegister.Command], port: Int, master: ActorRef[MasterSpammer.Command], supervisor: ActorRef[PlayerSupervisor.Command]) extends AbstractBehavior[MasterRegister.Command](context) {

  /**
   * The Socket this runs on
   */
  private val socket = new ServerSocket(port)

  /**
   * The UDP Port
   */
  private var updReceiverPort: Int = 0

  context.log.info("Listening on: {}", socket.getLocalPort)

  import MasterRegister._

  override def onMessage(msg: MasterRegister.Command): Behavior[MasterRegister.Command] = {
    msg match {
      case AcceptRegistration() => {
        val newClient = socket.accept()
        val butler = context.spawn(Butler(newClient, master, updReceiverPort), newClient.getRemoteSocketAddress.toString.substring(1))

        butler ! Butler.WelcomeClient(supervisor)
        context.self ! AcceptRegistration()
        this
      }

      case SetReceiverPort(port) => {
        updReceiverPort = port
        this
      }

      case InformHost(host) => {
        host ! HostController.SetHostPort(socket.getLocalPort)
        this
      }
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Shutting down registration on {}", socket.getLocalPort)
      socket.close()

      this
  }
}
