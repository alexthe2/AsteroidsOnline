package nl.rug.aoop
package Client.networking

import Client.input.InputControl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

/**
 * The sender sends the current player input to the Server
 */
object Sender {
  /**
   * Create a sender
   *
   * @param addr The ip address of the receiver
   * @param port The port of the receiver
   * @param uuid The uuid of the player
   * @return An actor of the Sender
   */
  def apply(addr: InetAddress, port: Int, uuid: String): Behavior[Command] =
    Behaviors.setup(ctx => new Sender(ctx, addr, port, uuid))

  /**
   * The command trait for this Actor
   */
  sealed trait Command

  /**
   * Send the current state
   *
   * @param delay The delay between this and the next message
   */
  final case class Send(delay: Long) extends Command
}

class Sender(context: ActorContext[Sender.Command], addr: InetAddress, port: Int, uuid: String) extends AbstractBehavior[Sender.Command](context) {

  /**
   * The Socket to send on
   */
  private val socket = new DatagramSocket(0)

  context.log.info("Sender created to {}/{} from port: {}", addr.getHostAddress, port, socket.getLocalPort)

  import Sender._

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case Send(delay) => {
        val data = (uuid + "-'" + InputControl.toString + "'").getBytes("UTF-8")
        val packet = new DatagramPacket(data, data.length, addr, port)

        socket.send(packet)

        context.scheduleOnce(delay milliseconds, context.self, Send(delay))
        this
      }
    }
  }
}
