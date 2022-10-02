package nl.rug.aoop
package Client.graphics.frame_addtions

import Client.graphics.AdvancedRotationEngine
import Client.input.InputControl
import Global.Config
import Shared.additional_objects.{DeadPlayer, ExportObject}
import Shared.game_objects.{Asteroid, Bullet, Player}

import io.circe.jawn.decode
import org.slf4j.LoggerFactory

import java.awt.event.{KeyEvent, KeyListener}
import java.awt.{Color, Graphics, Graphics2D}
import javax.swing.JPanel

/**
 * The GraphicsPanel for this Game
 */
class GraphicsPanel extends JPanel with KeyListener {
  addKeyListener(this)
  setFocusable(true)

  /**
   * The logger for this Panel
   */
  private val logger = LoggerFactory.getLogger(classOf[GraphicsPanel])
  /**
   * The ARE context for this
   */
  private val ARE = AdvancedRotationEngine.apply()
  /**
   * The current data this holds
   */
  private var data = List.empty[ExportObject]
  /**
   * The amount of players, necessary for the proper displaying of scores
   */
  private var playerInfoCount = 0

  logger.info("GraphicsPanel created")

  /**
   * Update the data for the panel
   *
   * @param newData The new Data
   */
  def updateData(newData: List[ExportObject]): Unit = {
    data = newData
  }

  override def keyTyped(e: KeyEvent): Unit = {
    // Forcefully overridden by also being a KeyListener
  }

  override def keyPressed(e: KeyEvent): Unit = {
    InputControl.newInput(e.getKeyCode, pressed = true)
  }

  override def keyReleased(e: KeyEvent): Unit = {
    InputControl.newInput(e.getKeyCode, pressed = false)
  }

  override def paint(g: Graphics): Unit = {
    val g2d = g.asInstanceOf[Graphics2D]

    /**
     * Clear the background
     */
    def clear(): Unit = {
      g2d.setColor(Color.BLACK)
      g2d.fillRect(0, 0, getWidth, getHeight)

      playerInfoCount = 0
    }

    /**
     * Decode an EO to a list of Doubles and invoke the passed function
     *
     * @param obj  The object to decode
     * @param func The function to call
     */
    def decodeAndPaint(obj: ExportObject, func: (Graphics2D, List[Double]) => Unit): Unit = {
      val data = decode[List[Double]](obj.data)
      data match {
        case Right(value) => func(g2d, value)
        case Left(err) => logger.error("Failed to decode: {}", err)
      }
    }

    /**
     * Decode a player to a list of string and invoke the passed drawer
     *
     * @param obj  The player
     * @param func The function to call
     */
    def decodeAndPaintPlayer(obj: ExportObject, func: (Graphics2D, List[String]) => Unit): Unit = {
      val data = decode[List[String]](obj.data)
      data match {
        case Right(value) => func(g2d, value)
        case Left(err) => logger.error("Failed to decode player: {}", err)
      }
    }

    clear()
    data.foreach(eo => {
      eo.objectType match {
        case cc if cc == classOf[Asteroid] => decodeAndPaint(eo, paintAsteroid)
        case cc if cc == classOf[Bullet] => decodeAndPaint(eo, paintBullet)
        case cc if cc == classOf[Player] => decodeAndPaintPlayer(eo, paintPlayer)
        case cc if cc == classOf[DeadPlayer] => decodeAndPaintPlayer(eo, paintDeadPlayer)
        case _ => logger.error("Couldn't match: {}", eo)
      }
    })
  }

  /**
   * Paint an Asteroid
   *
   * @param g2d  The Graphics
   * @param data The data
   */
  def paintAsteroid(g2d: Graphics2D, data: List[Double]): Unit = {
    val id = Config.ASTEROID_ARE_PRE + data(2).asInstanceOf[Int]
    val x = ((data.head - (data(2) / 2)) * Config.SCALE_FACTOR).asInstanceOf[Int]
    val y = ((data(1) - (data(2) / 2)) * Config.SCALE_FACTOR).asInstanceOf[Int]

    g2d.drawImage(ARE.getPictureForRotation(id, 0), x, y, null)
  }

  /**
   * Paint a bullet
   *
   * @param g2d  The graphics
   * @param data The data
   */
  def paintBullet(g2d: Graphics2D, data: List[Double]): Unit = {
    val x = ((data.head) * Config.SCALE_FACTOR - (Config.BULLET_WIDTH / 2)).asInstanceOf[Int]
    val y = ((data(1)) * Config.SCALE_FACTOR - (Config.BULLET_HEIGHT / 2)).asInstanceOf[Int]
    val rot = data(2);

    g2d.drawImage(ARE.getPictureForRotation(Config.BULLET_ID, rot), x, y, null)
  }

  /**
   * Paint a player
   *
   * @param g2d  The graphics
   * @param data The data
   */
  def paintPlayer(g2d: Graphics2D, data: List[String]): Unit = {
    /**
     * Draw the player info in the top left corner
     */
    def drawPlayerInfo(): Unit = {
      val strUp = data(5) + " - " + data(4) + "❤️"
      val strLow = "Score: " + data(6) + " ☄️"

      g2d.drawString(strUp, 20, 20 + 3 * playerInfoCount * Config.SCALE_FACTOR)
      g2d.drawString(strLow, 20, 35 + 3 * playerInfoCount * Config.SCALE_FACTOR)

      playerInfoCount += 1
    }

    val x = ((data.head.toDouble - (Config.PLAYER_WIDTH / 2)) * Config.SCALE_FACTOR).asInstanceOf[Int]
    val y = ((data(1).toDouble - (Config.PLAYER_HEIGHT / 2)) * Config.SCALE_FACTOR).asInstanceOf[Int]
    val rot = data(2).toDouble;
    val id = data(3).hashCode

    ARE.generateForId(id, Config.PLAYER_ID)
    g2d.drawImage(ARE.getPictureForRotation(id, rot), x, y, null)

    g2d.setColor(Color.white)
    g2d.drawString(data(5), x - data(5).length, y - 1 * Config.SCALE_FACTOR)
    drawPlayerInfo()
  }

  /**
   * Paint a dead player (score & name)
   *
   * @param g2d  The graphics
   * @param data The data
   */
  def paintDeadPlayer(g2d: Graphics2D, data: List[String]): Unit = {
    g2d.setColor(Color.white)

    val str = data.head + " died with " + data(1) + " ☄️"
    g2d.drawString(str, 20, 20 + 3 * playerInfoCount * Config.SCALE_FACTOR)
    playerInfoCount += 1
  }
}
