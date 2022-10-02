package nl.rug.aoop
package Client.controllers

import Client.controllers.frame.ClientFrameManager
import Client.networking.{ClientButler, Receiver, Sender}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.net._
import javax.swing.JOptionPane
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * The ClientController is an Actor controlling the entire client
 */
object ClientController {
  /**
   * Create the Actor for this Client
   *
   * @param addr The address of the host
   * @param port The port of the host
   * @param host Whether this is the host
   * @return
   */
  def apply(addr: String, port: String, host: Boolean = false): Behavior[Command] =
    Behaviors.setup(ctx => new ClientController(ctx, addr, port, host))

  /**
   * The Command trait for this Actor
   */
  sealed trait Command

  /**
   * Configure the Actor
   */
  final case class Configure() extends Command

  /**
   * Inform the Controller to leave the game
   */
  final case class InformLeave() extends Command

  /**
   * Inform the Controller about the port the sender runs on
   *
   * @param port The port
   */
  final case class InformSenderPort(port: Int) extends Command

  /**
   * Inform the Controller about the UUID of this Client
   *
   * @param uuid The uuid
   */
  final case class InformUUID(uuid: String) extends Command

  /**
   * Set the Receiver port for this Client
   *
   * @param port The port
   */
  final case class SetReceiverPort(port: Int) extends Command

  /**
   * Start the Client
   *
   * @param name        The name of the player
   * @param multiplayer Whether or not to show the port
   */
  final case class Start(name: String, multiplayer: Boolean = false) extends Command

  /**
   * Start the Client as a Spectator
   */
  final case class StartSpec() extends Command

  /**
   * Try to continue the registration
   *
   * will only work if both UUID & Port are set
   */
  final case class TryContinueRegistration() extends Command

  /**
   * Receiver didn't receive data -> Verify alive connection
   */
  final case class VerifyAlive() extends Command

  /**
   * Failure on Startup => Suicide
   */
  final case class Suicide() extends Command
}

class ClientController(context: ActorContext[ClientController.Command], addr: String, port: String, host: Boolean) extends AbstractBehavior[ClientController.Command](context) {
  context.log.info("Created Client Controller")

  /**
   * The Socket this Client will run on
   */
  private var socket: Option[Socket] = Option.empty
  /**
   * The address the Client will run on/connect to
   */
  private var hostAddr: Option[InetAddress] = Option.empty
  /**
   * The UUID of the client
   */
  private var uuid: Option[String] = Option.empty

  /**
   * The receiver port
   */
  private var receiverPort: Option[Int] = Option.empty

  /**
   * Whether or not to the client is registered
   */
  private var registered = false

  /**
   * Whether or not to show the port
   */
  private var showPort = false

  /**
   * Whether the Actor Configured itself
   */
  private var configured = false

  import ClientController._

  try {
    hostAddr = Option.apply[InetAddress](InetAddress.getByName(addr))
    socket = Option.apply(new Socket(hostAddr.get, port.toInt))
    context.self ! Configure()
  } catch {
    case ex: Throwable => handleException(ex)
  }


  /**
   * The Butler for this client
   */
  private var butler: Option[ActorRef[ClientButler.Command]] = Option.empty
  /**
   * The FrameManager for this client
   */
  private var frameManager: Option[ActorRef[ClientFrameManager.Command]] = Option.empty
  /**
   * The Receiver for this client
   */
  private var receiver: Option[ActorRef[Receiver.Command]] = Option.empty

  /**
   * Handle the occurring Exception and terminate the client
   *
   * @param exception The exception that occurred
   */
  def handleException(exception: Throwable): Unit = {
    exception match {
      case ex: ConnectException => JOptionPane.showMessageDialog(null, "Host wrong or unknown")
      case ex: NumberFormatException => JOptionPane.showMessageDialog(null, "Port should be numeric")
      case ex: UnknownHostException => JOptionPane.showMessageDialog(null, "Unknown host")
      case ex: SocketException => JOptionPane.showMessageDialog(null, "Cannot connect to server")
      case default => JOptionPane.showMessageDialog(null, "An internal error occurred: " + exception.getMessage)
    }

    context.self ! Suicide()
  }

  override def onMessage(msg: ClientController.Command): Behavior[ClientController.Command] = {
    msg match {
      case Configure() => {
        if (!configured) {
          butler = Option.apply(context.spawn(ClientButler(socket.get, context.self, host), "butler"))
          frameManager = Option.apply(context.spawn(ClientFrameManager(butler.get), "frame-manager"))
          receiver = Option.apply(context.spawn(Receiver(0, context.self, frameManager.get), "receiver"))
        }

        configured = true
        this
      }

      case Start(name, multiplayer) => {
        if (!configured) {
          context.log.info("Not configured yet")
          context.scheduleOnce(5 milliseconds, context.self, Start(name, multiplayer))
          return this
        }

        showPort = multiplayer

        butler.get ! ClientButler.Register(name)
        frameManager.get ! ClientFrameManager.StartFrameUpdate()
        receiver.get ! Receiver.ReceiveMessage()

        if (receiverPort.isDefined && showPort) {
          frameManager.get ! ClientFrameManager.AddPort(this.port.toInt)
        }
        this
      }

      case StartSpec() => {
        if (receiverPort.isEmpty) {
          context.self ! StartSpec()
          return this
        }

        butler.get ! ClientButler.RegisterSpec(InetAddress.getLocalHost.getHostAddress.toString, receiverPort.get.toString)
        frameManager.get ! ClientFrameManager.StartFrameUpdate()
        receiver.get ! Receiver.ReceiveMessage()
        this
      }

      case InformUUID(uid) => {
        uuid = Option.apply(uid)
        context.self ! TryContinueRegistration()
        this
      }

      case SetReceiverPort(port) => {
        receiverPort = Option.apply(port)
        context.self ! TryContinueRegistration()
        context.log.info("Port is: {}", port)

        if (showPort) {
          frameManager.get ! ClientFrameManager.AddPort(this.port.toInt)
        }
        this
      }

      case TryContinueRegistration() => {
        if (registered || uuid.isEmpty || receiverPort.isEmpty) {
          return this
        }

        registered = true
        butler.get ! ClientButler.RegisterReceiver(InetAddress.getLocalHost.getHostAddress.toString, receiverPort.get.toString)
        this
      }

      case InformSenderPort(port) => {
        val sender = context.spawn(Sender(hostAddr.get, port, uuid.get), "sender")
        sender ! Sender.Send(10)
        this
      }

      case InformLeave() => {
        frameManager.get ! ClientFrameManager.Kill()

        context.log.info("Committing suicide cause player left")
        Behaviors.stopped
      }

      case VerifyAlive() => {
        butler.get ! ClientButler.VerifyAlive()
        this
      }

      case Suicide() => {
        Behaviors.stopped
      }
    }
  }
}
