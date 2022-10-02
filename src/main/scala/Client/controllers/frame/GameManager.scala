package nl.rug.aoop
package Client.controllers.frame

import Client.controllers.{ClientController, HostController}
import Client.graphics.frames.{ClientFrame, LeaderboardFrame}
import Server.communication.DBConnector

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import javax.swing.JOptionPane
import scala.util.Random

/**
 * The GameManager is the main manager
 */
object GameManager {
  /**
   * Create a new GameManager as an Actor
   *
   * @return
   */
  def apply(): Behavior[Command] =
    Behaviors.setup(ctx => new GameManager(ctx))

  /**
   * The command trait for this Actor
   */
  sealed trait Command

  /**
   * Start a Single Player Game
   */
  final case class StartSinglePlayerGame() extends Command

  /**
   * Start the Multiplayer Game
   */
  final case class StartMultiPlayerGame() extends Command

  /**
   * Join a multiplayer Game
   */
  final case class JoinMultiPlayerGame() extends Command

  /**
   * Spectate a multiplayer Game
   */
  final case class SpectateMultiPlayerGame() extends Command

  /**
   * Start the process to show the Leaderboard
   */
  final case class ShowLeaderboard() extends Command

  /**
   * Show the leaderboard
   *
   * @param data The data to display
   */
  final case class DisplayLeaderboard(data: List[(String, String, String)]) extends Command

  /**
   * Called when the frame closes, kills every actor in the system, by committing suicide
   */
  final case class Stop() extends Command
}

class GameManager(context: ActorContext[GameManager.Command]) extends AbstractBehavior[GameManager.Command](context) {
  context.log.info("Started new GameManager {}", context.self.path)

  new ClientFrame(context.self)

  /**
   * The DBConn Actor to show the leaderboard if necessary
   */
  private val dbConn = context.spawn(DBConnector(), "conn")

  import GameManager._

  def getName: String = {
    var name = "#"
    val nameRegex = "[a-zA-Z]{3,12}".r
    while (name != null && !nameRegex.matches(name)) {
      name = JOptionPane.showInputDialog("Give your player a name, it should only contain alphabetic characters and be between 3 and 12 characters long")
    }

    name
  }

  override def onMessage(msg: GameManager.Command): Behavior[GameManager.Command] =
    msg match {
      case StartSinglePlayerGame() => {
        val name = getName
        val game = context.spawn(HostController(name), "game-" + Random.nextInt(1000) + "-" + name)
        game ! HostController.Start()
        this
      }

      case StartMultiPlayerGame() => {
        val name = getName
        val game = context.spawn(HostController(name, multiplayer = true), "multi-" + Random.nextInt(1000) + "-" + name)
        game ! HostController.Start()
        this
      }

      case JoinMultiPlayerGame() => {
        val name = getName
        if (name == null || name.isEmpty || name.isBlank) {
          return this
        }

        val ip = JOptionPane.showInputDialog("Provide the IP-Address of the host")
        val port = JOptionPane.showInputDialog("Provide the port of the host")

        val game = context.spawn(ClientController(ip, port), "multi-" + Random.nextInt(1000))
        game ! ClientController.Start(name)
        this
      }

      case SpectateMultiPlayerGame() => {
        val ip = JOptionPane.showInputDialog("Provide the IP-Address of the host")
        val port = JOptionPane.showInputDialog("Provide the port of the host")

        val game = context.spawn(ClientController(ip, port), "spec-" + Random.nextInt(1000))
        game ! ClientController.StartSpec()
        this
      }

      case ShowLeaderboard() => {
        dbConn ! DBConnector.CallbackAll(context.self)
        this
      }

      case DisplayLeaderboard(list) => {
        new LeaderboardFrame(list)
        this
      }

      case Stop() => Behaviors.stopped
    }
}
