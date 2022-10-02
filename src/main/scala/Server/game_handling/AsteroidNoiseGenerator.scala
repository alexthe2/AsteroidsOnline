package nl.rug.aoop
package Server.game_handling

import Global.Config
import Server.components.SSAsteroid
import Shared.game_objects.GameObject

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.Random
import scala.util.control.Breaks.{break, breakable}

/**
 * The AsteroidNoiseGenerator handles the creation of new Asteroids
 */
object AsteroidNoiseGenerator {
  /**
   * Create an AsteroidNoiseGenerator as an Actor
   *
   * @param supervisor The GameSupervisor
   * @return An Actor of this
   */
  def apply(supervisor: ActorRef[GameSupervisor.Command]): Behavior[Command] =
    Behaviors.setup(ctx => new AsteroidNoiseGenerator(ctx, supervisor))

  /**
   * The Command trait for this Actor
   */
  sealed trait Command

  /**
   * Generate the next Asteroid, it's gonna randomly try 50 times
   * @param objects The current objects in the world
   */
  final case class GenerateNext(objects: List[GameObject]) extends Command
}

class AsteroidNoiseGenerator(context: ActorContext[AsteroidNoiseGenerator.Command], supervisor: ActorRef[GameSupervisor.Command]) extends AbstractBehavior[AsteroidNoiseGenerator.Command](context) {
  context.log.info("Asteroid Noise Generator created")

  /**
   * How many times the Generator should trie before ignoring the request
   */
  private val TRIES = 50

  /**
   * The minimal Distance to other Objects
   */
  private val DISTANCE_TO_OTHER = 15

  import AsteroidNoiseGenerator._

  override def onMessage(msg: AsteroidNoiseGenerator.Command): Behavior[AsteroidNoiseGenerator.Command] = {
    msg match {
      case GenerateNext(objs) => {
        breakable { for(_ <- Range(0, TRIES)) {
          val pos = (Random.nextDouble() * Config.GAME_WIDTH, Random.nextDouble() * Config.GAME_HEIGHT)

          val testAsteroid = new SSAsteroid(pos, (0,0), DISTANCE_TO_OTHER, supervisor)
          val noCollision = objs.foldRight(true)((A, B) => (!A.isColliding(testAsteroid) && B))

          if (noCollision) {
            val randomSize = Random.shuffle( List(1,2,4)).head
            val randomDir = ((Math.random()-.5), (Math.random()-.5))

            supervisor ! GameSupervisor.AddGameObject(new SSAsteroid(pos, randomDir, randomSize, supervisor))
            break
          }
        } }

        this
      }
    }
  }
}
