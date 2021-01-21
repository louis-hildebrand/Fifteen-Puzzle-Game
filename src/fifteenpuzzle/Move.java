package fifteenpuzzle;

/**
 * All possible moves that can be done on the puzzle
 * 
 * @author louis
 */
public enum Move
{
	LEFT ("L"),
	RIGHT ("R"),
	UP ("U"),
	DOWN ("D");

	public String str;

	Move(String str)
	{
		this.str = str;
	}

	public Move inverse()
	{
		switch (this)
		{
			case LEFT:
				return RIGHT;
			case RIGHT:
				return LEFT;
			case DOWN:
				return UP;
			case UP:
				return DOWN;
			default:
				return null;
		}
	}

	public static Move parseMove(char c)
	{
		c = Character.toUpperCase(c);
		switch (c)
		{
			case 'L':
				return LEFT;
			case 'R':
				return RIGHT;
			case 'D':
				return DOWN;
			case 'U':
				return UP;
			default:
				return null;
		}
	}
}
