package nl.rug.aoop
package Server.communication

import Global.Config
import Server.game_handling.PlayerSupervisor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.net.{DatagramPacket, DatagramSocket, InetAddress, SocketException}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * The MasterSpammer sends a message to all ports registered to it
 */
object MasterSpammer {
  /**
   * Create a new MasterSpammer as an Actor
   * @param port The port it should run on
   * @param parent The GameSupervisor that it should inform of it's port
   * @return An actor representing the MasterSpammer
   */
  def apply(port: Int, parent: ActorRef[PlayerSupervisor.Command]): Behavior[Command] =
    Behaviors.setup[Command](ctx => new MasterSpammer(ctx, parent, port))

  /**
   * A command trait for this Actor
   */
  sealed trait Command;

  /**
   * Add a new receiver
   *
   * @param address The address of the receiver
   * @param port    The port the receiver
   */
  final case class AddNewReceiver(address: String, port: Int) extends Command

  /**
   * Change the delay between messages
   *
   * @param delay The new delay in milliseconds
   */
  final case class ChangeDelay(delay: Int) extends Command

  /**
   * Change the message that should be distributed
   *
   * @param newMessage The new message
   */
  final case class ChangeMessage(newMessage: String) extends Command

  /**
   * Sends an exit message to everyone
   */
  final case class DeathRow() extends Command

  /**
   * Send the message to all registered receivers
   */
  final case class SendMessage() extends Command
}

class MasterSpammer(context: ActorContext[MasterSpammer.Command], parent: ActorRef[PlayerSupervisor.Command], port: Int) extends AbstractBehavior[MasterSpammer.Command](context) {

  /**
   * The Socket this server runs on
   */
  private val socket = new DatagramSocket(port)

  context.log.info("Started UDP Spammer on port {}", socket.getLocalPort)

  /**
   * All clients this should send too
   */
  private val connected = ListBuffer.empty[(String, Int)]

  /**
   * The message it should send
   */
  private var message = ""

  /**
   * The delay between messages
   */
  private var delay: Int = 0

  import MasterSpammer._

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case AddNewReceiver(address, port) => {
        context.log.info2("Added {}/{} as a new receiver", address, port)
        connected += ((address, port))
        this
      }

      case ChangeMessage(newMessage) => {
        message = newMessage
        this
      }

      case SendMessage() => {
        val byteMessage = message.getBytes("UTF-8")
        sendToAll(byteMessage)

        context.scheduleOnce(delay milliseconds, context.self, SendMessage())
        this
      }

      case ChangeDelay(delay) => {
        context.log.info("Changing delay to {} milliseconds", delay)
        this.delay = delay
        this
      }

      case DeathRow() => {
        val end = Config.QUIT_MESSAGE.getBytes
        sendToAll(end)

        context.scheduleOnce(5 milliseconds, context.self, DeathRow())
        this
      }
    }
  }

  /**
   * Send to all known ports
   *
   * @param msg The message to send
   */
  def sendToAll(msg: Array[Byte]): Unit = {
    try {
      connected.foreach(client => {
        val packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(client._1), client._2)
        socket.send(packet)
      })
    } catch {
      case ex: SocketException => {
        context.log.error("Failed sending a message with the length {} with following ex: {}", msg.length, ex.getMessage)
      }
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      socket.close()

      this
  }
}
