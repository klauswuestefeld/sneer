package sneer.bricks.skin.windowboundssetter.impl;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.SwingUtilities;

import sneer.bricks.skin.windowboundssetter.WindowBoundsSetter;

class WindowBoundsSetterImpl implements WindowBoundsSetter{

	private Container _container;

	@Override
	public void defaultContainer(Container container) {
		_container = container;
	}

	@Override public void setBestBounds(Window window) { 																	setBestBounds(window, _container, false, 0); }
	@Override public void setBestBounds(Window window, Container container) { 									setBestBounds(window, container, false, 0); }
	@Override public void setBestBounds(Window window, Container container, int horizontal_limit) {		setBestBounds(window, container, false, horizontal_limit); }
	@Override public void setBestBounds(Window window, boolean resizeHeight) {									setBestBounds(window, _container, resizeHeight, 0); }
	@Override public void setBestBounds(Window window, Container container, boolean resizeHeight) { 	setBestBounds(window, container, resizeHeight, 0); }
	@Override public void setBestBounds(Window window, Container container, boolean resizeHeight , int  horizontalLimit) {
	
		int space = 20;
		
		Point location = defaultLocation(window);
		if(container!=null){
			try{
				location = container.getLocationOnScreen();
			}catch (IllegalComponentStateException e) {
				//ignore, using default location
			}
		}
		
		int y = location.y;
		int x = location.x;

		int width = window.getWidth() + space;
		int widthDif = 0;
		if(horizontalLimit != 0 && width > horizontalLimit) 
			widthDif = width - horizontalLimit;
		
		x = x - width - space + widthDif;
		width = width - widthDif;
		int height = window.getHeight();

		window.setBounds( x, y, width, height);
	}

	private Point defaultLocation(Window window) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); 
		return new Point((int) ((screenSize.getWidth()+window.getWidth())/2), 
								 (int) ((screenSize.getHeight())/4));
	}

	@Override
	public Rectangle unusedArea() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); 
		if(_container==null)
			return defaultUnusedArea(screenSize);

		Window windowAncestor = SwingUtilities.getWindowAncestor(_container);
		if(windowAncestor==null)
			return defaultUnusedArea(screenSize);
		
		Rectangle used = windowAncestor.getBounds();
		return new Rectangle(0, 0, screenSize.width-used.width, (int)used.getMaxY());
	}

	private Rectangle defaultUnusedArea(Dimension screenSize) {
		return new Rectangle(0,0, screenSize.width, screenSize.height);
	}
}