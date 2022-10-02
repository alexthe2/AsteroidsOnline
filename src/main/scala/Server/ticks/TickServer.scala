package nl.rug.aoop
package Server.ticks

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * The TickServer is an Actor that should be spawned as Singleton in every cluster
 *
 * Every tick it will invoke all methods passed to it as
 */
object TickServer {

  /**
   * Create the tick server as an Actor
   * @param tickAmount It's ticks-per-second
   * @return A behavior
   */
  def apply(tickAmount: Int): Behavior[Command] =
    Behaviors.setup(ctx => new TickServer(ctx, tickAmount))

  /**
   * This TickServer Command
   */
  sealed trait Command

  /**
   * Command to add a new invoke that should be executed per tick
   *
   * @param invoke The invoke to be added
   */
  final case class AddInvoke(actorRef: ActorRef[TickMessage]) extends Command

  /**
   * Command to kill the Server Actor
   */
  final case class KillServer() extends Command

  /**
   * Command to remove an invoke
   *
   * @param invoke The invoke to be removed
   */
  final case class RemoveInvoke(actorRef: ActorRef[TickMessage]) extends Command

  /**
   * Command to start the ticking
   */
  final case class StartServer() extends Command
}

/**
 * The TickServer as an Actor, it will invoke all methods given to it TICK-AMOUNT per second
 * @param context The Cluster Context
 * @param tickAmount The amount of ticks per second
 */
class TickServer(context: ActorContext[TickServer.Command], tickAmount: Int) extends AbstractBehavior[TickServer.Command](context) {

  /**
   * The time between messages
   */
  private val invokeTime = 1000.0f / tickAmount

  /**
   * The invokers, that should be informed about the tick
   */
  private val invokers = ListBuffer.empty[ActorRef[TickMessage]]

  context.log.info2("Created TickServer with {} tps, a tick will be invoked every {} milliseconds", tickAmount, invokeTime)

  import TickServer._

  override def onMessage(msg: TickServer.Command): Behavior[TickServer.Command] = {
    msg match {
      case StartServer() =>
        context.log.info("Starting ticks")
        context.system.scheduler.scheduleAtFixedRate(0 milliseconds, invokeTime milliseconds) {
          () => invokers.foreach(f => f ! TickMessage())
        }
        this

      case KillServer() =>
        context.log.info("Killing Tick Server")
        invokers.clear()
        Behaviors.stopped

      case AddInvoke(actor) =>
        context.log.info("Added {} to tick receivers", actor.path.name)
        invokers += actor
        this

      case RemoveInvoke(actor) =>
        context.log.info("Removed {} from tick receivers", actor.path.name)
        invokers -= actor
        this
    }
  }
}
