/**
 * MouseController.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.npPairViewer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * this class specifies how the annotation viewer will react to mouse movements/clicks
 */

public class MouseController
    extends MouseAdapter {


public MouseController(MainWindow mw) {
}

@Override
public void mouseClicked(MouseEvent e)
{

  // int x = e.getX(), y = e.getY();

}

/*
  
  * doubleClickAction() : when source node is double clicked, it also selects seleted source node's edges and child nodes
  
	private void doubleClickAction(SourceGraphCell inCell)
	{
		System.out.println("Selected before: " + graph.getSelectionCount());
		graph.addSelectionCells(inCell.getChildren().toArray());
		System.out.println("Double Click: " + inCell.getChildren().toArray().length);
		System.out.println("Selected after: " + graph.getSelectionCount());
	}//method doubleClickAction

	*/
}
