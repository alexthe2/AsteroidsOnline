package nl.rug.aoop
package Server.communication

import Server.game_handling.PlayerSupervisor
import Server.player_controller.PlayerController

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.net.{DatagramPacket, DatagramSocket}
import java.util.UUID
import scala.collection.mutable

/**
 * The MasterReceiver receives the input from all clients via UDP and distributes them to their Player Controllers
 */
object MasterReceiver {
  /**
   * Create a MasterReceiver as an Actor
   * @param port The port it should work on
   * @param supervisor The PlayerSupervisor it should report to
   * @return A MasterReceiver as an Actor
   */
  def apply(port: Int, supervisor: ActorRef[PlayerSupervisor.Command]): Behavior[Command] = {
    Behaviors.setup(ctx => new MasterReceiver(ctx, port, supervisor))
  }

  /**
   * The Command trait for this Actor
   */
  sealed trait Command

  /**
   * Handle a new Message that was received
   */
  case class HandleMessage() extends Command

  /**
   * Register a new Player Client
   * @param id The id under which it works
   * @param playerController The controller for the player
   */
  case class RegisterNewReceiver(id: UUID, playerController: ActorRef[PlayerController.Command]) extends Command
}

class MasterReceiver(context: ActorContext[MasterReceiver.Command], port: Int, supervisor: ActorRef[PlayerSupervisor.Command]) extends AbstractBehavior[MasterReceiver.Command](context) {

  /**
   * The Socket this receiver runs on
   */
  private val socket = new DatagramSocket(port)
  context.log.info("Started UDP Master Listener on port {}", socket.getLocalPort)
  supervisor ! PlayerSupervisor.SetReceiverPortAndStart(socket.getLocalPort)

  /**
   * All Receivers mapped to their UUID
   */
  private val allReceivers = mutable.HashMap.empty[UUID, ActorRef[PlayerController.Command]]

  /**
   * The message pattern of incoming messages: UUID-'INPUT'
   */
  private val messagePattern = "([a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12})-'([01]{4})'".r

  import MasterReceiver._

  override def onMessage(msg: MasterReceiver.Command): Behavior[MasterReceiver.Command] = {
    msg match {
      case RegisterNewReceiver(id, pc) => {
        allReceivers.put(id, pc)
      }

      case HandleMessage() => {
        /**
         * Handle the received message, by forwarding to the correct player controller
         *
         * @param msg The message
         */
        def handleMessage(msg: String): Unit = {
          msg match {
            case messagePattern(uuid, input) => {
              val playerC = allReceivers.get(UUID.fromString(uuid))
              if (playerC.isDefined) {
                playerC.get ! PlayerController.MovePlayer(input)
              }
            }

            case _ => {
              context.log.error("Received improper message")
            }
          }
        }

        val messageArr = Array.ofDim[Byte](64)
        val packet = new DatagramPacket(messageArr, messageArr.length)
        socket.receive(packet)

        val message = new String(messageArr, "UTF-8").split("\u0000")(0)
        handleMessage(message)

        context.self ! HandleMessage()
      }
    }

    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Shutting down receiver")
      socket.close()

      this
  }
}
