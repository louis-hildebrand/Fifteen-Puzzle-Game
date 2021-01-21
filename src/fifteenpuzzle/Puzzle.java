package fifteenpuzzle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.util.LinkedList;

/**
 * Represents a puzzle with dimensions numRows x numCols.
 * 
 * @author louis
 */
public class Puzzle
{
	public static final int BLANK_TILE = 0;
	private static final Color DEFAULT_DARK_WOOD = new Color(143, 76, 6);
	private static final Color DEFAULT_LIGHT_WOOD = new Color(239, 210, 171);
	private static final Color DEFAULT_DARK_METAL = Color.GRAY;
	private static final Color DEFAULT_LIGHT_METAL = Color.LIGHT_GRAY;
	private static final Font TILE_FONT = new Font("Dialog", Font.PLAIN, 32);

	private int numRows;
	private int numCols;
	private int blankRow;
	private int blankCol;
	private int tileSize;
	private Tile[][] tiles;
	private JPanel innerBoard;
	private boolean busy;
	private boolean scrambleComplete;
	private SyncObject moveSync;

	/**
	 * Creates a new puzzle with the given dimensions on the specified board. The
	 * puzzle starts in the solved state.
	 * 
	 * @param numRows    The number of rows in the puzzle
	 * @param numCols    The number of columns in the puzzle
	 * @param innerBoard The board on which the puzzle is to be placed
	 */
	public Puzzle(int numRows, int numCols, JPanel innerBoard)
	{
		// Initialize instance variables
		this.numRows = numRows;
		this.numCols = numCols;
		this.innerBoard = innerBoard;
		tileSize = Math.min(innerBoard.getWidth() / numCols, innerBoard.getHeight() / numRows);
		blankRow = numRows - 1;
		blankCol = numCols - 1;
		busy = false;
		scrambleComplete = true;
		moveSync = new SyncObject();

		tiles = makeTiles();
		innerBoard.revalidate();
		innerBoard.repaint();
	}

	private Tile[][] makeTiles()
	{
		Tile[][] tiles = new Tile[numRows][numCols];
		int[][] solvedState = PuzzleSolver.solvedState(numRows, numCols);

		for (int r = 0; r < numRows; r++)
		{
			for (int c = 0; c < numCols; c++)
			{
				if (r != blankRow || c != blankCol)
					tiles[r][c] = new Tile(solvedState[r][c], c * tileSize, r * tileSize, tileSize, isDark(solvedState[r][c]), innerBoard);
			}
		}

		return tiles;
	}

	private boolean isDark(int value)
	{
		int[] homeCell = PuzzleSolver.getHomeCell(value, numRows, numCols);
		if (homeCell[0] == PuzzleSolver.NO_CELL)
			return false;

		switch (Settings.TILE_PATTERN)
		{
			case LIGHT:
				return false;
			case DARK:
				return true;
			case COLUMNS:
				return homeCell[1] % 2 == 0;
			case ROWS:
				return homeCell[0] % 2 == 0;
			case CHECKER:
				return (homeCell[0] + homeCell[1]) % 2 == 0;
			default:
				return false;
		}
	}

	/**
	 * Sets the puzzle to match the given state. Once complete, the animation
	 * notifies any waiting threads via the scrambleSync object.
	 * 
	 * @param pattern      A 2-D int array with the values of all tiles (and
	 *                     Puzzle.BLANK_TILE for the blank tile)
	 * @param scrambleSync The SyncObject that is to be notified when the animation
	 *                     is complete
	 */
	public void applyStateInstant(int[][] pattern, SyncObject scrambleSync)
	{
		Thread animationThread = new Thread(new Runnable()
		{
			public void run()
			{
				scrambleComplete = false;
				innerBoard.removeAll();
				tiles = new Tile[numRows][numCols];

				for (int r = 0; r < numRows; r++)
				{
					for (int c = 0; c < numCols; c++)
					{
						if (pattern[r][c] != Puzzle.BLANK_TILE)
							tiles[r][c] = new Tile(pattern[r][c], c * tileSize, r * tileSize, tileSize, isDark(pattern[r][c]), innerBoard);
					}
				}

				int[] blankCell = PuzzleScrambler.getBlankCell(pattern);
				blankRow = blankCell[0];
				blankCol = blankCell[1];

				innerBoard.revalidate();
				innerBoard.repaint();

				scrambleComplete = true;
				synchronized (scrambleSync)
				{
					scrambleSync.notifyAll();
				}
			}
		});
		animationThread.start();
	}

	/**
	 * Scrambles the puzzle by applying the given scramble sequence, starting in the
	 * current state. Once complete, the animation notifies any waiting threads via
	 * the scrambleSync object.
	 * 
	 * @param sequence     The list of moves to apply to the puzzle
	 * @param scrambleSync The SyncObject that is to be notified when the animation
	 *                     is complete
	 */
	public void applySequenceAnimated(LinkedList<Move> sequence, SyncObject scrambleSync)
	{
		int oldAnimationTime = Settings.ANIMATION_TIME;
		Settings.ANIMATION_TIME = Settings.SCRAMBLE_SPEED;
		scrambleComplete = false;

		Thread animationThread = new Thread(new Runnable()
		{
			public void run()
			{
				for (Move m : sequence)
				{
					switch (m)
					{
						case LEFT:
							moveLeft();
							break;
						case RIGHT:
							moveRight();
							break;
						case UP:
							moveUp();
							break;
						case DOWN:
							moveDown();
							break;
						default:
							break;
					}
				}

				// Mark the scramble as complete, notify the waiting thread(s) that the scramble is complete, and reset the animation time
				scrambleComplete = true;
				synchronized (scrambleSync)
				{
					scrambleSync.notifyAll();
				}
				Settings.ANIMATION_TIME = oldAnimationTime;
			}
		});
		animationThread.start();
	}

	/**
	 * Tries to slide a tile in the specified direction. The move fails if the
	 * puzzle is in the process of being scrambled, is busy (i.e. another tile is
	 * being moved), or if there is no tile that can be moved in the given
	 * direction.
	 * 
	 * This method includes pauses, so it must not be called directly from the EDT.
	 * 
	 * @param m The direction in which to move
	 * @return TRUE if the move was successful and FALSE otherwise
	 */
	public boolean move(Move m)
	{
		if (!scrambleComplete)
			return false;

		// Wait for previous move to finish
		synchronized (moveSync)
		{
			while (busy)
			{
				try
				{
					moveSync.wait();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}

		switch (m)
		{
			case LEFT:
				return moveLeft();
			case RIGHT:
				return moveRight();
			case UP:
				return moveUp();
			case DOWN:
				return moveDown();
			default:
				return false;
		}
	}

	/**
	 * Tries to slide a tile one square to the right. The move fails if there is no
	 * tile that can be moved to the right.
	 * 
	 * This method includes pauses, so it must not be called directly from the EDT.
	 * 
	 * @return TRUE if the move was successful and FALSE otherwise
	 */
	private boolean moveRight()
	{
		// Block other tiles from moving while this one is moving
		setBusy(true);

		// Move is impossible
		if (blankCol == 0)
		{
			setBusy(false);
			return false;
		}

		// Find the new position of the blank space
		blankCol--;

		// Move the appropriate tile
		tiles[blankRow][blankCol].slideHorizontal(this.tileSize, this);
		tiles[blankRow][blankCol + 1] = this.tiles[blankRow][blankCol];
		tiles[blankRow][blankCol] = null;

		return true;
	}

	/**
	 * Tries to slide a tile one square to the left. The move fails if there is no
	 * tile that can be moved to the left.
	 * 
	 * This method includes pauses, so it must not be called directly from the EDT.
	 * 
	 * @return TRUE if the move was successful and FALSE otherwise
	 */
	private boolean moveLeft()
	{
		// Block other tiles from moving while this one is moving
		setBusy(true);

		// Move is impossible
		if (blankCol == this.numCols - 1)
		{
			setBusy(false);
			return false;
		}

		// Find the new position of the blank space
		blankCol++;

		// Move the appropriate tile
		tiles[blankRow][blankCol].slideHorizontal(-this.tileSize, this);
		tiles[blankRow][blankCol - 1] = this.tiles[blankRow][blankCol];
		tiles[blankRow][blankCol] = null;

		return true;
	}

	/**
	 * Tries to slide a tile one square up. The move fails if there is no tile that
	 * can be moved up.
	 * 
	 * This method includes pauses, so it must not be called directly from the EDT.
	 * 
	 * @return TRUE if the move was successful and FALSE otherwise
	 */
	private boolean moveUp()
	{
		// Block other tiles from moving while this one is moving
		setBusy(true);

		// Move is impossible
		if (blankRow == numRows - 1)
		{
			setBusy(false);
			return false;
		}

		// Find the new position of the blank space
		blankRow++;

		// Move the appropriate tile
		tiles[blankRow][blankCol].slideVertical(this.tileSize, this);
		tiles[blankRow - 1][blankCol] = this.tiles[blankRow][blankCol];
		tiles[blankRow][blankCol] = null;

		return true;
	}

	/**
	 * Tries to slide a tile one square down. The move fails if there is no tile
	 * that can be moved down.
	 * 
	 * This method includes pauses, so it must not be called directly from the EDT.
	 * 
	 * @return TRUE if the move was successful and FALSE otherwise
	 */
	private boolean moveDown()
	{
		// Block other tiles from moving while this one is moving
		setBusy(true);

		// Move is impossible
		if (blankRow == 0)
		{
			setBusy(false);
			return false;
		}

		// Find the new position of the blank space
		blankRow--;

		// Move the appropriate tile
		tiles[blankRow][blankCol].slideVertical(-this.tileSize, this);
		tiles[blankRow + 1][blankCol] = this.tiles[blankRow][blankCol];
		tiles[blankRow][blankCol] = null;

		return true;
	}

	/**
	 * Sets the state of the puzzle (busy or not busy).
	 * 
	 * The puzzle should be marked busy while a tile is in motion. As long as the
	 * puzzle is marked busy, it will ignore any attempts to move pieces.
	 * 
	 * @param busy Whether or not the puzzle is busy
	 */
	protected void setBusy(boolean busy)
	{
		this.busy = busy;
	}

	/**
	 * Checks if the puzzle is solved (as defined by PuzzleSolver.solvedState() )
	 * 
	 * @return TRUE if the puzzle is solved and FALSE otherwise
	 */
	public boolean isSolved()
	{
		int[][] solvedState = PuzzleSolver.solvedState(numRows, numCols);

		for (int r = numRows - 1; r >= 0; r--)
		{
			for (int c = numCols - 1; c >= 0; c--)
			{
				if (tiles[r][c] == null && solvedState[r][c] != BLANK_TILE)
					return false;
				if (tiles[r][c] != null && tiles[r][c].value != solvedState[r][c])
					return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the scramble animation is complete and the puzzle is ready to be
	 * solved
	 * 
	 * @return TRUE if the scramble animation is complete and FALSE otherwise
	 */
	public boolean isScrambleComplete()
	{
		return scrambleComplete;
	}

	public int[][] currentState()
	{
		int[][] out = new int[numRows][numCols];

		for (int r = 0; r < numRows; r++)
		{
			for (int c = 0; c < numCols; c++)
			{
				if (tiles[r][c] == null)
					out[r][c] = BLANK_TILE;
				else
					out[r][c] = tiles[r][c].value;
			}
		}

		return out;
	}

	protected SyncObject getMoveSync()
	{
		return moveSync;
	}

	/**
	 * An individual tile in the puzzle
	 * 
	 * @author louis
	 */
	private class Tile
	{
		private int value;
		private JLabel image;
		private JPanel board;

		/**
		 * Creates a tile with the specified value, initial position, and size, and
		 * places it on the given board.
		 * 
		 * @param value The number on the tile
		 * @param x     The initial x-coordinate of the tile's upper-left corner (in
		 *              pixels)
		 * @param y     The initial y-coordinate of the tile's upper-left corner (in
		 *              pixels)
		 * @param size  The side length of the tile (in pixels)
		 * @param board The board on which the tile should be placed
		 */
		private Tile(int value, int x, int y, int size, boolean dark, JPanel board)
		{
			// Copy arguments to instance variables
			this.value = value;
			this.board = board;

			// Draw tile and add it to the board
			this.image = drawImage(x, y, size, dark);
			this.board.add(this.image);
		}

		/**
		 * Generates the square JLabel that represents the tile onscreen. The tile
		 * images are taken from the Settings.ICON_DIR\Settings.STYLE\color directory.
		 * If the file is not found, the puzzle uses a simple coloured JLabel.
		 * 
		 * @param x    The x-coordinate of the tile's upper-left corner (in pixels)
		 * @param y    The y-coordinate of the tile's upper-left corner (in pixels)
		 * @param size The side length of the tile (in pixels)
		 * @return The JLabel used to represent the tile
		 */
		private JLabel drawImage(int x, int y, int size, boolean dark)
		{
			JLabel image;

			String iconFileName = Settings.getTilePath(value, dark);

			// Try to use the icons in AppData
			if (new File(iconFileName).exists())
			{
				// Locate and scale tile icon
				Image img = new ImageIcon(iconFileName).getImage();
				ImageIcon icon = new ImageIcon(img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH));

				// Create the JLabel with the scaled icon
				image = new JLabel();
				image.setBounds(new Rectangle(x, y, size, size));
				image.setIcon(icon);
			}
			// If the icons are not found, use a default image
			else
			{
				image = new JLabel(Integer.toString(value), SwingConstants.CENTER);
				image.setBounds(new Rectangle(x + 1, y + 1, size - 2, size - 2));
				image.setFont(TILE_FONT);
				image.setOpaque(true);
				// Choose the background color based on the style and whether the tile is light or dark
				if (Settings.STYLE == Appearance.WOOD)
				{
					if (dark)
						image.setBackground(DEFAULT_DARK_WOOD);
					else
						image.setBackground(DEFAULT_LIGHT_WOOD);
				}
				else
				{
					if (dark)
						image.setBackground(DEFAULT_DARK_METAL);
					else
						image.setBackground(DEFAULT_LIGHT_METAL);
				}
			}

			return image;
		}

		/**
		 * Moves the tile the specified number of pixels horizontally. Positive values
		 * correspond to moving to the right, while negative values correspond to moving
		 * to the left.
		 * 
		 * This method includes a pause, so it must not be called directly from the EDT.
		 * 
		 * @param dx     The number of pixels to move
		 * @param puzzle The puzzle which the tile is part of
		 */
		private void slideHorizontal(int dx, Puzzle puzzle)
		{
			SyncObject sync = new SyncObject();
			int sgn = (dx > 0 ? 1 : -1);
			int numSteps = Math.abs(dx) / Settings.STEP_SIZE;
			long dt = Settings.ANIMATION_TIME / numSteps;
			int finalX = image.getX() + dx;

			if (Math.abs(dx) < Settings.STEP_SIZE / 2 || dt == 0)
				image.setLocation(finalX, image.getY());
			else
			{
				synchronized (sync)
				{
					for (int i = 0; i < numSteps; i++)
					{
						image.setLocation(image.getX() + sgn * Settings.STEP_SIZE, image.getY());
						innerBoard.repaint();

						try
						{
							sync.wait(dt);
						}
						catch (InterruptedException e)
						{
							break;
						}
					}
				}
				image.setLocation(finalX, image.getY());
				innerBoard.repaint();

				puzzle.setBusy(false);
				synchronized (puzzle.getMoveSync())
				{
					puzzle.getMoveSync().notify();
				}
			}
		}

		/**
		 * Moves the tile the specified number of pixels vertically. Positive values
		 * correspond to moving up, while negative values correspond to moving down.
		 * 
		 * This method includes a pause, so it must not be called directly from the EDT.
		 * 
		 * @param dy     The number of pixels to move
		 * @param puzzle The puzzle which the tile is part of
		 */
		private void slideVertical(int dy, Puzzle puzzle)
		{
			int sgn = (dy > 0 ? 1 : -1);
			int numSteps = Math.abs(dy) / Settings.STEP_SIZE;
			long dt = Math.max(Settings.ANIMATION_TIME / numSteps, 1);
			int finalY = image.getY() - dy;

			if (Math.abs(dy) < Settings.STEP_SIZE / 2 || dt == 0)
				image.setLocation(image.getX(), finalY);
			else
			{
				SyncObject sync = new SyncObject();
				synchronized (sync)
				{
					for (int i = 0; i < numSteps; i++)
					{
						image.setLocation(image.getX(), image.getY() - sgn * Settings.STEP_SIZE);
						innerBoard.repaint();

						try
						{
							sync.wait(dt);
						}
						catch (InterruptedException e)
						{
							break;
						}
					}
				}
				image.setLocation(image.getX(), finalY);
				innerBoard.repaint();

				puzzle.setBusy(false);
				synchronized (puzzle.getMoveSync())
				{
					puzzle.getMoveSync().notify();
				}
			}
		}
	}
}
