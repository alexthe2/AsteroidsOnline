package nl.rug.aoop
package Server.components

import Global.Config
import Shared.game_objects.GameObject

/**
 * Trait for SelfMovable objects, must implement the function selfMove and onCollision
 */
trait SelfMovable extends GameObject {

  /**
   * Represents a move of this object, given a tick
   */
  def selfMove(): Unit

  /**
   * If the object is out of boundaries, move it to the other side of the boundary
   */
  def boundaryMove(): Unit = {
    if (_x < 0) _x = Config.GAME_WIDTH
    if (_y < 0) _y = Config.GAME_HEIGHT

    if (_x > Config.GAME_WIDTH) _x = 0
    if (_y > Config.GAME_HEIGHT) _y = 0
  }

  /**
   * Represents what should happen on collision with another object
   * @param other The GameObject this object collided with
   */
  def onCollision(other: GameObject): Unit
}
