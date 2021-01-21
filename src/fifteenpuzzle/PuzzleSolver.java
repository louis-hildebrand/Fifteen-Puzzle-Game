package fifteenpuzzle;

import java.util.ArrayList;
import java.util.LinkedList;

public class PuzzleSolver
{
	public static int NO_CELL = -1;

	/**
	 * Gives the solved state of the puzzle with the given dimensions (with
	 * Puzzle.BLANK_TILE as the blank cell's value)
	 * 
	 * NOTE: If the solved state is ever changed, getHomeCell() and isSolvable()
	 * must be modified accordingly
	 * 
	 * @param numRows The number of rows
	 * @param numCols The number of columns
	 * @return A 2-D int array containing the tile values of a solved puzzle with
	 *         the given number of rows and columns
	 */
	public static int[][] solvedState(int numRows, int numCols)
	{
		int[][] out = new int[numRows][numCols];

		for (int r = 0; r < numRows; r++)
		{
			for (int c = 0; c < numCols; c++)
			{
				if (r == numRows - 1 && c == numCols - 1)
					out[r][c] = Puzzle.BLANK_TILE;
				else
					out[r][c] = r * numCols + c + 1;
			}
		}

		return out;
	}

	// TODO Test efficiency compared to calculating Manhattan priority mod 2
	public static boolean isSolvable(int[][] pattern)
	{
		int[][] copiedValues = PuzzleScrambler.cloneArray(pattern);
		int numRows = copiedValues.length;
		int numCols = copiedValues[0].length;
		int swaps = 0;
		int val;

		for (int r = 0; r < numRows; r++)
		{
			for (int c = 0; c < numCols; c++)
			{
				while ((val = copiedValues[r][c]) != (r * numCols + c + 1))
				{
					// The blank tile is always at the bottom right, so this is the end of the algorithm
					if (val == Puzzle.BLANK_TILE)
						break;

					int targetRow = (val - 1) / numCols;
					int targetCol = (val - 1) % numCols;

					copiedValues[r][c] = copiedValues[targetRow][targetCol];
					copiedValues[targetRow][targetCol] = val;

					swaps++;
				}
			}
		}

		return swaps % 2 == 0;
	}

	public static boolean isSolved(int[][] values)
	{
		int numRows = values.length;
		int numCols = values[0].length;
		int[][] solvedState = PuzzleSolver.solvedState(numRows, numCols);

		for (int r = numRows - 1; r >= 0; r--)
		{
			for (int c = numCols - 1; c >= 0; c--)
			{
				if (values[r][c] != solvedState[r][c])
					return false;
			}
		}

		return true;
	}

	public static int[] getHomeCell(int value, int numRows, int numCols)
	{
		if (value == Puzzle.BLANK_TILE)
			return new int[] { numRows - 1, numCols - 1 };
		else if (value >= numRows * numCols || value <= 0)
			return new int[] { NO_CELL };
		else
		{
			int homeRow = (value - 1) / numCols;
			int homeCol = (value - 1) % numCols;
			return new int[] { homeRow, homeCol };
		}
	}

	public static LinkedList<Move> solve(int[][] values)
	{
		if (!isSolvable(values))
			return null;

		PriorityQueue queue = new PriorityQueue();
		State out;
		queue.enqueue(new State(values, 0, null, null));

		// Dequeue/enqueue until the solved state is reached
		while (true)
		{
			out = queue.dequeue();
			if (isSolved(out.board))
				break;
		}

		// Trace back states to the beginning
		LinkedList<Move> solution = new LinkedList<Move>();
		while (true)
		{
			if (out.prevMove == null)
				break;
			solution.add(0, out.prevMove);
			out = out.prevState;
		}

		return solution;
	}

	protected static void printBoard(int[][] board, int indent)
	{
		int numRows = board.length;
		int numCols = board[0].length;

		for (int r = 0; r < numRows; r++)
		{
			String line = " ".repeat(indent);
			for (int c = 0; c < numCols; c++)
			{
				line += Integer.toString(board[r][c]) + " ";
			}
			System.out.println(line);
		}
	}

	private static class State
	{
		int[][] board;
		int numMoves;
		Move prevMove;
		State prevState;
		int numRows;
		int numCols;
		int priority;

		private State(int[][] board, int numMoves, Move prevMove, State prevState)
		{
			this.board = board;
			this.numMoves = numMoves;
			this.prevMove = prevMove;
			this.prevState = prevState;

			numRows = board.length;
			numCols = board[0].length;
			// priority starts at -1 to indicate that it has not been evaluated yet
			priority = -1;
		}

		private int getPriority()
		{
			if (priority != -1)
				return priority;

			int sum = 0;

			for (int r = 0; r < numRows; r++)
			{
				for (int c = 0; c < numCols; c++)
				{
					if (board[r][c] == Puzzle.BLANK_TILE)
						continue;

					int[] homeCell = getHomeCell(board[r][c], numRows, numCols);
					sum += Math.abs(r - homeCell[0]) + Math.abs(c - homeCell[1]);
				}
			}

			priority = numMoves + sum;
			return priority;
		}

		private Iterable<State> getNeighbors()
		{
			int[] blankCell = PuzzleScrambler.getBlankCell(board);
			ArrayList<State> nextStates = new ArrayList<State>();

			// Add left move (if valid)
			if (blankCell[1] != numCols - 1 && prevMove != Move.RIGHT)
				nextStates.add(new State(PuzzleScrambler.applyMove(board, Move.LEFT), numMoves + 1, Move.LEFT, this));
			// Add right move (if valid)
			if (blankCell[1] != 0 && prevMove != Move.LEFT)
				nextStates.add(new State(PuzzleScrambler.applyMove(board, Move.RIGHT), numMoves + 1, Move.RIGHT, this));
			// Add down move (if valid)
			if (blankCell[0] != 0 && prevMove != Move.UP)
				nextStates.add(new State(PuzzleScrambler.applyMove(board, Move.DOWN), numMoves + 1, Move.DOWN, this));
			// Add up move (if valid)
			if (blankCell[0] != numRows - 1 && prevMove != Move.DOWN)
				nextStates.add(new State(PuzzleScrambler.applyMove(board, Move.UP), numMoves + 1, Move.UP, this));
			return nextStates;
		}
	}

	private static class PriorityQueue
	{
		Node front;
		Node rear;

		private void enqueue(State state)
		{
			if (front == null)
			{
				Node newNode = new Node(state, null, null);
				front = newNode;
				rear = newNode;
			}
			else
			{
				int priority = state.getPriority();

				if (rear.data.getPriority() <= priority)
				{
					Node newNode = new Node(state, rear, null);
					rear.behind = newNode;
					rear = newNode;
				}
				else
				{
					Node currentNode = rear;

					// Starting at the back of the queue, move forward until the appropriate spot
					// to insert the new data is reached
					while (currentNode != null && currentNode.data.getPriority() > priority)
					{
						currentNode = currentNode.before;
					}

					// Insert the new node at the front of the list
					if (currentNode == null)
					{
						Node newNode = new Node(state, null, front);
						front.before = newNode;
						front = newNode;
					}
					// Insert the new node in the middle of the list
					else
					{
						Node newNode = new Node(state, currentNode, currentNode.behind);
						currentNode.behind.before = newNode;
						currentNode.behind = newNode;
					}
				}
			}
		}

		private State dequeue()
		{
			if (front == null)
				return null;

			Node firstNode = front;
			if (front.behind == null)
			{
				front = null;
				rear = null;
			}
			else
			{
				front = front.behind;
				front.before = null;
			}

			// Enqueue neighbors
			for (State state : firstNode.data.getNeighbors())
			{
				enqueue(state);
			}

			return firstNode.data;
		}

		private class Node
		{
			Node before;
			Node behind;
			State data;

			private Node(State data, Node before, Node behind)
			{
				this.data = data;
				this.before = before;
				this.behind = behind;
			}
		}
	}
}
