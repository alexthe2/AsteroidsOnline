package nl.rug.aoop
package Client.controllers.frame

import Client.graphics.frames.GameFrame
import Client.networking.ClientButler
import Global.Serializer
import Shared.additional_objects.ExportObject

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}

import java.net.InetAddress
import javax.swing.{JMenu, JOptionPane}
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

/**
 * The ClientFrameManager is the Actor Manager for the ClientFrame
 */
object ClientFrameManager {
  /**
   * Create a new Manager as an Actor
   *
   * @param butler An Reference to the ClientButler
   * @return The Manager as an Actor
   */
  def apply(butler: ActorRef[ClientButler.Command]): Behavior[Command] =
    Behaviors.setup(ctx => new ClientFrameManager(ctx, butler))

  /**
   * The Command trait for this Actor
   */
  sealed trait Command

  /**
   * Update the port of this Manager from the CC
   *
   * @param port The port
   */
  final case class AddPort(port: Int) extends Command

  /**
   * Inform about stopping the Game
   */
  final case class InformStop() extends Command

  /**
   * Kill the frame by disabling this
   */
  final case class Kill() extends Command

  /**
   * Start the frame update with a refreshment rate around 60fps
   *
   * @param delay The delay between frame updates
   */
  final case class StartFrameUpdate(delay: Long = 14) extends Command

  /**
   * Set the message that came from the server
   *
   * @param message Update the message from the server
   */
  final case class SetMessage(message: String) extends Command

  /**
   * Update the frame (will recall itself)
   *
   * @param delay The delay inbetween
   */
  final case class UpdateFrame(delay: Long) extends Command
}

class ClientFrameManager(context: ActorContext[ClientFrameManager.Command], butler: ActorRef[ClientButler.Command]) extends AbstractBehavior[ClientFrameManager.Command](context) {
  /**
   * The GameFrame
   */
  private val frame = new GameFrame("Asteroids", butler)

  /**
   * The list of current objects to be displayed
   */
  private var data = List.empty[ExportObject]

  import ClientFrameManager._

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case StartFrameUpdate(delay) => {
        context.self ! UpdateFrame(delay)
        this
      }

      case UpdateFrame(delay) => {
        frame.panel.updateData(data)
        frame.repaint()

        context.scheduleOnce(delay milliseconds, context.self, UpdateFrame(delay))
        this
      }

      case SetMessage(message) => {
        val newData = Serializer.deSerialize(message)
        if (newData.isFailure) {
          context.log.error("Couldn't deserialize message")
        } else {
          data = newData.get
        }

        this
      }

      case AddPort(port) => {
        context.log.info("Will update to show port {}", port)
        frame.getJMenuBar.add(new JMenu("Connect to " + InetAddress.getLocalHost.getHostAddress + ":" + port))
        this
      }

      case InformStop() => {
        JOptionPane.showMessageDialog(frame, "Game ended :)), will update leaderboard")
        this
      }

      case Kill() => {
        frame.setVisible(false)
        frame.dispose()
        Behaviors.stopped
      }
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      frame.setVisible(false)
      frame.dispose()
      this
  }
}
