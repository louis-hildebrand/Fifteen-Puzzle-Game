package fifteenpuzzle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * The main menu for the game. Displays the main menu GUI when the game is
 * started and includes methods for launching the various game modes.
 * 
 * TODO Add documentation everywhere
 * 
 * @author louis
 */
public class MainMenu implements ActionListener
{
	private static final double TOP_PANE_HEIGHT = 0.25;
	private static final Color TOP_PANE_COLOR = Settings.ACCENT_COLOR;
	private static final Color BUTTON_PANE_COLOR = Settings.MAIN_COLOR;
	private static final Color BOT_RIGHT_COLOR = Settings.MAIN_COLOR;
	private static final Font TITLE_FONT = new Font("Sans Serif", Font.BOLD, 90);
	private static final Color TITLE_COLOR = new Color(7, 55, 99);
	public static final Font NORMAL_FONT = new Font("Dialog", Font.PLAIN, 40);
	private static final Font BOLD_FONT = new Font("Dialog", Font.BOLD, 50);
	private static final int BUTTON_WIDTH = 300;
	private static final int BUTTON_HEIGHT = 90;
	private static final int RADIO_BUTTON_WIDTH = 250;
	private static final int RADIO_BUTTON_HEIGHT = 75;

	// Test comment

	private JFrame frame;
	private JPanel menuPane;
	private SyncObject sync;
	private JRadioButton timedMode;
	private JRadioButton freeMode;
	private JSpinner rowSpinner;
	private JSpinner colSpinner;

	/**
	 * Creates the main menu, calls createAndShowGUI() on the EDT
	 */
	public static void main(String[] args)
	{
		MainMenu mm = new MainMenu();
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// Set up GUI
				mm.createAndShowGUI();

				// Get the app data folder
				if (!new File(Settings.HOME_DIR).exists())
				{
					String msg = "This app uses the " + Settings.HOME_DIR
							+ "\nfolder to store solve results and GUI components. You may choose\nnot to create the directory, but your solves would not be saved.";
					String[] options = new String[] { "Create directory", "Don't create" };
					int choice = JOptionPane.showOptionDialog(null, msg, "Directory creation", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if (choice == JOptionPane.NO_OPTION)
						Settings.SAVE_SOLVES = false;
					else
					{
						boolean success = new File(Settings.HOME_DIR).mkdir();
						if (!success)
							JOptionPane.showMessageDialog(null, "An unexpected error occurred and the directory could not be created",
									"Directory creation error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
	}

	/**
	 * Draws and displays the main menu GUI
	 * 
	 * TODO Speed up startup?
	 */
	private void createAndShowGUI()
	{
		// Provide an object on which to synchronize threads
		sync = new SyncObject();

		// Create window for entire app
		frame = new JFrame("15 Puzzle");
		frame.setSize(new Dimension(1200, 700));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);

		// Create main menu GUI
		menuPane = new JPanel();
		menuPane.setLayout(new BorderLayout());
		frame.add(menuPane);

		// Title and name
		JPanel topPane = new JPanel();
		topPane.setOpaque(true);
		topPane.setBackground(TOP_PANE_COLOR);
		topPane.setLayout(new BoxLayout(topPane, BoxLayout.PAGE_AXIS));
		topPane.setPreferredSize(new Dimension(frame.getWidth(), (int) (frame.getHeight() * TOP_PANE_HEIGHT)));
		menuPane.add(topPane, BorderLayout.NORTH);

		topPane.add(Box.createVerticalGlue());

		JLabel title = new JLabel("15 Puzzle");
		title.setFont(TITLE_FONT);
		title.setForeground(TITLE_COLOR);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		topPane.add(title);

		JLabel name = new JLabel("Developed by Louis Hildebrand");
		name.setFont(NORMAL_FONT);
		name.setForeground(TITLE_COLOR);
		name.setAlignmentX(Component.CENTER_ALIGNMENT);
		topPane.add(name);

		topPane.add(Box.createVerticalGlue());

		// Bottom Pane
		JPanel botPane = new JPanel();
		botPane.setLayout(new GridLayout(1, 2));
		menuPane.add(botPane, BorderLayout.CENTER);

		// Bottom left: buttons
		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(BUTTON_PANE_COLOR);
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.PAGE_AXIS));
		botPane.add(buttonPane);

		buttonPane.add(Box.createVerticalGlue());
		buttonPane.add(Box.createVerticalGlue());
		buttonPane.add(Box.createVerticalGlue());

		buttonPane.add(makeButton("Start", BUTTON_WIDTH, BUTTON_HEIGHT, this, NORMAL_FONT));

		buttonPane.add(Box.createVerticalGlue());

		buttonPane.add(makeButton("Statistics", BUTTON_WIDTH, BUTTON_HEIGHT, this, NORMAL_FONT));

		buttonPane.add(Box.createVerticalGlue());

		buttonPane.add(makeButton("Settings", BUTTON_WIDTH, BUTTON_HEIGHT, this, NORMAL_FONT));

		buttonPane.add(Box.createVerticalGlue());

		buttonPane.add(makeButton("Quit", BUTTON_WIDTH, BUTTON_HEIGHT, this, NORMAL_FONT));

		buttonPane.add(Box.createVerticalGlue());
		buttonPane.add(Box.createVerticalGlue());
		buttonPane.add(Box.createVerticalGlue());

		// Bottom right
		JPanel botRight = new JPanel();
		botRight.setLayout(new BoxLayout(botRight, BoxLayout.PAGE_AXIS));
		botRight.setBackground(BOT_RIGHT_COLOR);
		botPane.add(botRight);

		botRight.add(Box.createVerticalGlue());

		// Game mode selection
		JLabel gameModeMsg = new JLabel("Select a game mode:");
		gameModeMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
		gameModeMsg.setFont(BOLD_FONT);
		botRight.add(gameModeMsg);

		ButtonGroup gameModes = new ButtonGroup();

		freeMode = makeRadioButton("Freeplay", true, RADIO_BUTTON_WIDTH, RADIO_BUTTON_HEIGHT, NORMAL_FONT);
		gameModes.add(freeMode);
		botRight.add(freeMode);

		timedMode = makeRadioButton("Timed", false, RADIO_BUTTON_WIDTH, RADIO_BUTTON_HEIGHT, NORMAL_FONT);
		gameModes.add(timedMode);
		botRight.add(timedMode);

		botRight.add(Box.createVerticalGlue());

		// Dimension selection
		JLabel dimSelectMsg = new JLabel("Select puzzle dimensions:");
		dimSelectMsg.setFont(BOLD_FONT);
		dimSelectMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
		botRight.add(dimSelectMsg);

		JPanel dimSpinnerPane = new JPanel();
		dimSpinnerPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		dimSpinnerPane.setMaximumSize(new Dimension(200, 30));
		dimSpinnerPane.setLayout(new GridLayout(1, 3));
		dimSpinnerPane.setOpaque(false);
		botRight.add(dimSpinnerPane);

		rowSpinner = new JSpinner(new SpinnerNumberModel(4, Settings.MIN_ROWS, Settings.MAX_ROWS, 1));
		rowSpinner.setFont(NORMAL_FONT);
		dimSpinnerPane.add(rowSpinner);
		JLabel temp = new JLabel("X");
		temp.setHorizontalAlignment(JLabel.CENTER);
		temp.setVerticalAlignment(JLabel.CENTER);
		temp.setFont(NORMAL_FONT);
		dimSpinnerPane.add(temp);
		colSpinner = new JSpinner(new SpinnerNumberModel(4, Settings.MIN_COLS, Settings.MAX_COLS, 1));
		colSpinner.setFont(NORMAL_FONT);
		dimSpinnerPane.add(colSpinner);

		botRight.add(Box.createVerticalGlue());

		frame.revalidate();
	}

	public static JButton makeButton(String name, int width, int height, ActionListener listener, Font backupFont)
	{
		JButton button = new JButton();
		String iconPath = Settings.BUTTON_DIR + "\\" + name;
		if (new File(iconPath + ".png").exists())
		{
			Image img = new ImageIcon(iconPath + ".png").getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
			button.setIcon(new ImageIcon(img));

			String rolloverIconPath = iconPath + "_Rollover.png";
			if (new File(rolloverIconPath).exists())
			{
				Image rolloverImg = new ImageIcon(rolloverIconPath).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
				button.setRolloverIcon(new ImageIcon(rolloverImg));

				String pressedIconPath = iconPath + "_Pressed.png";
				if (new File(pressedIconPath).exists())
				{
					Image pressedImg = new ImageIcon(pressedIconPath).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
					button.setPressedIcon(new ImageIcon(pressedImg));
				}
			}

			button.setBorder(null);
			button.setOpaque(false);
			button.setContentAreaFilled(false);
		}
		else
		{
			button.setText(name);
			button.setFont(backupFont);
			button.setMinimumSize(new Dimension(width, height));
			button.setMaximumSize(new Dimension(width, height));
		}

		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.addActionListener(listener);
		button.setActionCommand(name.toUpperCase());

		return button;
	}

	public static JRadioButton makeRadioButton(String name, boolean isSelected, int width, int height, Font backupFont)
	{
		JRadioButton button = new JRadioButton();

		String notSelectedPath = Settings.RADIO_BUTTON_DIR + "\\" + name + "_NotSelected.png";
		String selectedPath = Settings.RADIO_BUTTON_DIR + "\\" + name + "_Selected.png";
		if (new File(notSelectedPath).exists() && new File(selectedPath).exists())
		{
			Image notSelectedImg = new ImageIcon(notSelectedPath).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
			button.setIcon(new ImageIcon(notSelectedImg));

			Image selectedImg = new ImageIcon(selectedPath).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
			button.setSelectedIcon(new ImageIcon(selectedImg));
		}
		else
		{
			button.setText(name);
			button.setFont(backupFont);
		}

		button.setOpaque(false);
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setSelected(isSelected);

		return button;
	}

	/**
	 * Creates a new TimedSession and waits for it to end
	 */
	private void startTimedSession()
	{
		Thread gameThread = new Thread()
		{
			public void run()
			{
				frame.remove(menuPane);
				frame.revalidate();
				frame.repaint();

				int[] dim = getDimensions();
				TimedSession currentSession = new TimedSession(dim[0], dim[1], frame, sync);

				try
				{
					synchronized (sync)
					{
						while (!currentSession.isEnded())
						{
							sync.wait();
						}
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				frame.add(menuPane);
				frame.revalidate();
				frame.repaint();
			}
		};
		gameThread.start();
	}

	/**
	 * Creates a new FreeSession and waits for it to end
	 */
	private void startFreeSession()
	{
		Thread gameThread = new Thread()
		{
			public void run()
			{
				frame.remove(menuPane);
				frame.revalidate();
				frame.repaint();

				int[] dim = getDimensions();
				FreeSession currentSession = new FreeSession(dim[0], dim[1], frame, sync);

				try
				{
					synchronized (sync)
					{
						while (!currentSession.isEnded())
						{
							sync.wait();
						}
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				frame.add(menuPane);
				frame.revalidate();
				frame.repaint();
			}
		};
		gameThread.start();
	}

	/**
	 * Accesses the current value of the spinner for the puzzle dimensions
	 * 
	 * @return An int array whose first element is the number of rows and whose
	 *         second element is the number of columns
	 */
	private int[] getDimensions()
	{
		return new int[] { (int) rowSpinner.getValue(), (int) colSpinner.getValue() };
	}

	/**
	 * TODO Write solve history page
	 */
	private void showSolveHistory()
	{
		System.out.println("Solve history in progress...");
	}

	// TODO Write settings page
	private void showSettings()
	{
		System.out.println("Settings menu in progress...");
		//		Thread settingsThread = new Thread(new Runnable()
		//		{
		//			public void run()
		//			{
		//				frame.remove(menuPane);
		//				frame.revalidate();
		//
		//				SyncObject menuSync = new SyncObject();
		//				SettingsMenu settings = new SettingsMenu(frame, menuSync);
		//
		//				while (!settings.isEnded())
		//				{
		//					synchronized (menuSync)
		//					{
		//						try
		//						{
		//							menuSync.wait();
		//						}
		//						catch (InterruptedException e)
		//						{
		//							break;
		//						}
		//					}
		//				}
		//
		//				frame.add(menuPane);
		//				frame.revalidate();
		//				frame.repaint();
		//			}
		//		});
		//		settingsThread.start();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "START":
				if (timedMode.isSelected())
					startTimedSession();
				else if (freeMode.isSelected())
					startFreeSession();
				break;
			case "STATISTICS":
				showSolveHistory();
				break;
			case "SETTINGS":
				showSettings();
				break;
			case "QUIT":
				System.exit(0);
				break;
		}
	}
}
