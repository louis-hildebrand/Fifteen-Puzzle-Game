package fifteenpuzzle;

import java.awt.Color;

public class Settings
{
	// Tile movement animations / scrambling
	public static int ANIMATION_TIME = 40; // Time (in milliseconds) for animations (e.g. moving a tile from one location to another)
	public static double SCRAMBLE_ANIMATION_DELAY = 0.5; // Time (as a proportion of ANIMATION_TIME) to wait while animating the scramble if the puzzle is busy
	public static int MIN_SCRAMBLE_ANIMATION_WAIT = 5; // (MUST BE STRICTLY GREATER THAN ZERO) Minimum time (as a proportion of ANIMATION_TIME) to wait while animating the scramble if the puzzle is busy
	public static int SCRAMBLE_SPEED = 24; // Animation time (in milliseconds) when scrambling the puzzle
	public static int MAX_SCRAMBLE_TIME = 60000; // Maximum time that can be taken for the scramble animation
	public static final int MAX_SCRAMBLE_SIZE = 9; // Maximum puzzle size (rows * cols) for an animated scramble to be attempted
	public static int INSPECTION_TIME = 7000; // Time (in milliseconds) for user to inspect puzzle before starting
	public static final int STEP_SIZE = 20; // Number of pixels a tile should move at a time

	public static final String HOME_DIR =
			System.getProperty("user.home") + "\\AppData\\Roaming\\Fifteen Puzzle"; // Directory in which game data (including icons) are stored
	public static final String ASSET_DIR = HOME_DIR + "\\Assets";

	// GUI icons
	public static final String BUTTON_DIR = ASSET_DIR + "\\Buttons";
	public static final String RADIO_BUTTON_DIR = ASSET_DIR + "\\Radio Buttons";

	public static final String getTilePath(int value, boolean isDark)
	{
		String color = isDark ? "Dark" : "Light";
		return ASSET_DIR + "\\" + STYLE.name + "\\" + color + "\\"
				+ Integer.toString(value) + ".png";
	}

	public static Appearance STYLE = Appearance.WOOD; // Style of the tiles and board
	public static TilePattern TILE_PATTERN = TilePattern.COLUMNS; // Pattern of light/dark tiles
	public static Color MAIN_COLOR = new Color(240, 240, 240);
	public static Color ACCENT_COLOR = new Color(195, 240, 255);

	// Solve history
	public static final String SOLVE_HISTORY = HOME_DIR + "\\solves.txt"; // Directory in which past solves are stored
	public static boolean SAVE_SOLVES = true;

	// Puzzle dimensions
	public static final int MIN_ROWS = 2;
	public static final int MAX_ROWS = 10;
	public static final int MIN_COLS = 2;
	public static final int MAX_COLS = 10;
}