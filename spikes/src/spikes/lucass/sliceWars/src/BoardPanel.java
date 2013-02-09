package spikes.lucass.sliceWars.src;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class BoardPanel extends JPanel {

	private Board _board;

	public BoardPanel(Board board) {
		_board = board;
	}

	@Override
	protected void paintComponent(final Graphics g) {
		final Graphics2D g2 = (Graphics2D) g;
		render(g2);
		paintComponents(g2);
		g.dispose();
	}

	private void render(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		Set<BoardCell> boardCells = _board.getBoardCells();
		for (BoardCell boardCell : boardCells) {
			g2.draw(boardCell.polygon);
			g2.drawString("d:"+boardCell.cell.diceCount, boardCell.polygon.xpoints[0], boardCell.polygon.ypoints[0]);
		}
	}
	
	//-------------------------------------------------------
	
	public static void main(String[] args) {
		
		Board board = createBoard();
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.add(new BoardPanel(board));
		frame.setSize(800, 600);
		frame.setVisible(true);
	}

	private static Board createBoard() {
		Board board = new Board();
		Polygon square1 = getSquare1();
		board.addCell(square1);
		
		Polygon square2 = getSquare2();
		board.addCell(square2);
		
		Polygon square3 = getSquare3();
		board.addCell(square3);
		
		board.addCell(square1);
		board.addCell(square2);
		board.addCell(square3);
		board.link(square1,square2);
		board.link(square2,square3);
		return board;
	}
	
	private static Polygon getSquare1() {
		int sideLenght = 100;
		int quarter = sideLenght/4;
		int half = sideLenght/2;
		int x = 0;
		int y = 0;
		
		int[] squareXPoints = new int[]{x     ,x+quarter,x+(quarter*3),x+sideLenght,x+(quarter*3),x+quarter   };
		int[] squareYPoints = new int[]{y+half,y        ,y            ,y+half      ,y+sideLenght ,y+sideLenght};
		int squareNPoints = 6;
		Polygon square = new Polygon(squareXPoints, squareYPoints, squareNPoints);
		return square;
	}
	
	private static Polygon getSquare2() {
		int[] squareXPoints = new int[]{100,200,200,100};
		int[] squareYPoints = new int[]{ 0, 0,100,100};
		int squareNPoints = 4;
		Polygon square = new Polygon(squareXPoints, squareYPoints, squareNPoints);
		return square;
	}
	
	private static Polygon getSquare3() {
		int[] squareXPoints = new int[]{200,300,300,200};
		int[] squareYPoints = new int[]{ 0, 0,100,100};
		int squareNPoints = 4;
		Polygon square = new Polygon(squareXPoints, squareYPoints, squareNPoints);
		return square;
	}
}
