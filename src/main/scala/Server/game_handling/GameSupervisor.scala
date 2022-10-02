package nl.rug.aoop
package Server.game_handling

import Client.controllers.HostController
import Global.Serializer
import Server.components.SelfMovable
import Server.ticks.TickServer
import Server.Server
import Shared.additional_objects.ExportObject
import Shared.game_objects.GameObject

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import nl.rug.aoop.Server.Server.Suicide

import scala.collection.mutable.ListBuffer

/**
 * The Game Supervisor is the main supervisor of the game state and responsible for running the game
 */
object GameSupervisor {
  /**
   * Create the GameSupervisor as an Actor
   *
   * @param tickServer A reference to the TickServer
   * @return
   */
  def apply(tickServer: ActorRef[TickServer.Command], server: ActorRef[Server.ServerCommand]): Behavior[Command] =
    Behaviors.setup[Command](ctx => new GameSupervisor(ctx, tickServer, server))

  /**
   * The GameSupervisor commands
   */
  sealed trait Command

  /**
   * Add a new GameObject, will forward to the PhysicsEngine
   * @param selfMovable The GameObject to Add
   */
  final case class AddGameObject(selfMovable: SelfMovable) extends Command

  /**
   * Add a Static information to the info that should be send
   *
   * @param obj The static info
   */
  final case class AddStatic(obj: ExportObject) extends Command

  /**
   * Configure this Game
   */
  final case class Configure() extends Command

  /**
   * Inform the host of the port the TCP receiver runs on
   *
   * @param host a Reference to the HostController
   */
  final case class InformHost(host: ActorRef[HostController.Command]) extends Command


  /**
   * Remove a GameObject, will forward to the PhysicsEngine
   *
   * @param selfMovable The GameObject to remove
   */
  final case class RemoveGameObject(selfMovable: SelfMovable) extends Command

  /**
   * Start the game
   */
  final case class Start() extends Command

  /**
   * Update the state of the game and inform the UDP Spammer
   *
   * @param newObjects The new game state
   */
  final case class UpdateState(newObjects: List[GameObject]) extends Command

  /**
   * Tell the Server to Stop
   */
  final case class Terminate() extends Command
}

class GameSupervisor(context: ActorContext[GameSupervisor.Command], tickServer: ActorRef[TickServer.Command], server: ActorRef[Server.ServerCommand]) extends AbstractBehavior[GameSupervisor.Command](context) {
  context.log.info("Created Game Supervisor: {}", context.self.path)

  /**
   * PhysicsEngine for this game
   */
  private val physicsEngine = context.spawn(PhysicsComponentHandler(context.self), "physicsEngine")

  /**
   * PlayerSupervisor for this game
   */
  private val playerSupervisor = context.spawn(PlayerSupervisor(context.self), "playerSupervisor")

  /**
   * Is the game started
   */
  private var gameStarted = false

  /**
   * Static additions to the message send, like dead player or other info
   */
  private val staticAdditions = ListBuffer.empty[ExportObject]

  import GameSupervisor._
  context.self ! Configure()

  override def onMessage(msg: GameSupervisor.Command): Behavior[GameSupervisor.Command] = {
    msg match {
      case Configure() => {
        tickServer ! TickServer.AddInvoke(physicsEngine)
        playerSupervisor ! PlayerSupervisor.Configure()
        playerSupervisor ! PlayerSupervisor.Start()

        this
      }

      case Start() => {
        if (!gameStarted) {
          gameStarted = true
          context.log.info("Game starting")
          tickServer ! TickServer.StartServer()
        }
        this
      }

      case UpdateState(objects) => {
        val convData = Serializer.serialize(objects.map(G => G.serialize) ++ staticAdditions.toList)
        if (convData.isFailure) {
          context.log.error("Can't convert Data: {}", objects)
          return this
        }

        playerSupervisor ! PlayerSupervisor.UpdateMessageToSend(convData.get)
        this
      }

      case AddGameObject(gameObject) => {
        physicsEngine ! PhysicsComponentHandler.AddNewGameObject(gameObject)
        this
      }

      case RemoveGameObject(gameObject) => {
        physicsEngine ! PhysicsComponentHandler.RemoveGameObject(gameObject)
        this
      }

      case InformHost(host) => {
        playerSupervisor ! PlayerSupervisor.InformHost(host)
        this
      }

      case AddStatic(obj) => {
        staticAdditions += obj
        this
      }

      case Terminate() => {
        server ! Suicide()
        this
      }
    }
  }
}
