package nl.rug.aoop
package Server.components

import Server.game_handling.GameSupervisor
import Shared.game_objects.{Asteroid, GameObject}

import akka.actor.typed.ActorRef

/**
 * Represents a ServerSided Asteroid that is thread-safe
 *
 * @param startPos   The starting position
 * @param moveVec    The self-move
 * @param size       The size of this asteroid
 * @param supervisor The game supervisor
 */
class SSAsteroid(startPos: (Double, Double), val moveVec: (Double, Double), override val size: Int, supervisor: ActorRef[GameSupervisor.Command]) extends Asteroid(startPos._1, startPos._2, size) with SelfMovable {
  /**
   * The score you receuve when you hit this asteroid
   */
  val score: Int = 5 - size

  override def selfMove(): Unit = {
    move(moveVec._1, moveVec._2)
  }

  /**
   * On collision with any object this asteroid will split into a smaller sub-asteroid and remove this asteroid
   * @param other The GameObject this object collided with
   */
  override def onCollision(other: GameObject): Unit = {
    /**
     * Resize the vector to the new length
     * @param vec The current vector
     * @param newLength The new length
     * @return The new vector
     */
    def resize(vec: (Double, Double), newLength: Double): (Double, Double) = {
      val currLen = math.sqrt(math.pow(vec._1, 2) + math.pow(vec._2, 2))

      (vec._1 / currLen * newLength, vec._2 / currLen * newLength)
    }

    /**
     * Get the direction in which this object should go, and create a random speed
     * @return The movement vector of this object
     */
    def getDir: (Double, Double) = {
      val dir = (x - other.x, y - other.y)
      val speed = (Math.random / 2) + .0001

      resize(dir, speed)
    }

    supervisor ! GameSupervisor.RemoveGameObject(this)
    val newSize = size / 2
    if (newSize != 0) {
      supervisor ! GameSupervisor.AddGameObject(new SSAsteroid((x, y), getDir, newSize, supervisor))
    }
  }
}
