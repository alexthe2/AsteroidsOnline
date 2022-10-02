package nl.rug.aoop
package Client.graphics.frame_addtions

import Client.networking.ClientButler

import akka.actor.typed.ActorRef

import javax.swing.{JMenuBar, JMenuItem}

/**
 * The Bar for a Game, invokes Start & Stop with the given Butler
 *
 * @param butler The Butler to invoke on
 */
class StartBar(butler: ActorRef[ClientButler.Command]) extends JMenuBar {
  /**
   * The StartButton, invokes start on press
   */
  class StartButton extends JMenuItem("Start") {
    this.addActionListener(e => butler ! ClientButler.SendStart())
  }

  /**
   * The StopButton, invokes Stop on press
   */
  class StopButton extends JMenuItem("Stop") {
    this.addActionListener(e => butler ! ClientButler.SendStop())
  }

  this.add(new StartButton)
  this.add(new StopButton)
}
