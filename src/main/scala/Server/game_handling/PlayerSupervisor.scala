package nl.rug.aoop
package Server.game_handling

import Client.controllers.HostController
import Server.communication._
import Server.components.SSPlayer
import Server.player_controller.PlayerController
import Shared.additional_objects.DeadPlayer

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * The Player Supervisor is responsible for the communication & registration of Players
 */
object PlayerSupervisor {

  /**
   * Create a new Player Supervisor as an Actor
   *
   * @param supervisor The GameSupervisor
   * @return A Player Supervisor as an Actor
   */
  def apply(supervisor: ActorRef[GameSupervisor.Command]): Behavior[Command] =
    Behaviors.setup[Command](ctx => new PlayerSupervisor(ctx, supervisor))

  sealed trait Command

  /**
   * Set the port for the receiver and start accepting connections
   * @param localPort The port of the receiver
   */
  final case class SetReceiverPortAndStart(localPort: Int) extends Command


  /**
   * Create a new player also updating the player count
   * @param id The uuid of the player
   */
  final case class CreateNewPlayer(id: UUID, name: String) extends Command

  /**
   * Create a new spectator
   * @param addr The address for the UDP of the spectator
   * @param port The address for the UDP of the spectator
   */
  final case class CreateNewSpectator(addr: String, port: Int) extends Command

  /**
   * Handle a game ready from the client
   * If all clients are ready the game is started
   */
  final case class HandleGameReady() extends Command

  /**
   * Configure this Player Supervisor
   */
  final case class Configure() extends Command

  /**
   * Start the Receiver & Sender
   */
  final case class Start() extends Command

  /**
   * Update the message for the master spammer
   * @param message The new message
   */
  final case class UpdateMessageToSend(message: String) extends Command

  /**
   * Inform the host of the port the TCP receiver runs on
   * @param host a Reference to the HostController
   */
  final case class InformHost(host: ActorRef[HostController.Command]) extends Command

  final case class HandlePlayerDies(player: SSPlayer) extends Command

  final case class InformLeave(clientUUID: UUID) extends Command

  final case class CheckReadyOrDead() extends Command

  final case class RemovePlayer(playerDead: Boolean) extends Command

  final case class Terminate() extends Command
}

class PlayerSupervisor(context: ActorContext[PlayerSupervisor.Command], supervisor: ActorRef[GameSupervisor.Command]) extends AbstractBehavior[PlayerSupervisor.Command](context) {

  /**
   * Create the UDP Spammer for this game
   */
  private val masterSpammer = context.spawn(MasterSpammer(0, context.self), "masterSpammer")

  /**
   * Create the TCP register for this game
   */
  private val masterRegister = context.spawn(MasterRegister(0, masterSpammer, context.self), "masterRegister")

  /**
   * Create the UDP register for this game
   */
  private val masterReceiver = context.spawn(MasterReceiver(0, context.self), "masterReceiver")

  /**
   * The Amount of players in game
   */
  private var playerCount = 0

  /**
   * The Amount of players that are ready
   */
  private var playerReady = 0

  /**
   * The Amount of players that are dead
   */
  private var playerDead = 0

  /**
   * A reference list with the player controllers, mapped to their uuid
   */
  private val playerControllers = scala.collection.mutable.Map.empty[UUID, ActorRef[PlayerController.Command]]

  /**
   * A connection to the DB
   */
  private val dbCon = context.spawn(DBConnector(), "dbConn")

  import PlayerSupervisor._

  override def onMessage(msg: PlayerSupervisor.Command): Behavior[PlayerSupervisor.Command] =
    msg match {
      case Configure() => {
        masterSpammer ! MasterSpammer.ChangeDelay(20)
        this
      }

      case Start() => {
        masterSpammer ! MasterSpammer.SendMessage()
        masterReceiver ! MasterReceiver.HandleMessage()

        this
      }

      case SetReceiverPortAndStart(localPort) => {
        masterRegister ! MasterRegister.SetReceiverPort(localPort)
        masterRegister ! MasterRegister.AcceptRegistration()

        this
      }

      case CreateNewPlayer(id, name) => {
        val playerController = context.spawn(PlayerController(supervisor, context.self), "controller-" + id.toString)
        playerController ! PlayerController.CreatePlayer(id, (40, 40), name)
        playerControllers.addOne((id, playerController))

        masterReceiver ! MasterReceiver.RegisterNewReceiver(id, playerController)
        playerCount += 1
        this
      }

      case UpdateMessageToSend(message) => {
        masterSpammer ! MasterSpammer.ChangeMessage(message)
        this
      }

      case HandleGameReady() => {
        playerReady += 1
        context.self ! CheckReadyOrDead()

        this
      }

      case InformHost(host) => {
        masterRegister ! MasterRegister.InformHost(host)
        this
      }

      case HandlePlayerDies(player) => {
        supervisor ! GameSupervisor.RemoveGameObject(player)
        supervisor ! GameSupervisor.AddStatic(new DeadPlayer(player.name, player.points.toInt.toString))
        dbCon ! DBConnector.AddScore(player.name, player.points.toInt)

        playerDead += 1
        context.self ! CheckReadyOrDead()
        this
      }

      case InformLeave(clientUUID) => {
        playerControllers(clientUUID) ! PlayerController.Leave()

        this
      }

      case RemovePlayer(dead) => {
        playerCount -= 1
        if (dead) {
          playerDead -= 1
        }

        context.self ! CheckReadyOrDead()
        this
      }

      case CheckReadyOrDead() => {
        if (playerCount == playerReady) {
          supervisor ! GameSupervisor.Start()
        }

        if (playerDead == playerCount) {
          context.log.info("GAME ENDED!!!")
          masterSpammer ! MasterSpammer.DeathRow()
          context.scheduleOnce(10 seconds, supervisor, GameSupervisor.Terminate())
        }

        this
      }

      case CreateNewSpectator(addr, port) => {
        masterSpammer ! MasterSpammer.AddNewReceiver(addr, port)
        this
      }

      case Terminate() => {
        supervisor ! GameSupervisor.Terminate()
        this
      }
    }
}
