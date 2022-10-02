package nl.rug.aoop
package Client.graphics.frames

import Client.controllers.frame.GameManager

import akka.actor.typed.ActorRef

import java.awt.GridLayout
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JButton, JFrame}

/**
 * The ClientFrame is the main Frame for the game
 */
object ClientFrame {
  /**
   * Create a new ClientFrame
   *
   * @param controller The GameManager Controller to report too
   * @return A new ClientFrame
   */
  def apply(controller: ActorRef[GameManager.Command]): ClientFrame = new ClientFrame(controller)
}

class ClientFrame(controller: ActorRef[GameManager.Command]) extends JFrame("Asteroids") {
  /**
   * The Frame width
   */
  private val frameWidth = 200

  /**
   * The Frame height
   */
  private val frameHeight = 500


  super.setSize(frameWidth, frameHeight)
  super.setLocationRelativeTo(null)
  super.setLayout(new GridLayout(5, 1))

  /**
   * Create a new button with a command to call to the GameManager
   *
   * @param text    The text for the button
   * @param command The command that should be executed
   * @return The JButton
   */
  def createButton(text: String, command: GameManager.Command): JButton = {
    val button = new JButton(text)
    button.addActionListener(e => {
      controller ! command
    })

    button
  }

  super.add(createButton("Single Player Game", GameManager.StartSinglePlayerGame()))
  super.add(createButton("Host Multiplayer Game", GameManager.StartMultiPlayerGame()))
  super.add(createButton("Join Multiplayer Game", GameManager.JoinMultiPlayerGame()))
  super.add(createButton("Spectate Multiplayer Game", GameManager.SpectateMultiPlayerGame()))
  super.add(createButton("Show Leaderboard", GameManager.ShowLeaderboard()))

  super.addWindowListener(new WindowAdapter {
    override def windowClosed(e: WindowEvent): Unit = {
      controller ! GameManager.Stop()
      super.windowClosed(e)
    }
  })

  super.setVisible(true)
}
