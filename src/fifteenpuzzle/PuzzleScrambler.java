package fifteenpuzzle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class PuzzleScrambler
{
	public static char ROW_SEPARATOR = '/';

	public static int[][] generateScrambleState(int numRows, int numCols)
	{
		Random rgen = new Random();
		int[][] values;
		int randIndex;

		do
		{
			values = new int[numRows][numCols];
			ArrayList<Integer> availableValues = new ArrayList<>();
			for (int i = 1; i < numRows * numCols; i++)
				availableValues.add(i);

			for (int r = 0; r < numRows; r++)
			{
				for (int c = 0; c < numCols; c++)
				{
					if (r == numRows - 1 && c == numCols - 1)
					{
						values[r][c] = Puzzle.BLANK_TILE;
					}
					else
					{
						randIndex = rgen.nextInt(availableValues.size());
						values[r][c] = availableValues.get(randIndex);
						availableValues.remove(randIndex);
					}
				}
			}

			// If the puzzle is not solvable, swap the first two tiles
			if (!PuzzleSolver.isSolvable(values))
			{
				int temp = values[0][0];
				values[0][0] = values[0][1];
				values[0][1] = temp;
			}
		} while (PuzzleSolver.isSolved(values));

		return values;
	}

	// TODO Take current state of puzzle into account?
	public static LinkedList<Move> generateScrambleSequence(int numRows, int numCols)
	{
		int[][] scrambleState = generateScrambleState(numRows, numCols);

		LinkedList<Move> solution = PuzzleSolver.solve(scrambleState);
		return reversedSequence(solution);
	}

	public static LinkedList<Move> reversedSequence(LinkedList<Move> sequence)
	{
		LinkedList<Move> out = new LinkedList<Move>();

		for (int i = sequence.size() - 1; i >= 0; i--)
		{
			out.add(sequence.get(i).inverse());
		}

		return out;
	}

	public static LinkedList<Move> stringToSequence(String str)
	{
		String tempStr = str.replace(" ", "").replace("\n", "").toUpperCase();
		LinkedList<Move> sequence = new LinkedList<Move>();

		for (char c : tempStr.toCharArray())
		{
			Move m = Move.parseMove(c);
			if (m == null)
				return null;
			else
				sequence.add(m);
		}

		return sequence;
	}

	public static String sequenceToString(LinkedList<Move> sequence, int perRow)
	{
		if (sequence.size() == 0 || sequence == null)
			return "";

		String str = "";

		for (int i = 0; i < sequence.size(); i++)
		{
			if (i % perRow == 0)
			{
				str += "\n";
			}
			str += sequence.get(i).str + " ";
		}

		return str.substring(1);
	}

	/**
	 * Attempts to interpret the given string as a puzzle with the specified number
	 * of rows and columns. Tiles are separated by spaces and rows are separated by
	 * newlines or by forward slashes.
	 * 
	 * @param str     The string to be parsed
	 * @param numRows The desired number of rows
	 * @param numCols The desired number of columns
	 * @return A 2-D int array with the tile values if the conversion was successful
	 *         and NULL if the conversion failed
	 */
	public static int[][] stringToState(String str, int numRows, int numCols)
	{
		int[][] out = new int[numRows][numCols];
		// Make a list of numbers that haven't been added to the puzzle yet
		ArrayList<String> validNumbers = new ArrayList<String>();
		for (int i = 1; i < numRows * numCols; i++)
		{
			validNumbers.add(Integer.toString(i));
		}
		validNumbers.add(Integer.toString(Puzzle.BLANK_TILE));

		// Treat newline characters the same as '/' (both indicate a row break)
		String tempStr = str.replace('\n', '/');
		String[] rows = tempStr.split("/");
		if (rows.length != numRows)
			return null;
		else
		{
			for (int r = 0; r < numRows; r++)
			{
				String[] cols = rows[r].strip().split(" ");
				if (cols.length != numCols)
					return null;
				else
				{
					for (int c = 0; c < numCols; c++)
					{
						if (validNumbers.contains(cols[c]))
						{
							out[r][c] = Integer.parseInt(cols[c]);
							validNumbers.remove(cols[c]);
						}
						else
							return null;
					}
				}
			}
		}

		return out;
	}

	/**
	 * Converts the given puzzle state to a string. Tiles are separated by spaces
	 * and rows and separated by forward slashes.
	 * 
	 * @param state A 2-D int array with the tile values of the puzzle to be
	 *              converted
	 * @return A string representing the given puzzle state
	 */
	public static String stateToString(int[][] state)
	{
		int numRows = state.length;
		int numCols = state[0].length;
		String out = "";

		for (int r = 0; r < numRows; r++)
		{
			for (int c = 0; c < numCols; c++)
			{
				out += Integer.toString(state[r][c]) + " ";
			}
			out += "/ ";
		}

		// Return everything except the last "/ "
		return out.substring(0, out.length() - 3);
	}

	public static int[][] cloneArray(int[][] values)
	{
		int[][] copiedValues = new int[values.length][values[0].length];
		for (int i = 0; i < values.length; i++)
		{
			copiedValues[i] = values[i].clone();
		}
		return copiedValues;
	}

	public static int[][] applySequence(int[][] currentState, LinkedList<Move> sequence)
	{
		int[][] copiedState = cloneArray(currentState);
		int[] blankCell = getBlankCell(copiedState);

		for (Move m : sequence)
		{
			switch (m)
			{
				case LEFT:
					blankCell = applyMoveLeft(copiedState, blankCell);
					break;
				case RIGHT:
					blankCell = applyMoveRight(copiedState, blankCell);
					break;
				case DOWN:
					blankCell = applyMoveDown(copiedState, blankCell);
					break;
				case UP:
					blankCell = applyMoveUp(copiedState, blankCell);
					break;
			}
		}

		return copiedState;
	}

	public static int[][] applyMove(int[][] currentState, Move move)
	{
		int[][] copiedState = cloneArray(currentState);
		int[] blankCell = getBlankCell(copiedState);

		switch (move)
		{
			case LEFT:
				blankCell = applyMoveLeft(copiedState, blankCell);
				break;
			case RIGHT:
				blankCell = applyMoveRight(copiedState, blankCell);
				break;
			case DOWN:
				blankCell = applyMoveDown(copiedState, blankCell);
				break;
			case UP:
				blankCell = applyMoveUp(copiedState, blankCell);
				break;
		}

		return copiedState;
	}

	public static int[] getBlankCell(int[][] currentState)
	{
		int numRows = currentState.length;
		int numCols = currentState[0].length;

		for (int r = numRows - 1; r >= 0; r--)
		{
			for (int c = numCols - 1; c >= 0; c--)
			{
				if (currentState[r][c] == Puzzle.BLANK_TILE)
					return new int[] { r, c };
			}
		}

		return new int[] { -1 };
	}

	private static int[] applyMoveLeft(int[][] currentState, int[] blankCell)
	{
		int numCols = currentState[0].length;
		int blankRow = blankCell[0];
		int blankCol = blankCell[1];

		if (blankCol == numCols - 1)
			return blankCell;

		currentState[blankRow][blankCol] = currentState[blankRow][blankCol + 1];
		currentState[blankRow][blankCol + 1] = Puzzle.BLANK_TILE;

		return new int[] { blankRow, blankCol + 1 };
	}

	private static int[] applyMoveRight(int[][] currentState, int[] blankCell)
	{
		int blankRow = blankCell[0];
		int blankCol = blankCell[1];

		if (blankCol == 0)
			return blankCell;

		currentState[blankRow][blankCol] = currentState[blankRow][blankCol - 1];
		currentState[blankRow][blankCol - 1] = Puzzle.BLANK_TILE;

		return new int[] { blankRow, blankCol - 1 };
	}

	private static int[] applyMoveDown(int[][] currentState, int[] blankCell)
	{
		int blankRow = blankCell[0];
		int blankCol = blankCell[1];

		if (blankRow == 0)
			return blankCell;

		currentState[blankRow][blankCol] = currentState[blankRow - 1][blankCol];
		currentState[blankRow - 1][blankCol] = Puzzle.BLANK_TILE;

		return new int[] { blankRow - 1, blankCol };
	}

	private static int[] applyMoveUp(int[][] currentState, int[] blankCell)
	{
		int numRows = currentState.length;
		int blankRow = blankCell[0];
		int blankCol = blankCell[1];

		if (blankRow == numRows - 1)
			return blankCell;

		currentState[blankRow][blankCol] = currentState[blankRow + 1][blankCol];
		currentState[blankRow + 1][blankCol] = Puzzle.BLANK_TILE;

		return new int[] { blankRow + 1, blankCol };
	}
}
