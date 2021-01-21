package fifteenpuzzle;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

public abstract class AbstractSession implements ActionListener
{
	private static final double INFO_PANE_WIDTH = 0.3;
	private static final double MAX_PUZZLE_WIDTH = 0.85;
	private static final double MAX_PUZZLE_HEIGHT = 0.8;
	private static final int BORDER_THICKNESS = 25;
	public static final Font NORMAL_FONT = new Font("Dialog", Font.PLAIN, 20);
	public static final Color PLAYING_AREA_PANE_COLOR = Settings.MAIN_COLOR;
	public static final Color INFO_PANE_COLOR = Settings.ACCENT_COLOR;
	private static final Color BORDER_COLOR_WOOD = new Color(115, 64, 18);
	private static final Color BORDER_COLOR_METAL = new Color(145, 145, 145);
	private static final Color INNER_BOARD_COLOR_WOOD = new Color(217, 120, 74);
	private static final Color INNER_BOARD_COLOR_METAL = new Color(239, 239, 239);

	private int numRows;
	private int numCols;
	private SyncObject menuSync;
	private JFrame window;
	private JPanel gamePane;
	private JPanel infoPane;
	private JPanel playingAreaPane;
	private JLabel border;
	private Puzzle puzzle;
	private boolean ended;
	private boolean showWarnings;

	public AbstractSession(int numRows, int numCols, JFrame window, SyncObject sync)
	{
		this.numRows = numRows;
		this.numCols = numCols;
		this.window = window;
		this.menuSync = sync;
		this.showWarnings = true;

		// Create GUI, including puzzle board and (blank) info pane
		try
		{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					initPlayingArea();
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private void initPlayingArea()
	{
		// Pane to contain entire in-game GUI
		gamePane = new JPanel();
		gamePane.setLayout(new BorderLayout());
		window.add(gamePane);

		window.revalidate();

		// Add content area on right side for timer, etc. (leaving some space on the left for the puzzle)
		infoPane = new JPanel();
		infoPane.setPreferredSize(new Dimension((int) (gamePane.getWidth() * INFO_PANE_WIDTH), gamePane.getHeight()));
		infoPane.setLayout(new BorderLayout());
		infoPane.setBackground(INFO_PANE_COLOR);
		gamePane.add(infoPane, BorderLayout.EAST);

		// Create content area for puzzle
		playingAreaPane = new JPanel();
		playingAreaPane.setLayout(new BorderLayout());
		playingAreaPane.setBackground(PLAYING_AREA_PANE_COLOR);
		gamePane.add(playingAreaPane, BorderLayout.CENTER);

		JPanel quitPane = new JPanel();
		quitPane.setLayout(new BoxLayout(quitPane, BoxLayout.LINE_AXIS));
		quitPane.setOpaque(true);
		quitPane.setBackground(PLAYING_AREA_PANE_COLOR);
		playingAreaPane.add(quitPane, BorderLayout.NORTH);

		JButton quitButton = MainMenu.makeButton("ESC", 100, 30, this, NORMAL_FONT);
		quitPane.add(quitButton);

		JPanel puzzlePane = new JPanel();
		puzzlePane.setLayout(new BoxLayout(puzzlePane, BoxLayout.PAGE_AXIS));
		puzzlePane.setOpaque(true);
		puzzlePane.setBackground(PLAYING_AREA_PANE_COLOR);
		playingAreaPane.add(puzzlePane);

		puzzlePane.add(Box.createVerticalGlue());

		border = makePuzzleBorder();
		puzzlePane.add(border);

		puzzlePane.add(Box.createVerticalGlue());

		window.revalidate();
		window.repaint();
	}

	// TODO Account for non-square puzzles
	private JLabel makePuzzleBorder()
	{
		window.revalidate();
		int tileSize = (int) Math.min((MAX_PUZZLE_WIDTH * playingAreaPane.getWidth() - 2 * BORDER_THICKNESS) / numCols,
				(MAX_PUZZLE_HEIGHT * playingAreaPane.getHeight() - 2 * BORDER_THICKNESS) / numRows);
		int borderWidth = numCols * tileSize + 2 * BORDER_THICKNESS;
		int borderHeight = numRows * tileSize + 2 * BORDER_THICKNESS;

		JLabel border = new JLabel();
		border.setAlignmentX(Component.CENTER_ALIGNMENT);
		border.setPreferredSize(new Dimension(borderWidth, borderHeight));
		border.setMinimumSize(border.getPreferredSize());
		border.setMaximumSize(border.getPreferredSize());
		border.setLayout(null);

		String borderPath = Settings.HOME_DIR + "\\" + Settings.STYLE.name + "\\Border.png";
		if (new File(borderPath).exists())
		{
			// Locate and scale border icon
			Image img = new ImageIcon(borderPath).getImage();
			ImageIcon icon = new ImageIcon(img.getScaledInstance(borderWidth, borderHeight, java.awt.Image.SCALE_SMOOTH));
			border.setIcon(icon);
		}
		else
		{
			border.setOpaque(true);
			if (Settings.STYLE == Appearance.WOOD)
				border.setBackground(BORDER_COLOR_WOOD);
			else
				border.setBackground(BORDER_COLOR_METAL);
		}

		return border;
	}

	public Puzzle makePuzzle(JPanel innerBoard)
	{
		try
		{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					innerBoard.setBounds(BORDER_THICKNESS, BORDER_THICKNESS, border.getWidth() - 2 * BORDER_THICKNESS,
							border.getHeight() - 2 * BORDER_THICKNESS);
					switch (Settings.STYLE)
					{
						case WOOD:
							innerBoard.setBackground(INNER_BOARD_COLOR_WOOD);
							break;
						case METAL:
							innerBoard.setBackground(INNER_BOARD_COLOR_METAL);
							break;
					}
					border.add(innerBoard);

					window.revalidate();
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}

		puzzle = new Puzzle(numRows, numCols, innerBoard);
		return puzzle;
	}

	/**
	 * Displays a popup error dialog with the given message. The user can choose to
	 * ignore future error messages, which will block this method from creating
	 * dialogs during the current session.
	 * 
	 * @param errMsg The error message to be displayed
	 */
	protected void showErrorMessage(String errMsg)
	{
		if (!showWarnings)
			return;

		String[] options = { "OK", "Don't show again" };
		int choice = JOptionPane.showOptionDialog(null, errMsg, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, null);
		if (choice == 1)
			showWarnings = false;
	}

	public void sendMove(Move m)
	{
		Thread moveThread = new Thread(new Runnable()
		{
			public void run()
			{
				puzzle.move(m);
			}
		});
		moveThread.start();
	}

	/**
	 * 
	 * 
	 * This method includes a pause, so it must not be called directly from the EDT
	 * 
	 * @param sequence
	 */
	public void sendScramble(LinkedList<Move> sequence)
	{
		if (!puzzle.isScrambleComplete())
			return;

		SyncObject scrambleSync = new SyncObject();

		if (sequence.size() * Settings.SCRAMBLE_SPEED <= Settings.MAX_SCRAMBLE_TIME)
			puzzle.applySequenceAnimated(sequence, scrambleSync);
		else
			puzzle.applyStateInstant(PuzzleScrambler.applySequence(puzzle.currentState(), sequence), scrambleSync);

		// Wait for scramble animation to finish before proceeding
		synchronized (scrambleSync)
		{
			while (!puzzle.isScrambleComplete())
			{
				try
				{
					scrambleSync.wait();
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
		}
	}

	/**
	 * 
	 * 
	 * This method includes a pause, so it must not be called directly from the EDT
	 * 
	 * @param sequence
	 */
	public void sendScramble(int[][] pattern)
	{
		if (!puzzle.isScrambleComplete())
			return;

		SyncObject scrambleSync = new SyncObject();
		puzzle.applyStateInstant(pattern, scrambleSync);

		synchronized (scrambleSync)
		{
			while (!puzzle.isScrambleComplete())
			{
				try
				{
					scrambleSync.wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void quit()
	{
		ended = true;

		// Remove the session GUI from the window
		window.remove(gamePane);

		// TODO Add a way of ending the puzzle animation?
		synchronized (menuSync)
		{
			menuSync.notifyAll();
		}
	}

	protected JPanel getInfoPane()
	{
		return infoPane;
	}

	/**
	 * @return FALSE if the user has chosen to return to the main menu and TRUE
	 *         otherwise
	 */
	public boolean isEnded()
	{
		return ended;
	}
}
