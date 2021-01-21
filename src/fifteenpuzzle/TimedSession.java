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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class TimedSession extends AbstractSession implements KeyListener, ActionListener
{
	private final static int NO_AVG = -1;
	private final static int AVG_ALL_SOLVES = -2;

	private final static int INFO_PANE_BORDER = 15;
	private final static int INFO_SECTION_SPACING = 70;
	private final static int[] AVG_LENS = new int[] { 1, 5, 12, AVG_ALL_SOLVES };
	private final static Font PB_FONT = new Font("Dialog", Font.BOLD, 20);
	private final static Color PB_FOREGROUND = Color.GREEN;
	private static final Color PB_BACKGROUND = new Color(0, 0, 0, 75);

	private JFrame window;
	private Puzzle puzzle;
	private int numRows;
	private int numCols;
	private SolveStatus status;
	private JPanel innerBoard;
	private JLabel msg;
	private JPanel avgStats;
	private JPanel solveHistory;
	private JLabel[][] averages;
	private long[] bestAvgs;
	private ArrayList<Solve> sessionSolves;
	private Solve currentSolve;
	private PuzzleTimer timer;
	private JPanel timerPane;
	private FileWriter fileWriter;

	public TimedSession(int numRows, int numCols, JFrame window, SyncObject sync)
	{
		super(numRows, numCols, window, sync);

		this.numRows = numRows;
		this.numCols = numCols;
		this.window = window;
		this.status = SolveStatus.PRE;

		// Set up file writer for the solve results
		if (Settings.SAVE_SOLVES)
		{
			try
			{
				File solveHistory = new File(Settings.SOLVE_HISTORY);
				boolean newFile = solveHistory.createNewFile();
				this.fileWriter = new FileWriter(solveHistory, true);
				if (newFile)
				{
					System.out.println("Setting up new file...");
					fileWriter.write("Puzzle,Date,Duration,Moves,Scramble\n");
					fileWriter.flush();
				}
			}
			catch (IOException e1)
			{
				showErrorMessage("Error: file writer could not be created for " + Settings.SOLVE_HISTORY + ". Your times will not be saved.");
			}
		}

		// Set up the GUI
		try
		{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					initInfoPane();
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}

		// Create puzzle and add it to the board
		innerBoard = new JPanel();
		innerBoard.setLayout(null);
		innerBoard.addKeyListener(this);
		innerBoard.setFocusable(true);
		puzzle = makePuzzle(innerBoard);
		innerBoard.requestFocusInWindow();
	}

	private void startNewSolve()
	{
		TimedSession thisSession = this;
		Thread startThread = new Thread(new Runnable()
		{
			public void run()
			{
				timer.reset();

				// Scramble puzzle
				status = SolveStatus.SCRAMBLING;
				updateInstructions();
				String scrambleState = scramblePuzzle();
				status = SolveStatus.INSPECT;
				updateInstructions();

				// Create new Solve object to track stats about solve
				currentSolve = new Solve(numRows, numCols, scrambleState);

				// Give user inspection time
				if (Settings.INSPECTION_TIME != 0)
					timer.inspection(thisSession);
				timer.startTimer();
				status = SolveStatus.SOLVING;
				updateInstructions();
			}
		});
		startThread.start();
	}

	private String scramblePuzzle()
	{
		if (numRows * numCols > Settings.MAX_SCRAMBLE_SIZE)
		{
			int[][] scrambleState = PuzzleScrambler.generateScrambleState(numRows, numCols);
			sendScramble(scrambleState);
			return PuzzleScrambler.stateToString(scrambleState);
		}
		else
		{
			LinkedList<Move> scrambleSequence = PuzzleScrambler.generateScrambleSequence(numRows, numCols);
			sendScramble(scrambleSequence);
			return PuzzleScrambler.stateToString(PuzzleScrambler.applySequence(puzzle.currentState(), scrambleSequence));
		}
	}

	private void endSolve()
	{
		timer.halt();
		saveSolve();
		status = SolveStatus.POST;
	}

	private void saveSolve()
	{
		currentSolve.setDuration(timer.getCurrentTime());
		sessionSolves.add(currentSolve);
		updateAverages();

		if (Settings.SAVE_SOLVES && fileWriter != null)
		{
			try
			{
				fileWriter.write(currentSolve.toString());
				fileWriter.flush();
			}
			catch (IOException e)
			{
				showErrorMessage("Error: solve could not be saved");
			}
		}
	}

	private void initInfoPane()
	{
		JPanel infoPane = getInfoPane();

		// Create timer
		timerPane = new JPanel();
		timerPane.setOpaque(false);
		timerPane.setLayout(new BoxLayout(timerPane, BoxLayout.PAGE_AXIS));
		infoPane.add(timerPane, BorderLayout.NORTH);
		timer = new PuzzleTimer(timerPane, INFO_PANE_BORDER);

		// Create space to give instructions
		JPanel lowerPane1 = new JPanel();
		lowerPane1.setOpaque(false);
		lowerPane1.setLayout(new BorderLayout());
		infoPane.add(lowerPane1, BorderLayout.CENTER);

		infoPane.add(Box.createRigidArea(new Dimension(INFO_PANE_BORDER, 10)), BorderLayout.WEST);
		infoPane.add(Box.createRigidArea(new Dimension(INFO_PANE_BORDER, 10)), BorderLayout.EAST);

		// Instructions
		JPanel msgPane = new JPanel();
		msgPane.setOpaque(false);
		msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.PAGE_AXIS));
		lowerPane1.add(msgPane, BorderLayout.NORTH);

		msgPane.add(Box.createRigidArea(new Dimension(10, INFO_SECTION_SPACING)));

		msg = new JLabel("", SwingConstants.CENTER);
		updateInstructions();
		msg.setFont(NORMAL_FONT);
		msg.setAlignmentX(Component.CENTER_ALIGNMENT);
		msg.setHorizontalAlignment(JLabel.CENTER);
		msgPane.add(msg, BorderLayout.NORTH);

		msgPane.add(Box.createRigidArea(new Dimension(10, INFO_SECTION_SPACING)));

		lowerPane1.add(Box.createRigidArea(new Dimension(10, INFO_PANE_BORDER)), BorderLayout.SOUTH);

		// Session stats
		JPanel statistics = new JPanel();
		statistics.setOpaque(false);
		statistics.setLayout(new GridLayout(2, 1));
		lowerPane1.add(statistics, BorderLayout.CENTER);

		// Averages
		avgStats = new JPanel();
		avgStats.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		avgStats.setLayout(new GridLayout(AVG_LENS.length + 1, 3));
		avgStats.setOpaque(false);
		statistics.add(avgStats);
		averages = new JLabel[AVG_LENS.length + 1][3];
		bestAvgs = new long[AVG_LENS.length];
		for (int i = 0; i < AVG_LENS.length; i++)
			bestAvgs[i] = -1;
		initAverages();

		// Full solve history
		sessionSolves = new ArrayList<Solve>();
		solveHistory = new JPanel();
		solveHistory.setBackground(Color.WHITE);
		statistics.add(solveHistory);

		window.revalidate();
		window.repaint();
	}

	private void initAverages()
	{
		JLabel temp;

		// First row
		temp = new JLabel("");
		averages[0][0] = temp;
		avgStats.add(temp);

		temp = generateCenteredLabel("Current");
		averages[0][1] = temp;
		avgStats.add(temp);

		temp = generateCenteredLabel("Best");
		averages[0][2] = temp;
		avgStats.add(temp);

		for (int i = 0; i < AVG_LENS.length; i++)
		{
			// Row header (e.g. "Single," "Ao5," "Session avg.")
			if (AVG_LENS[i] == 1)
				temp = generateCenteredLabel("Single");
			else if (AVG_LENS[i] == AVG_ALL_SOLVES)
				temp = generateCenteredLabel("Session avg.");
			else
				temp = generateCenteredLabel("Ao" + AVG_LENS[i]);
			averages[i + 1][0] = temp;
			avgStats.add(temp);

			// Current score
			temp = generateCenteredLabel("-");
			averages[i + 1][1] = temp;
			avgStats.add(temp);

			// Best score
			if (AVG_LENS[i] == AVG_ALL_SOLVES)
				temp = generateCenteredLabel("");
			else
				temp = generateCenteredLabel("-");
			averages[i + 1][2] = temp;
			avgStats.add(temp);
		}

		avgStats.revalidate();
		avgStats.repaint();
	}

	// TODO Global PB vs session PB
	// TODO Discard fastest/slowest solves when calculating averages
	private void updateAverages()
	{
		long currentAvg;

		for (int i = 0; i < AVG_LENS.length; i++)
		{
			currentAvg = currentAvg(AVG_LENS[i]);
			if (currentAvg == NO_AVG)
				averages[i + 1][1].setText("-");
			else
				averages[i + 1][1].setText(PuzzleTimer.millisToString(currentAvg));

			if (AVG_LENS[i] != AVG_ALL_SOLVES)
			{
				if (currentAvg == NO_AVG)
					averages[i + 1][2].setText("-");
				else if (isBestAvg(AVG_LENS[i], currentAvg))
				{
					bestAvgs[i] = currentAvg;
					averages[i + 1][2].setText(PuzzleTimer.millisToString(currentAvg));
					if (sessionSolves.size() > AVG_LENS[i])
					{
						averages[i + 1][1].setForeground(PB_FOREGROUND);
						averages[i + 1][1].setBackground(PB_BACKGROUND);
						averages[i + 1][1].setFont(PB_FONT);
						averages[i + 1][2].setForeground(PB_FOREGROUND);
						averages[i + 1][2].setBackground(PB_BACKGROUND);
						averages[i + 1][2].setFont(PB_FONT);
					}
				}
				else
				{
					averages[i + 1][1].setForeground(Color.BLACK);
					averages[i + 1][1].setBackground(AbstractSession.INFO_PANE_COLOR);
					averages[i + 1][1].setFont(AbstractSession.NORMAL_FONT);
					averages[i + 1][2].setForeground(Color.BLACK);
					averages[i + 1][2].setBackground(AbstractSession.INFO_PANE_COLOR);
					averages[i + 1][2].setFont(AbstractSession.NORMAL_FONT);
				}
			}
		}

		window.repaint();
	}

	private long currentAvg(int len)
	{
		if (sessionSolves.size() < len)
			return NO_AVG;

		if (len == AVG_ALL_SOLVES)
			len = sessionSolves.size();

		long sum = 0;

		for (int i = 1; i <= len; i++)
		{
			sum += (sessionSolves.get(sessionSolves.size() - i).getDuration());
		}

		return sum / len;
	}

	private boolean isBestAvg(int len, long currentAvg)
	{
		int index = indexOf(AVG_LENS, len);
		long previousBest = bestAvgs[index];

		return (previousBest == NO_AVG && currentAvg != NO_AVG) || (currentAvg != NO_AVG && currentAvg < previousBest);
	}

	private int indexOf(int[] array, int key)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == key)
				return i;
		}
		return -1;
	}

	public void sendMove(Move m)
	{
		Thread moveThread = new Thread(new Runnable()
		{
			public void run()
			{
				boolean successfulMove = puzzle.move(m);
				if (successfulMove)
					currentSolve.addMove();

				// Check if the puzzle was solved (but only if the user actually moved a tile)
				if (successfulMove && puzzle.isSolved())
				{
					endSolve();
					updateInstructions();
				}
			}
		});
		moveThread.start();
	}

	private JLabel generateCenteredLabel(String str)
	{
		JLabel out = new JLabel(str);
		out.setFont(NORMAL_FONT);
		out.setOpaque(true);
		out.setBackground(AbstractSession.INFO_PANE_COLOR);
		out.setAlignmentX(Component.CENTER_ALIGNMENT);
		out.setHorizontalAlignment(JLabel.CENTER);
		out.setVerticalAlignment(JLabel.CENTER);

		return out;
	}

	private void updateInstructions()
	{
		switch (status)
		{
			case PRE:
				msg.setText("<html><center>Press SPACE to begin solving<br>.</center></html>");
				break;
			case SCRAMBLING:
				msg.setText("<html><center>Scrambling<br>...</center></html>");
				break;
			case INSPECT:
				msg.setText("<html><center>Inspection<br>...</center></html>");
				break;
			case SOLVING:
				msg.setText("<html><center>Solving<br>...</center></html>");
				break;
			case POST:
				msg.setText("<html><center>Puzzle solved!<br>Press SPACE to begin the next solve</center></html>");
				break;
			default:
				break;
		}
	}

	public void quit()
	{
		status = SolveStatus.EXIT;
		if (fileWriter != null)
		{
			try
			{
				fileWriter.close();
			}
			catch (IOException e)
			{
			}
		}
		super.quit();
	}

	public SolveStatus getStatus()
	{
		return status;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (puzzle == null)
			return;

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			quit();
		}
		else if (status == SolveStatus.INSPECT)
		{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_LEFT:
					status = SolveStatus.SOLVING;
					sendMove(Move.LEFT);
					break;
				case KeyEvent.VK_RIGHT:
					status = SolveStatus.SOLVING;
					sendMove(Move.RIGHT);
					break;
				case KeyEvent.VK_DOWN:
					status = SolveStatus.SOLVING;
					sendMove(Move.DOWN);
					break;
				case KeyEvent.VK_UP:
					status = SolveStatus.SOLVING;
					sendMove(Move.UP);
					break;
			}
		}
		else if (status == SolveStatus.SOLVING)
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

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (puzzle == null)
			return;

		if ((status == SolveStatus.PRE || status == SolveStatus.POST) && e.getKeyCode() == KeyEvent.VK_SPACE)
			startNewSolve();
	}

	/**
	 * Blank method to satisfy the requirements of the KeyListener interface
	 */
	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "ESC":
				quit();
				break;
		}
	}
}
