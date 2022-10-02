package nl.rug.aoop
package Server.communication

import Server.game_handling.PlayerSupervisor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{Socket, SocketException}
import java.util.UUID

/**
 * The Butler represents a server sided PlayerConnection via TCP
 */
object Butler {
  /**
   * Create a new Butler as an Actor
   * @param newClient The client Connection
   * @param master The UDP Spammer
   * @param port The port the UDP Receiver runs on
   * @return The Butler as an Actor
   */
  def apply(newClient: Socket, master: ActorRef[MasterSpammer.Command], port: Int): Behavior[Command] = Behaviors.setup(ctx => new Butler(ctx, newClient, master, port))

  /**
   * The command trait for this Actor
   */
  sealed trait Command;

  /**
   * Initialize an incoming client connection
   * @param supervisor The GameSupervisor
   */
  final case class WelcomeClient(supervisor: ActorRef[PlayerSupervisor.Command]) extends Command

  /**
   * Register the client
   * @param supervisor The GameSupervisor
   */
  final case class RegisterClient(supervisor: ActorRef[PlayerSupervisor.Command]) extends Command

  /**
   * Support the client with problems
   * @param supervisor The GameSupervisor
   */
  final case class SupportClient(supervisor: ActorRef[PlayerSupervisor.Command]) extends Command

}

class Butler(context: ActorContext[Butler.Command], socket: Socket, master: ActorRef[MasterSpammer.Command], receiverPort: Int) extends AbstractBehavior[Butler.Command](context) {
  import Butler._

  context.log.info("Created Butler for {}", socket.getRemoteSocketAddress.toString)

  /**
   * The Input Stream for this connection
   */
  private val in = new ObjectInputStream(socket.getInputStream)

  /**
   * The Output Stream for this connection
   */
  private val out = new ObjectOutputStream(socket.getOutputStream)

  /**
   * The UUID for this client
   */
  private val clientUUID = UUID.randomUUID()

  /**
   * Handle an incoming message
   * @param expectedRegex The expected regex from the message
   * @param handleFunc A function that given the input will return a response
   * @return If the message matched
   */
  def handleMessage(expectedRegex: String, handleFunc: (String) => String): Boolean = {
    try {
      val input = in.readObject().asInstanceOf[String]
      context.log.info("Received: {}", input)
      if (input == null || input.isEmpty || !input.matches(expectedRegex)) {
        context.log.info("Failed against: {}", expectedRegex)
        return false
      }

      backWrite(handleFunc(input))
      return true
    } catch {
      case e: SocketException => {
        context.log.info("SocketException from {}, message: {}", clientUUID.toString, e.getMessage)
        Behaviors.stopped
      }
    }

    false
  }

  /**
   * Send a message to the other Butler
   *
   * @param msg The message to send
   */
  def backWrite(msg: String): Unit = {
    out.writeObject(msg)
    out.flush()
    out.reset()
  }

  /**
   * In case of a failure, shutdown the connection
   */
  def fail(): Unit = {
    out.writeObject("FAILED")
    out.flush()
    out.reset()

    Behaviors.stopped
  }

  override def onMessage(msg: Butler.Command): Behavior[Butler.Command] = {
    msg match {
      case WelcomeClient(supervisor) => {
        val expectedRegisterPattern = "client[\\.,# -]register[\\.,# -]([a-zA-z]{3,12})".r
        val expectedSpectatorPattern = "client[\\.,# -]spectate[\\.,# -]addr'([0-9\\.]+)'#port'([0-9]+)'".r

        /**
         * Create a Player with the given name
         * @param msg The input string
         * @return The UUID of the player
         */
        def createPlayer(name: String): String = {
          context.log.info("Creating player for {}", name)
          supervisor ! PlayerSupervisor.CreateNewPlayer(clientUUID, name)

          clientUUID.toString
        }

        /**
         * Create a Spectator
         * @param msg The input string
         */
        def specPlayer(addr: String, port: String): String = {
          supervisor ! PlayerSupervisor.CreateNewSpectator(addr, port.toInt)

          "game.spectator.added"
        }

        val request = in.readObject().asInstanceOf[String]
        request match {
          case expectedRegisterPattern(name) => {
            backWrite(createPlayer(name))
            context.self ! RegisterClient(supervisor)
          }
          case expectedSpectatorPattern(addr, port) => backWrite(specPlayer(addr, port))
          case _ => fail()
        }

        this
      }

      case RegisterClient(supervisor) => {
        val expectedPattern = "addr'([0-9\\.]+)'#port'([0-9]+)'"

        /**
         * Register function used by the message handler
         * @param message The input message
         * @return The result
         */
        def register(message: String): String = {
          val exportPattern = expectedPattern.r
          message match {
            case exportPattern(addr, port) => {
              master ! MasterSpammer.AddNewReceiver(addr, port.toInt)
            }
          }

          receiverPort.toString
        }

        if(!handleMessage(expectedPattern, register)) {
          fail()
        }

        context.self ! SupportClient(supervisor)
        this
      }

      case SupportClient(supervisor) => {
        def startGame(): String = {
          supervisor ! PlayerSupervisor.HandleGameReady()

          "game.acquired"
        }

        def leaveGame(): String = {
          supervisor ! PlayerSupervisor.InformLeave(clientUUID)

          "game.left"
        }

        def exitGame(): String = {
          supervisor ! PlayerSupervisor.Terminate()

          "game.exited"
        }

        val startPattern = "client[\\.,# -]start".r
        val leavePattern = "client[\\.,# -]leave".r
        val exitPattern = "client[\\.,# -]exit".r

        val request = in.readObject().asInstanceOf[String]
        request match {
          case startPattern() => backWrite(startGame())
          case leavePattern() => backWrite(leaveGame())
          case exitPattern() => backWrite(exitGame())
          case _ => backWrite("Unknown")
        }

        context.self ! SupportClient(supervisor)
        this
      }
    }
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
