package nl.rug.aoop
package Server.components

import Server.game_handling.{GameSupervisor, PlayerSupervisor}
import Shared.game_objects.{GameObject, Player}

import akka.actor.typed.ActorRef

import java.util.UUID

/**
 * Represents a server sided Player
 *
 * @param startPos   The start position of this player
 * @param playerUUID The id of this player
 * @param supervisor The game supervisor
 */
class SSPlayer(startPos: (Double, Double), playerUUID: UUID, name: String, gameSupervisor: ActorRef[GameSupervisor.Command], playerSupervisor: ActorRef[PlayerSupervisor.Command]) extends Player(startPos._1, startPos._2, playerUUID.toString, name = name) with SelfMovable {
  /**
   * The last known input to this player
   */
  @volatile
  private var WAD = (0, 0, 0)

  /**
   * Whether there has been a shoot input
   */
  @volatile
  private var shoot = false

  /**
   * Last shoot input, prevent spamming
   */
  private var lastShoot = false

  /**
   * THe current movement vector of this player
   */
  private var moveVec: (Double, Double) = (0, 0)

  /**
   * The maximal speed of the player
   */
  private val MAX_SPEED = .8

  /**
   * Speed decrease on each tick (aka. friction)
   */
  private val SPEED_DECREASE = .97

  /**
   * Movement speed
   */
  private val MOVEMENT_SPEED = .05

  /**
   * Rotation speed
   */
  private val ROTATION_SPEED = .05

  /**
   * Bullet Speed
   */
  private val BULLET_SPEED = 1.4;

  /**
   * How many points you should get per tick you live
   */
  private val LIFETIME_INCREASE = .1

  /**
   * Handle the shooting
   */
  def handleShooting(): Unit = {
    if (!shoot) {
      return
    }

    shoot = false
    val spawnOffset = applyRotation((0, BULLET_SPEED))
    val movement = spawnOffset
    val startPosition = (x + .5 + spawnOffset._1, y + spawnOffset._2)

    gameSupervisor ! GameSupervisor.AddGameObject(new SSBullet(startPosition, movement, rot, this, gameSupervisor))
  }

  /**
   * The new input to the player
   * @param inputList The input as a list of booleans, representing W-A-D-Space
   */
  def changeInput(inputList: List[Int]): Unit = this.synchronized {
    WAD = (inputList.head, inputList(1), inputList(2))
    val shootValue = inputList(3) == 1
    if (lastShoot != shootValue) {
      shoot = shootValue
      lastShoot = shootValue
    }
  }

  /**
   * Apply the current rotation to the input vector
   * @param vec The vector that should be rotated
   * @return The rotated vector
   */
  def applyRotation(vec: (Double, Double)): (Double, Double) = {
    val x = vec._1 * math.cos(rot) - vec._2 * math.sin(rot)
    val y = vec._1 * math.sin(rot) + vec._2 * math.cos(rot)

    (x, y)
  }

  /**
   * Modify the vector given the input
   */
  def modifyVec(): Unit = {

    /**
     * Shorten the vector to the maximal speed
     */
    def shortenVec(): Unit = {
      val currLen = math.sqrt(math.pow(moveVec._1, 2) + math.pow(moveVec._2, 2))
      if (currLen > MAX_SPEED) {
        moveVec = (moveVec._1 / currLen * MAX_SPEED, moveVec._2 / currLen * MAX_SPEED)
      }
    }

    val newDirVec = applyRotation(0, WAD._1 * MOVEMENT_SPEED)
    rotate(WAD._2 * -ROTATION_SPEED + WAD._3 * ROTATION_SPEED)
    moveVec = (moveVec._1 * SPEED_DECREASE + newDirVec._1, moveVec._2 * SPEED_DECREASE + newDirVec._2)

    shortenVec()
  }

  /**
   * Move the player, by modifying the current input
   */
  override def selfMove(): Unit = {
    modifyVec()
    move(moveVec._1, moveVec._2)
    handleShooting()
    increasePoints(LIFETIME_INCREASE)
  }

  override def onCollision(other: GameObject): Unit = {
    if (other.isInstanceOf[SSAsteroid]) {
      lives -= 1
      if (lives == 0) {
        playerSupervisor ! PlayerSupervisor.HandlePlayerDies(this)
      }
    }
  }
}
