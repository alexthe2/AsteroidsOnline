package nl.rug.aoop
package Shared.additional_objects

import io.circe.syntax._

/**
 * Represents a DeadPlayer
 *
 * @param name   The name of the player
 * @param points The amount of points when he died
 */
class DeadPlayer(val name: String, val points: String) extends ExportObject(classOf[DeadPlayer], List(name, points).asJson.toString)