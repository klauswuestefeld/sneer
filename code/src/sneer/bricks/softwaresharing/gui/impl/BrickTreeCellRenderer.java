package sneer.bricks.softwaresharing.gui.impl;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

class BrickTreeCellRenderer extends DefaultTreeCellRenderer {

@Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, 
			boolean expanded, boolean leaf, int row, boolean hasFocus_) {
	  
	JLabel result = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus_);
    
    ImageIcon icon = value instanceof AbstractTreeNodeWrapper
    	? ((AbstractTreeNodeWrapper<?>) value).getIcon()
    	: null;
    
    if (icon != null) 
    	result.setIcon(icon);

    if(value instanceof BrickHistoryTreeNode)
    	if(Util.isBrickStagedForExecution(((BrickHistoryTreeNode)value).sourceObject()))
			isStaged(result);
    
    if(value instanceof BrickVersionTreeNode)
    	if((((BrickVersionTreeNode)value).sourceObject()).isChosenForExecution())
			isStaged(result);
    
    return result;
  }

	private void isStaged(JLabel result) {
		result.setForeground(new Color(35,160,35));
	}
}