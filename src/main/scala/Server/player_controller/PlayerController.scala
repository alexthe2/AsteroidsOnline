package nl.rug.aoop
package Server.player_controller

import Server.components.SSPlayer
import Server.game_handling.{GameSupervisor, PlayerSupervisor}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.util.UUID

/**
 * Create a new PlayerController
 */
object PlayerController {
  def apply(supervisor: ActorRef[GameSupervisor.Command], playerSupervisor: ActorRef[PlayerSupervisor.Command]): Behavior[Command] = Behaviors.setup(ctx => new PlayerController(ctx, playerSupervisor, supervisor))

  sealed trait Command

  /**
   * Create a Player with the given id at the position
   *
   * @param id       The id of the player
   * @param startPos The start position
   */
  final case class CreatePlayer(id: UUID, startPos: (Double, Double), name: String) extends Command

  /**
   * Leave the game
   */
  final case class Leave() extends Command

  /**
   * Change the movement input of the player
   *
   * @param input The input string
   */
  final case class MovePlayer(input: String) extends Command
}

class PlayerController(context: ActorContext[PlayerController.Command], playerSupervisor: ActorRef[PlayerSupervisor.Command], supervisor: ActorRef[GameSupervisor.Command]) extends AbstractBehavior[PlayerController.Command](context) {
  import PlayerController._

  /**
   * The Player for this controller
   */
  private var player: Option[SSPlayer] = Option.empty

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case CreatePlayer(id, pos, name) => {
        player = Some(new SSPlayer(pos, id, name, supervisor, playerSupervisor))
        supervisor ! GameSupervisor.AddGameObject(player.get)
        this
      }

      case MovePlayer(input) => {
        if (player.isEmpty) {
          return this
        }

        def convert(e: Char): Int = if (e == '0') 0 else 1

        val c1 = convert(input.charAt(0))
        val c2 = convert(input.charAt(1))
        val c3 = convert(input.charAt(2))
        val c4 = convert(input.charAt(3))

        player.get.changeInput(List(c1, c2, c3, c4))
        this
      }

      case Leave() => {
        val playerDead = player.get.lives <= 0
        playerSupervisor ! PlayerSupervisor.RemovePlayer(playerDead)

        if (!playerDead) {
          supervisor ! GameSupervisor.RemoveGameObject(player.get)
        }

        Behaviors.stopped
      }
    }
}
