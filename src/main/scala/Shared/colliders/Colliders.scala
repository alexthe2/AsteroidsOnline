package nl.rug.aoop
package Shared.colliders

import Shared.game_objects.GameObject

import breeze.linalg.{DenseMatrix, DenseVector}

import scala.collection.mutable.ListBuffer

/**
 * Represents a collider
 */
sealed abstract class Collider {
  /**
   * Get the collider as a list of polygons who is circular connected form a collider about
   *
   * @return
   */
  def getAsPolygons(parent: GameObject): List[(Double, Double)]

  /**
   * The Bounding Box around the object for fast CD
   * @return
   */
  def getBoundingBox(parent: GameObject): ((Double, Double), (Double, Double))

  /**
   * Given another collider check whether this object collides with the other object
   * @param parent The parent GameObject this is attached to
   * @param other The other collider
   * @return Whether there is a collision
   *
   * TODO: Add more CD then just BB
   */
  def isColliding(parent: GameObject, other: GameObject): Boolean = {
    def checkBoundingBoxes = {
      val thisBB = getBoundingBox(parent)
      val otherBB = other.collider.getBoundingBox(other)

      (thisBB._1._1 < otherBB._2._1 && thisBB._2._1 > otherBB._1._1 // x - collision
      && thisBB._1._2 < otherBB._2._2 && thisBB._2._2 > otherBB._1._2) // y - collision
    }

    checkBoundingBoxes
  }

  /**
   * Small Helper functions to convert a vector to a tuple
   * @param vec The Vector
   * @return Tuple representation of the vector
   */
  def toTuple(vec: DenseVector[Double]): (Double, Double) = (vec(0), vec(1))
}

/**
 * A rectangle Collider
 * @param width The width of the rectangle
 * @param height The height of the rectangle
 */
case class RectangleCollider(width: Double, height: Double) extends Collider {
  /**
   * Using matrix multiplication and the transformation matrix we rotate the rectangle by a given area
   * @param parent The parent GameObject this is attached to
   * @return A list of points, that if connected circular give the rectangle as a polygon
   */
  override def getAsPolygons(parent: GameObject): List[(Double, Double)] = {
    val center = DenseVector(parent.x, parent.y)
    val v1 = DenseVector(Math.cos(parent.rot), Math.sin(parent.rot))
    val v2 = DenseVector(-v1(1), v1(0))

    v1 *= width / 2.0
    v2 *= height / 2.0

    type localOp = (DenseVector[Double], DenseVector[Double]) => DenseVector[Double]
    def calc(op1: localOp, op2: localOp) : DenseVector[Double] = op2 (op1 (center, v1), v2)

    List(toTuple(calc(_-_, _-_)), toTuple(calc(_+_, _-_)), toTuple(calc(_+_, _+_)), toTuple(calc(_-_, _+_)))
  }

  override def getBoundingBox(parent: GameObject): ((Double, Double), (Double, Double)) = {
    val asPoly = getAsPolygons(parent)
    (asPoly.head, asPoly(2))
  }
}

/**
 * A circular collider
 * @param radius The radius of the collider
 * @param resolution How many points should represent the circle, the more points the more accurate the circle is
 */
case class CircleCollider(radius: Double, resolution: Int) extends Collider {
  /**
   * By rotating a line every time 1/resolution degrees, we can get points that describe our circle
   * @param parent The parent GameObject attached to it
   * @return A list of points, that if connected circular give the circle as a polygon
   */
  override def getAsPolygons(parent: GameObject): List[(Double, Double)] = {
    var v = DenseVector(parent.x, parent.y + radius)
    val points = ListBuffer.empty[(Double, Double)]
    val angle = 360.0 / resolution
    val rotMatrix = DenseMatrix((Math.cos(angle), -Math.sin(angle)), (Math.sin(angle), Math.cos(angle)))

    for(_ <- 1 to resolution) {
      points += toTuple(v)
      v = rotMatrix * v
    }

    List.from(points)
  }

  override def getBoundingBox(parent: GameObject): ((Double, Double), (Double, Double)) = ((parent.x - radius, parent.y - radius), (parent.x + radius, parent.y + radius))
}