package nl.rug.aoop
package Client.networking

import Client.controllers.ClientController
import Client.controllers.ClientController.InformLeave
import Client.controllers.frame.ClientFrameManager
import Global.Config

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.net.{DatagramPacket, DatagramSocket, SocketTimeoutException}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * The Receiver is a UDP Receiver Actor
 */
object Receiver {

  /**
   * Create a new UDP Receiver
   *
   * @param port The port it should run on
   * @return A UDP Receiver Actor
   */
  def apply(port: Int, supervisor: ActorRef[ClientController.Command], frame: ActorRef[ClientFrameManager.Command]): Behavior[Command] =
    Behaviors.setup[Command](ctx => new Receiver(ctx, port, supervisor, frame))

  /**
   * The Command trait for this actor
   */
  sealed trait Command

  /**
   * Receive a Message and update the last message
   */
  final case class ReceiveMessage() extends Command
}

class Receiver(context: ActorContext[Receiver.Command], port: Int, supervisor: ActorRef[ClientController.Command], frame: ActorRef[ClientFrameManager.Command]) extends AbstractBehavior[Receiver.Command](context) {

  /**
   * Whether or not the receiver is still running
   */
  private var running = true

  /**
   * The port this runs on
   */
  private val socket = new DatagramSocket(port)

  context.log.info("Started UDP Listener on port {}", socket.getLocalPort)
  supervisor ! ClientController.SetReceiverPort(socket.getLocalPort)

  /**
   * The characters the messages end with
   */
  private val SPLIT_CHAR = '\u0000'

  import Receiver._

  override def onMessage(msg: Receiver.Command): Behavior[Receiver.Command] = {
    msg match {
      case ReceiveMessage() => {
        val messageArr = Array.ofDim[Byte](8196)
        val packet = new DatagramPacket(messageArr, messageArr.length)

        /**
         * Try receiving a message, if it cannot be received after 1 seconds, check whether the server is still alive
         *
         * @return Whether receiving was successful
         */
        def tryReceiving: Boolean = {
          socket.setSoTimeout(1000)
          try {
            socket.receive(packet)
            true
          } catch {
            case e: SocketTimeoutException => {
              supervisor ! ClientController.VerifyAlive()
              context.scheduleOnce(5 milliseconds, context.self, ReceiveMessage())
              false
            }
          }
        }

        /**
         * Handle the received message
         *
         * @param msg The message
         */
        def handleMessage(msg: String): Unit = {
          /**
           * If the message is a termination, handle the closing
           */
          def handleDeathRow(): Unit = {
            if (running) {
              running = false
              context.log.info("Received death command")
              context.scheduleOnce(5 seconds, supervisor, InformLeave())
              frame ! ClientFrameManager.InformStop()
            }
          }

          if (msg(0) != SPLIT_CHAR) {
            val message = msg.split(SPLIT_CHAR)(0)
            if (message == Config.QUIT_MESSAGE) {
              handleDeathRow()
            } else {
              frame ! ClientFrameManager.SetMessage(message)
            }
          }
        }

        if (!tryReceiving) {
          return this
        }

        handleMessage(new String(messageArr, "UTF-8"))
        context.scheduleOnce(5 milliseconds, context.self, ReceiveMessage())
        this
      }
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Shutting down Receiver on port {}", socket.getLocalPort)
      socket.close()

      this
  }
}
