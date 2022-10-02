package nl.rug.aoop
package Client.graphics.frames

import javax.swing.table.DefaultTableModel
import javax.swing.{JFrame, JScrollPane, JTable}

/**
 * Create a new Frame for the Leaderboard
 *
 * @param values The values for the leaderboard
 */
object LeaderboardFrame {
  def apply(values: List[(String, String, String)]): LeaderboardFrame = new LeaderboardFrame(values)
}

class LeaderboardFrame(values: List[(String, String, String)]) extends JFrame("Leaderboard") {

  /**
   * The TableModel for this frame
   */
  private val tableModel = new DefaultTableModel()


  /**
   * The frame width
   */
  private val frameWidth = 300

  /**
   * The frame height
   */
  private val frameHeight = 300

  /**
   * Fill the model with the information given
   */
  def fillModel(): Unit = {
    tableModel.addColumn("Name")
    tableModel.addColumn("Points")
    tableModel.addColumn("Date")

    values.foreach { case (a, b, c) => {
      tableModel.addRow(Array[AnyRef](a, b, c))
    }
    }
  }


  fillModel()
  super.setSize(frameWidth, frameHeight)
  super.setLocationRelativeTo(null)
  super.add(new JScrollPane(new JTable(tableModel)))
  super.setVisible(true)
}
