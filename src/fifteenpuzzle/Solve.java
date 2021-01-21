package fifteenpuzzle;

public class Solve
{
	private int numRows;
	private int numCols;
	private long startTime;
	private String scrambleState;
	private int moves;
	private long duration;

	public Solve(int numRows, int numCols, String scrambleState)
	{
		this.numRows = numRows;
		this.numCols = numCols;
		this.startTime = System.currentTimeMillis();
		this.scrambleState = scrambleState;
		this.moves = 0;
		this.duration = (long) 0;
	}

	public void addMove()
	{
		moves++;
	}

	public int getMoves()
	{
		return moves;
	}

	/**
	 * Sets the duration of the solve
	 * 
	 * @param duration The time taken to solve the puzzle (in milliseconds)
	 */
	public void setDuration(long duration)
	{
		this.duration = duration;
	}

	/**
	 * Gets the duration of the solve
	 * 
	 * @return The time taken to solve the puzzle (in milliseconds)
	 */
	public long getDuration()
	{
		return duration;
	}

	public String toString()
	{
		return String.format("%dx%d,%d,%d,%d,%s\n", numRows, numCols, startTime, duration, moves, scrambleState);
	}
}
