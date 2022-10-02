package nl.rug.aoop
package Shared.game_objects

import Shared.additional_objects.ExportObject
import Shared.colliders.Collider

/**
 * The GameObject is the most abstract definition of an object
 *
 * @param _x       It's startX position
 * @param _y       It's startY position
 * @param _rot     It's start rotation
 * @param collider It's collider
 */
abstract class GameObject(protected var _x: Double, protected var _y: Double, protected var _rot: Double, val collider: Collider) {

  /**
   * Move the GameObject
   *
   * @param x X to move by
   * @param y Y to move by
   */
  def move(x: Double, y: Double): Unit = {
    _x += x
    _y += y
  }

  /**
   * Rotate the GameObject
   *
   * @param angle The angle to rotate by
   */
  def rotate(angle: Double): Unit = {
    _rot = (_rot + angle) % 360.0
  }

  /**
   * Get the X coordinate
   *
   * @return The X coordinate
   */
  def x: Double = _x

  /**
   * Get the Y coordinate
   *
   * @return The Y coordinate
   */
  def y: Double = _y


  /**
   * Get the rotation
   *
   * @return The rotation
   */
  def rot: Double = _rot

  /**
   * Check whether this object is colliding with another object
   *
   * @param other The other object
   * @return Whether they are colliding
   */
  def isColliding(other: GameObject): Boolean = collider.isColliding(this, other)

  /**
   * Convert this GameObject into a Serializable Object
   *
   * @return The serialized object
   */
  def serialize: ExportObject
}