package nl.rug.aoop
package Client.graphics

import Global.Config

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The Loader is a Singleton, that whole purpose it to load data
 */
object Loader {
  /**
   * Load into the Advanced Rotation Engine from /graphics
   */
  def loadIntoARE(): Unit = {
    val ARE = AdvancedRotationEngine.apply()

    /**
     * Get an image and rescale to the given size
     *
     * @param path   The path to the image
     * @param width  The width to rescale to
     * @param height The height to rescale to
     * @return The scaled image
     */
    def getImage(path: String, width: Int, height: Int): BufferedImage = {
      val image = ImageIO.read(new File(path)).getScaledInstance(width, height, Image.SCALE_SMOOTH)

      val scaledImage = new BufferedImage(width, height, Image.SCALE_SMOOTH)
      val graphics = scaledImage.getGraphics
      graphics.drawImage(image, 0, 0, null)
      graphics.dispose()

      scaledImage
    }

    /**
     * Load all Asteroids
     */
    def loadAsteroids(): Unit = {
      val asteroidsPre = "graphics/asteroid"
      val asteroidsPost = ".png"

      List(1, 2, 4).foreach(s => {
        val path = asteroidsPre + s + asteroidsPost
        val img = getImage(path, s * Config.SCALE_FACTOR * 2, s * Config.SCALE_FACTOR * 2)
        ARE.addImage(Config.ASTEROID_ARE_PRE + s, img)
      })
    }

    /**
     * Load all Bullets
     */
    def loadBullets(): Unit = {
      val bulletsPre = "graphics/bullet"
      val bulletsPost = ".png"

      List(1, 2, 3).foreach(s => {
        val path = bulletsPre + s + bulletsPost
        val img = getImage(path, (Config.BULLET_WIDTH * Config.SCALE_FACTOR).toInt, (Config.SCALE_FACTOR * Config.BULLET_HEIGHT).toInt)
        ARE.addImage(Config.BULLET_PRE + s, img)
      })
    }

    /**
     * Load the Player
     */
    def loadPlayers(): Unit = {
      val path = "graphics/player.png"
      val img = getImage(path, Config.PLAYER_WIDTH * Config.SCALE_FACTOR, Config.SCALE_FACTOR * Config.PLAYER_HEIGHT)
      ARE.addImage(Config.PLAYER_ID, img)
    }

    loadAsteroids()
    loadBullets()
    loadPlayers()
  }
}
