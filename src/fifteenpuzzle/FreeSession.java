package fifteenpuzzle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Game mode with no timer, where the user can freely play with the puzzle or
 * enter a scramble of their choice
 * 
 * @author louis
 */
public class FreeSession extends AbstractSession implements KeyListener, ActionListener
{
	private static int INFO_PANE_BORDER = 20;
	private static int INFO_PANE_SPACE = 10;
	private static Color TEXT_AREA_COLOR = Settings.MAIN_COLOR;
	private static Font INFO_FONT = NORMAL_FONT;

	private JPanel innerBoard;
	private JTextArea userScramble;
	private JTextArea solveResultText;
	private int numRows;
	private int numCols;
	private Puzzle puzzle;

	/**
	 * Sets up the session GUI using the superclass constructor and starts the game.
	 * The puzzle is left in the solved state at first, with an option to scramble
	 * it at any time
	 * 
	 * @param numRows The number of rows for the puzzle
	 * @param numCols The number of columns for the puzzle
	 * @param window  The JFrame on which the GUI is to be displayed
	 * @param sync    The object on which the calling thread in the main menu is
	 *                synchronized (used to notify the main menu that the user would
	 *                like to end the session)
	 */
	public FreeSession(int numRows, int numCols, JFrame window, SyncObject sync)
	{
		super(numRows, numCols, window, sync);

		this.numRows = numRows;
		this.numCols = numCols;

		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				initInfoPane();
			}
		});
		initPuzzle();
	}

	private void initInfoPane()
	{
		JPanel infoPane = getInfoPane();
		infoPane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_BORDER)), BorderLayout.NORTH);
		infoPane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_BORDER)), BorderLayout.SOUTH);
		infoPane.add(Box.createRigidArea(new Dimension(INFO_PANE_BORDER, 10)), BorderLayout.WEST);
		infoPane.add(Box.createRigidArea(new Dimension(INFO_PANE_BORDER, 10)), BorderLayout.EAST);

		JPanel innerInfoPane = new JPanel();
		innerInfoPane.setOpaque(false);
		innerInfoPane.setLayout(new GridLayout(2, 1));
		infoPane.add(innerInfoPane);

		// Scramble
		JPanel scramblePane = new JPanel();
		scramblePane.setLayout(new BoxLayout(scramblePane, BoxLayout.PAGE_AXIS));
		scramblePane.setOpaque(false);
		innerInfoPane.add(scramblePane);

		scramblePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		JButton scrambleButton = new JButton("Scramble");
		scrambleButton.setActionCommand("SCRAMBLE");
		scrambleButton.addActionListener(this);
		scrambleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		scrambleButton.setFont(AbstractSession.NORMAL_FONT);
		scramblePane.add(scrambleButton);

		scramblePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		userScramble = new JTextArea();
		userScramble.setBackground(TEXT_AREA_COLOR);
		userScramble.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		userScramble.setFont(INFO_FONT);
		scramblePane.add(userScramble);

		scramblePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		// Solve
		JPanel solvePane = new JPanel();
		solvePane.setLayout(new BoxLayout(solvePane, BoxLayout.PAGE_AXIS));
		solvePane.setOpaque(false);
		innerInfoPane.add(solvePane);

		solvePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		JButton solveButton = new JButton("Solve");
		solveButton.setActionCommand("SOLVE");
		solveButton.addActionListener(this);
		solveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		solveButton.setFont(AbstractSession.NORMAL_FONT);
		solvePane.add(solveButton);

		solvePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		infoPane.revalidate();
		solveResultText = new JTextArea();
		if (numRows * numCols <= Settings.MAX_SCRAMBLE_SIZE)
			solveResultText.setText("The solution will be displayed here");
		solveResultText.setEditable(false);
		solveResultText.setFont(INFO_FONT);
		solveResultText.setBackground(TEXT_AREA_COLOR);
		solveResultText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		solvePane.add(solveResultText);

		solvePane.add(Box.createRigidArea(new Dimension(10, INFO_PANE_SPACE)));

		infoPane.revalidate();
	}

	private void initPuzzle()
	{
		innerBoard = new JPanel();
		innerBoard.setLayout(null);
		innerBoard.addKeyListener(this);
		innerBoard.setFocusable(true);
		puzzle = makePuzzle(innerBoard);
		innerBoard.requestFocusInWindow();
	}

	// TODO Find way to split large scramble state entries without disrupting the conversion?
	private void scramblePuzzle()
	{
		// If the user enters their own scramble, use that
		String userScrambleText = userScramble.getText();
		if (!userScrambleText.equals(""))
		{
			LinkedList<Move> scrambleSequence = PuzzleScrambler.stringToSequence(userScrambleText);
			// Valid user sequence
			if (scrambleSequence != null)
			{
				sendScramble(scrambleSequence);
				userScramble.setText("");
			}
			else
			{
				int[][] scrambleState = PuzzleScrambler.stringToState(userScrambleText, numRows, numCols);
				// Valid state
				if (scrambleState != null)
				{
					sendScramble(scrambleState);
					userScramble.setText("");
				}
				// Invalid scramble text
				else
					showErrorMessage("This entry was not recognized as a valid scramble");
			}
		}
		// Otherwise, generate a scramble automatically
		else
		{
			// Large puzzle: just use instant scramble
			if (numRows * numCols > Settings.MAX_SCRAMBLE_SIZE)
				sendScramble(PuzzleScrambler.generateScrambleState(numRows, numCols));
			// Small enough puzzle: try animated scramble
			else
				sendScramble(PuzzleScrambler.generateScrambleSequence(numRows, numCols));
		}
	}

	// TODO Add solve feature
	/**
	 * 
	 * This method includes a pause, so it must not be called directly from the EDT
	 */
	private void solvePuzzle()
	{
		solveResultText.setText("Solving...");

		if (numRows * numCols > Settings.MAX_SCRAMBLE_SIZE)
		{
			sendScramble(PuzzleSolver.solvedState(numRows, numCols));
			solveResultText.setText("");
		}
		else
		{
			LinkedList<Move> solution = PuzzleSolver.solve(puzzle.currentState());
			solveResultText.setText(PuzzleScrambler.sequenceToString(solution, 21));
			sendScramble(solution);
		}
	}

	/**
	 * Event handling for buttons
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "SCRAMBLE":
				Thread scrambleThread = new Thread(new Runnable()
				{
					public void run()
					{
						scramblePuzzle();
					}
				});
				scrambleThread.start();
				innerBoard.requestFocusInWindow();
				break;
			case "SOLVE":
				Thread solveThread = new Thread(new Runnable()
				{
					public void run()
					{
						solvePuzzle();
					}
				});
				solveThread.start();
				innerBoard.requestFocusInWindow();
				break;
			case "ESC":
				quit();
				break;
		}
	}

	/**
	 * Event handling for key presses that should be responded to immediately, i.e.
	 * ESC (return to main menu) and ARROW KEYS (move pieces on puzzle)
	 */
	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			quit();
		}
		else
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_UP:
					sendMove(Move.UP);
					break;
				case KeyEvent.VK_DOWN:
					sendMove(Move.DOWN);
					break;
				case KeyEvent.VK_RIGHT:
					sendMove(Move.RIGHT);
					break;
				case KeyEvent.VK_LEFT:
					sendMove(Move.LEFT);
					break;
			}
		}
	}

	/**
	 * Empty method to satisfy the requirements of the KeyListener interface
	 */
	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	/**
	 * Empty method to satisfy the requirements of the KeyListener interface
	 */
	@Override
	public void keyTyped(KeyEvent e)
	{
	}
}
