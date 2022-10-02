package nl.rug.aoop
package Client.graphics.frames

import Client.graphics.frame_addtions.{GraphicsPanel, StartBar}
import Client.networking.ClientButler
import Global.Config

import akka.actor.typed.ActorRef

import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.JFrame


/**
 * The GameFrame is the virtual component to the game
 */
object GameFrame {
  /**
   * Create a game frame
   *
   * @param name   The name of the Frame
   * @param butler The Butler this should respond too on closing
   * @return A GameFrame
   */
  def apply(name: String, butler: ActorRef[ClientButler.Command]): GameFrame = new GameFrame(name, butler)
}

class GameFrame(name: String, butler: ActorRef[ClientButler.Command]) extends JFrame(name) {
  private val frameWidth = Config.GAME_WIDTH * Config.SCALE_FACTOR
  private val frameHeight = Config.GAME_HEIGHT * Config.SCALE_FACTOR

  /**
   * The GamePanel
   */
  val panel = new GraphicsPanel

  super.setSize(frameWidth, frameHeight)
  super.setLocationRelativeTo(null)
  super.add(panel)
  super.setJMenuBar(new StartBar(butler))

  super.addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      butler ! ClientButler.SendStop()
      super.windowClosing(e)
    }
  });

  super.setVisible(true)
}
