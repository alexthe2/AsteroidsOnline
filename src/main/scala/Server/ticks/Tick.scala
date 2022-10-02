package nl.rug.aoop
package Server.ticks

/**
 * Any Actor that allows to receive ticks must extend this command
 */
trait TickCommand

/**
 * Represents a tick, that was received by the TickServer
 */
final case class TickMessage() extends TickCommand