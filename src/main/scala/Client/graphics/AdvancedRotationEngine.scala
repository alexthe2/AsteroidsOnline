package nl.rug.aoop
package Client.graphics

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

/**
 * The AdvancedRotationEngine allows for caching of known rotations, and therefore fast access to repetitive images
 *
 * It will automatically clean itself every 2 seconds, removing all rotations that were accessed below the median during this time
 *
 * The Engine is more effective as a Singleton and should thus only be accessed via it's companion object
 */
object AdvancedRotationEngine {
  /**
   * The Engine
   */
  private val engine = new AdvancedRotationEngine(2)

  /**
   * Access the Engine
   * @return The AdvancedRotationEngine
   */
  def apply(): AdvancedRotationEngine = engine
}

class AdvancedRotationEngine(cleanupInterval: Int = 2) {
  /**
   * An Actor System to schedule the cleaning up every x seconds
   */
  private val system = ActorSystem("rotationSystem")
  import system.dispatcher
  system.scheduler.schedule(cleanupInterval seconds, cleanupInterval seconds) {
    selfCleanUp()
  }

  /**
   * All pictures that are known to this engine
   */
  val pictures = collection.mutable.Map.empty[Int, BufferedImage]

  /**
   * A buffered map, of all known pictures should only be accessed by taking ownership (via synchronized) over it
   */
  val rotatedPictures = collection.mutable.Map.empty[(Int, Double), (BufferedImage, Int)]

  /**
   * The Logger for the ARE
   */
  private val logger = LoggerFactory.getLogger(classOf[AdvancedRotationEngine])

  /**
   * Add a picture to the Engine
   * @param id The id of the picture
   * @param image The picture
   */
  def addImage(id: Int, image: BufferedImage): Unit = {
    logger.info("Added new picture for {}", id)
    pictures.addOne((id, image))
  }

  /**
   * Get a picture that is rotated
   * @param id The id of the picture
   * @param rotation The rotation
   * @return The rotated pictures
   */
  def getPictureForRotation(id: Int, rotation: Double): BufferedImage = {
    /**
     * Check whether the picture is known and if so, return it and increase it's count
     * @return An Option of the result
     */
    def tryKnown: Option[BufferedImage] = {
      val picture = rotatedPictures.synchronized { rotatedPictures.get((id, rotation)) }
      if (picture.isDefined) {
        rotatedPictures.synchronized {
          rotatedPictures.remove((id, rotation))
          rotatedPictures.addOne(((id, rotation), (picture.get._1, picture.get._2 + 1)))
        }
        return Option.apply(picture.get._1)
      }

      Option.empty
    }

    /**
     * Create a rotation for the picture and add it to the database
     * @return The rotated picture
     */
    def createAndAdd: BufferedImage = {
      val origPicture = pictures.get(id)
      if (origPicture.isEmpty) {
        throw new IllegalArgumentException("Id is unknown")
      }

      val newPicture = rotateImage(rotation, origPicture.get)
      rotatedPictures.synchronized {
        rotatedPictures.addOne(((id, rotation), (newPicture, 1)))
      }

      newPicture
    }

    if (rotation == 0) {
      val pic = pictures.get(id)
      if (pic.isEmpty) {
        throw new IllegalArgumentException("Id is unknown")
      }
      return pic.get
    }

    val known = tryKnown
    if (known.isDefined) {
      return known.get
    }

    createAndAdd
  }

  /**
   * Rotate an image by a given angle
   * @param angle The angle to rotate
   * @param image The image that should be rotated
   * @return The rotated image
   */
  private def rotateImage(angle: Double, image: BufferedImage): BufferedImage = {
    val width = image.getWidth
    val height = image.getHeight
    val typeOfImage = image.getType

    val newImage = new BufferedImage(width, height, typeOfImage)
    val graphics = newImage.createGraphics

    graphics.rotate(angle, width / 2, height / 2)
    graphics.drawImage(image, null, 0, 0)

    newImage
  }

  /**
   * Cleanup the map, by taking the median and removing all pictures under and including the median
   */
  private def selfCleanUp(): Unit = {
    val accesses = ListBuffer.empty[Int]

    rotatedPictures.values.foreach(e => accesses += e._2)
    if (accesses.length > 1) {
      val median = accesses.toList.sortWith(_ < _).drop(accesses.length / 2).head
      rotatedPictures.synchronized {
        rotatedPictures.filter(entry => entry._2._2 > median)
      }
    }
  }

  /**
   *
   * @param hashCode
   * @param id
   *
   * Took strong suggestions from: https://saibaburvr.medium.com/java-applying-colors-of-your-choice-to-images-884dd6d7f12d
   */
  def generateForId(hashCode: Int, id: Int): Unit = {
    if (pictures.contains(hashCode)) {
      return
    }

    val pimage = pictures.get(id)
    if (pimage.isEmpty) {
      throw new IllegalArgumentException("Id is unknown")
    }


    logger.info("Generating new Color scheme for {}", hashCode)
    val randomEngine = new Random(hashCode)

    /**
     * Generate a random Bright Color using a Color Scheme from (https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color)
     *
     * @return The color as a tuple
     */
    def genUntilBright = {
      var r = randomEngine.nextDouble()
      var g = randomEngine.nextDouble()
      var b = randomEngine.nextDouble()

      while ((0.2126 * (r * 255) + 0.7152 * (g * 255) + 0.0722 * (b * 255)) < 50) {
        r = randomEngine.nextDouble()
        g = randomEngine.nextDouble()
        b = randomEngine.nextDouble()
      }

      (r,g,b)
    }

    val color = genUntilBright

    val image = pimage.get
    for (y <- Range(0, image.getHeight())) {
      for (x <- Range(0, image.getWidth())) {
        var pixel = image.getRGB(x, y)

        val alpha = (pixel>>24)&0xff
        val red = (pixel>>16)&0xff
        val green = (pixel>>8)&0xff
        val blue = pixel&0xff

        pixel = (alpha<<24) | ((color._1 * red).toInt <<16) | ((color._2 * green).toInt <<8) | ((color._3 * blue).toInt)
        image.setRGB(x, y, pixel)
      }
    }

    addImage(hashCode, image)
  }
}
