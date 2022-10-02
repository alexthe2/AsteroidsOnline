package nl.rug.aoop
package Shared.game_objects

import Shared.additional_objects.ExportObject
import Shared.colliders.CircleCollider

import io.circe.syntax._

/**
 * The Bullet class, represents an Bullet GameObject
 *
 * @param startX   The startX position
 * @param startY   The startY position
 * @param startRot The startRotation
 */
class Bullet(startX: Double, startY: Double, startRot: Double) extends GameObject(startX, startY, startRot, CircleCollider(1, 8)) {
  override def serialize: ExportObject = {
    ExportObject(classOf[Bullet], List(x.toString, y.toString, rot.toString).asJson.toString())
  }
}