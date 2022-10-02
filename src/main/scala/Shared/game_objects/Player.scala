package nl.rug.aoop
package Shared.game_objects

import Shared.additional_objects.ExportObject
import Shared.colliders.CircleCollider

import io.circe.syntax._

/**
 * The Player GameObject
 *
 * @param startX   The startX position
 * @param startY   The startY position
 * @param playerId The playerID as a String
 * @param lives    The amount of lives the player has
 * @param name     The name of the player
 * @param points   The amount of points the player has
 */
class Player(startX: Double, startY: Double, playerId: String, var lives: Int = 5, val name: String, var points: Double = 0) extends GameObject(startX, startY, 0, CircleCollider(1, 10)) {

  /**
   * increase the amount of the points the player has
   *
   * @param amount The amount to increase by
   */
  def increasePoints(amount: Double): Unit = {
    points += amount
  }

  override def serialize: ExportObject = {
    ExportObject(classOf[Player], List(x.toString, y.toString, rot.toString, playerId, lives.toString, name, points.toInt.toString).asJson.toString)
  }
}
