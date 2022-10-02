package nl.rug.aoop
package Server.game_handling

import Server.components.SelfMovable
import Server.ticks.{TickCommand, TickMessage}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable.ListBuffer

/**
 * The PhysicsComponentHandle handles all self-movable components
 */
object PhysicsComponentHandler {
  def apply(parent: ActorRef[GameSupervisor.Command]): Behavior[TickCommand] =
    Behaviors.setup(ctx => new PhysicsComponentHandler(ctx, parent))

  /**
   * The Command of the PhysicsComponentHandler, it extends TickCommand in order to receive ticks
   */
  sealed trait Command extends TickCommand

  /**
   * Add a new GameObject to the PhysicsComponentHandler
   * @param gameObject The GameObject to add
   */
  final case class AddNewGameObject(gameObject: SelfMovable) extends Command

  /**
   * Check collisions between objects
   */
  final case class CheckCollisions() extends Command

  /**
   * After the components have been moved, update the supervisor
   */
  final case class CallbackToSupervisor() extends Command

  /**
   * Remove a GameObject from the PhysicsComponentHandler
   *
   * @param gameObject The GameObject to remove
   */
  final case class RemoveGameObject(gameObject: SelfMovable) extends Command
}

class PhysicsComponentHandler(context: ActorContext[TickCommand], parent: ActorRef[GameSupervisor.Command]) extends AbstractBehavior[TickCommand](context) {

  /**
   * All components handled by this Physics
   */
  private val components = ListBuffer.empty[SelfMovable]

  /**
   * Generator for random asteroids
   */
  private val generator = context.spawn(AsteroidNoiseGenerator(parent), "noise-generator")

  /**
   * How many ticks does it take to spawn a random Asteroid
   */
  private val TICK_INTERVAL = 40

  /**
   * The current tick count for the spawning
   */
  private var currTickCount = 0

  import PhysicsComponentHandler._

  override def onMessage(msg: TickCommand): Behavior[TickCommand] = {
    msg match {
      case TickMessage() => {
        /**
         * Handle the spawning of new asteroids
         */
        def handleSpawning(): Unit = {
          currTickCount += 1
          if (currTickCount >= TICK_INTERVAL) {
            currTickCount = 0
            generator ! AsteroidNoiseGenerator.GenerateNext(components.toList)
          }
        }

        components.foreach(f => {
          f.selfMove()
          f.boundaryMove()
        })

        context.self ! CheckCollisions()
        handleSpawning()
        this
      }

      case CheckCollisions() => {
        for(i <- components.indices) {
          for(j <- Range(i + 1, components.size)) {
            if (components(i).isColliding(components(j))) {
              components(i).onCollision(components(j))
              components(j).onCollision(components(i))
            }
          }
        }

        context.self ! CallbackToSupervisor()
        this
      }

      case CallbackToSupervisor() => {
        parent ! GameSupervisor.UpdateState(List.from(components))
        this
      }

      case AddNewGameObject(gb) => {
        components += gb
        this
      }

      case RemoveGameObject(gb) => {
        components -= gb
        this
      }
    }
  }
}
