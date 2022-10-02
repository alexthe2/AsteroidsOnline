package nl.rug.aoop
package Server.components

import Server.game_handling.GameSupervisor
import Shared.game_objects.{Asteroid, Bullet, GameObject, Player}

import akka.actor.typed.ActorRef

/**
 * Represents a ServerSided Bullet that is thread-safe
 *
 * @param startPos   The starting position
 * @param moveVec    The self-move
 * @param supervisor The game supervisor
 */
class SSBullet(startPos: (Double, Double), val moveVec: (Double, Double), val startRot: Double, playerOrigin: Player, supervisor: ActorRef[GameSupervisor.Command]) extends Bullet(startPos._1, startPos._2, startRot) with SelfMovable {

  /**
   * Lifespan of this bullet in ticks
   */
  private var LIFESPAN = 40

  /**
   * Reduce the lifetime of this bullet
   *
   * At the end of it's lifetime, destroy it
   */
  def disintegrate(): Unit = this.synchronized {
    LIFESPAN -= 1
    if (LIFESPAN <= 0) {
      supervisor ! GameSupervisor.RemoveGameObject(this)
    }
  }

  override def selfMove(): Unit = {
    move(moveVec._1, moveVec._2)
    disintegrate()
  }

  /**
   * On collision with any object this bullet will destroy itself
   * @param other The GameObject this object collided with
   */
  override def onCollision(other: GameObject): Unit = {
    if (other.isInstanceOf[Player]) {
      return
    }

    if (other.isInstanceOf[Asteroid]) {
      playerOrigin.increasePoints(other.asInstanceOf[SSAsteroid].score)
    }

    supervisor ! GameSupervisor.RemoveGameObject(this)
  }
}
