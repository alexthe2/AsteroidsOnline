package nl.rug.aoop
package Shared.game_objects

import Shared.additional_objects.ExportObject
import Shared.colliders.CircleCollider

import io.circe.syntax._

/**
 * The Asteroid Class, represents an Asteroid GameObject
 *
 * @param startX The startX position
 * @param startY The startY position
 * @param size   The size of the Asteroid (1,2 or 4)
 */
class Asteroid(startX: Double, startY: Double, val size: Int) extends GameObject(startX, startY, 0, CircleCollider(size, 10)) {
  override def serialize: ExportObject = {
    ExportObject(classOf[Asteroid], List(x.toString, y.toString, size.toString).asJson.toString())
  }
}
