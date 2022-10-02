package nl.rug.aoop
package Server.communication

import Client.controllers.frame.GameManager

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.sql.{DriverManager, PreparedStatement}
import java.time.Instant
import scala.collection.mutable.ListBuffer
import scala.util.Using

/**
 * The DBConnector is an Actor that handles the Database connection
 */
object DBConnector {
  /**
   * Create a new Actor
   *
   * @return The DBConn Actor
   */
  def apply(): Behavior[Command] =
    Behaviors.setup[Command](ctx => new DBConnector(ctx))

  /**
   * The command trait for this Actor
   */
  sealed trait Command

  /**
   * Add a new score to the Database
   *
   * @param name  The name of the player
   * @param score The score of the player
   */
  final case class AddScore(name: String, score: Int) extends Command

  /**
   * Callback to the GameManager a 2d list of all scores over players
   *
   * @param ref A reference to the GameManager
   */
  final case class CallbackAll(ref: ActorRef[GameManager.Command]) extends Command
}

class DBConnector (context: ActorContext[DBConnector.Command]) extends AbstractBehavior[DBConnector.Command](context) {

  import DBConnector._

  override def onMessage(msg: DBConnector.Command): Behavior[DBConnector.Command] = {
    msg match{
      case AddScore(name,score) => {
        insert(name, score)
        this
      }

      case CallbackAll(ref) => {
        ref ! GameManager.DisplayLeaderboard(get())
        this
      }
    }
  }

  /**
   * Get all scores from the Database sorted by score
   *
   * @return A List of tuples with name/score/time sorted by score
   */
  def get(): List[(String, String, String)] = {
    val query = "SELECT name,score,time FROM scores ORDER BY score DESC;"
    val buffer = ListBuffer.empty[(String, String, String)]
    Using.Manager { use =>
      val conn = use(DriverManager.getConnection("jdbc:sqlite:db.sqlite"))
      val stmt = use(conn.createStatement())

      val data = stmt.executeQuery(query)
      while (data.next()) {
        buffer += ((data.getString("name"), data.getInt("score").toString, data.getString("time")))
      }
    }

    buffer.toList
  }

  /**
   * Insert a new row into the scores table
   *
   * @param name  The name of the player
   * @param score The score of the player
   */
  def insert(name: String, score: Int): Unit = {
    /**
     * Set the Data for the statement
     *
     * @param curr The statement to set on
     */
    def setData(curr: PreparedStatement): Unit = {
      curr.setString(1, name)
      curr.setInt(2, score)
      curr.setString(3, java.sql.Timestamp.from(Instant.now()).toString)
    }

    val query = "INSERT INTO scores(name,score,time) VALUES(?,?,?)"
    val update = Using.Manager { use =>
      val conn = use(DriverManager.getConnection("jdbc:sqlite:db.sqlite"))
      val stmt = use(conn.prepareStatement(query))

      setData(stmt)
      stmt.executeUpdate()
    }

    if (update.isSuccess) {
      context.log.info("Successfully updated score for {}", name)
    } else {
      context.log.error("Failed to update score")
    }
  }
}