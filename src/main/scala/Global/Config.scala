package nl.rug.aoop
package Global

import scala.util.Random

/**
 * The Config for the Game
 */
object Config {
  val SCALE_FACTOR = 10

  val ASTEROID_ARE_PRE = 10

  private val BULLETS = List(1, 2, 3)
  private var bulletIdForThisGame = -1

  val BULLET_PRE = 20
  val BULLET_ID: Int = {
    if (bulletIdForThisGame == -1) {
      bulletIdForThisGame = BULLET_PRE + Random.shuffle(BULLETS).head
    }

    bulletIdForThisGame
  }

  val PLAYER_ID = 1
  val BULLET_WIDTH = .4
  val BULLET_HEIGHT = .8
  val PLAYER_WIDTH = 2
  val PLAYER_HEIGHT = 2
  val GAME_WIDTH = 80;
  val GAME_HEIGHT = 60;
  val QUIT_MESSAGE = "game.quit"
}
