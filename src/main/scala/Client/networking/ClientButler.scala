package nl.rug.aoop
package Client.networking

import Client.controllers.ClientController

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket
import javax.swing.JOptionPane

/**
 * The ClientButler is the Client Sided communication over TCP with the Server
 */
object ClientButler {
  /**
   * Create a Client Actor
   *
   * @param socket     The socket the actor should run on
   * @param supervisor The supervisor
   * @param host       Whether ot not the game is a host
   * @return The ClientButler as an Actor
   */
  def apply(socket: Socket, supervisor: ActorRef[ClientController.Command], host: Boolean = false): Behavior[Command] =
    Behaviors.setup(ctx => new ClientButler(ctx, socket, supervisor, host))

  /**
   * The command trait for this Actor
   */
  sealed trait Command

  /**
   * Register this Client on the server
   *
   * @param name The player name
   */
  final case class Register(name: String) extends Command

  /**
   * Register a Spectator to the Server
   *
   * @param ip   The ip of the UDP Receiver
   * @param port The port of the UDP Receiver
   */
  final case class RegisterSpec(ip: String, port: String) extends Command

  /**
   * Register a Receiver
   *
   * @param ip   The ip of the UDP Receiver
   * @param port The port of the UDP Receiver
   */
  final case class RegisterReceiver(ip: String, port: String) extends Command

  /**
   * Send a Start to to the Server
   */
  final case class SendStart() extends Command

  /**
   * Send a Stop to the Server, either disconnecting the Client or closing the game if the client is the host
   */
  final case class SendStop() extends Command

  /**
   * Verify that the connection is alive, or terminate if it's dead
   */
  final case class VerifyAlive() extends Command
}

class ClientButler(context: ActorContext[ClientButler.Command], socket: Socket, supervisor: ActorRef[ClientController.Command], host: Boolean) extends AbstractBehavior[ClientButler.Command](context) {

  import ClientButler._

  /**
   * The OOS for the communication
   */
  private val out = new ObjectOutputStream(socket.getOutputStream)

  /**
   * The OIS for the communication
   */
  private val in = new ObjectInputStream(socket.getInputStream)

  override def onMessage(msg: ClientButler.Command): Behavior[ClientButler.Command] = {
    msg match {
      case Register(name) => {
        val uuid = send("client.register." + name)
        if (uuid.isEmpty || uuid.matches("FAILED")) {
          context.log.info("Failed to register")
          return Behaviors.stopped
        }

        supervisor ! ClientController.InformUUID(uuid)
        this
      }

      case RegisterSpec(ip, port) => {
        val reg = send("client.spectate.addr'" + ip + "'#port'" + port + "'")
        if (reg != "game.spectator.added") {
          context.log.info("Failed to connect")
        }

        this
      }

      case RegisterReceiver(ip, port) => {
        val messageToSend = "addr'" + ip + "'#port'" + port + "'"
        val portToSendTo = send(messageToSend)
        if (portToSendTo.isEmpty || !portToSendTo.matches("[0-9]+")) {
          context.log.info("Received misinformation from server while registering")
          return Behaviors.stopped
        }

        supervisor ! ClientController.InformSenderPort(portToSendTo.toInt)
        this
      }

      case SendStart() => {
        send("client.start")
        this
      }

      case SendStop() => {
        val message = "client." + (if (host) "exit" else "leave")
        context.log.info("Leaving game with {}", message)
        send(message)
        supervisor ! ClientController.InformLeave()
        this
      }

      case VerifyAlive() => {
        context.log.info("Verifying server connection")
        val message = "client.ping"
        val response = send(message)
        context.log.info("Received: {}", response)

        if (response != "server.pong") {
          JOptionPane.showMessageDialog(null, "Server disconnected")
          supervisor ! ClientController.InformLeave()
        }
        this
      }
    }
  }

  /**
   * Send a String and receive the reply
   *
   * @param msg The message to send
   * @return The reply
   */
  private def send(msg: String): String = {
    out.writeObject(msg)
    out.flush()
    out.reset()

    in.readObject().asInstanceOf[String]
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Shutting down connection to {}", socket.getRemoteSocketAddress.toString)
      out.close()
      in.close()
      socket.close()

      this
  }
}
