package fifteenpuzzle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class SettingsMenu implements KeyListener, ActionListener
{
	private static final double CATEGORY_PANE_WIDTH = 0.25;
	private static final Color CATEGORY_PANE_COLOR = Settings.ACCENT_COLOR;
	private static final Color CHOICE_PANE_COLOR = Settings.MAIN_COLOR;
	private static final int LEFT_PANE_SPACE = 30;
	private static final int BUTTON_WIDTH = 250;
	private static final int BUTTON_HEIGHT = 75;

	private JFrame window;
	private JPanel settingsPane;
	private JPanel categoryPane;
	private String[] categories = { "General", "Tiles", "Animations" };
	private JPanel choicePane;
	private SyncObject menuSync;
	private boolean isEnded;

	public SettingsMenu(JFrame window, SyncObject menuSync)
	{
		this.window = window;
		this.menuSync = menuSync;
		isEnded = false;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				createAndShowGUI();
			}
		});
	}

	private void createAndShowGUI()
	{
		settingsPane = new JPanel();
		settingsPane.setBackground(CHOICE_PANE_COLOR);
		settingsPane.setLayout(new BorderLayout());
		window.add(settingsPane);

		window.revalidate();

		// Left pane (category selection)
		JPanel leftPane = new JPanel();
		leftPane.setLayout(new BorderLayout());
		settingsPane.add(leftPane, BorderLayout.WEST);

		JPanel quitPane = new JPanel();
		quitPane.setOpaque(true);
		quitPane.setBackground(CATEGORY_PANE_COLOR);
		quitPane.setLayout(new BoxLayout(quitPane, BoxLayout.LINE_AXIS));
		leftPane.add(quitPane, BorderLayout.NORTH);

		quitPane.add(MainMenu.makeButton("ESC", 100, 30, this, MainMenu.NORMAL_FONT));

		categoryPane = new JPanel();
		categoryPane.setOpaque(true);
		categoryPane.setBackground(CATEGORY_PANE_COLOR);
		categoryPane.setLayout(new BoxLayout(categoryPane, BoxLayout.PAGE_AXIS));
		categoryPane.setPreferredSize(new Dimension((int) (CATEGORY_PANE_WIDTH * settingsPane.getWidth()), settingsPane.getHeight()));
		leftPane.add(categoryPane, BorderLayout.CENTER);
		categoryPane.addKeyListener(this);
		categoryPane.requestFocusInWindow();

		categoryPane.add(Box.createRigidArea(new Dimension(10, LEFT_PANE_SPACE)));

		ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < categories.length; i++)
		{
			JRadioButton button = MainMenu.makeRadioButton(categories[i], i == 0, BUTTON_WIDTH, BUTTON_HEIGHT, MainMenu.NORMAL_FONT);
			button.setActionCommand(categories[i].toUpperCase());
			button.addActionListener(this);
			categoryPane.add(button);
			group.add(button);
		}
		showChoicePane(categories[0].toUpperCase());

		window.revalidate();
	}

	private void showChoicePane(String category)
	{
		if (choicePane != null)
			window.remove(choicePane);

		if (category.toUpperCase().equals(categories[0].toUpperCase()))
			showGeneralPane();
		else if (category.toUpperCase().equals(categories[1].toUpperCase()))
			showTilePane();
		else if (category.toUpperCase().equals(categories[2].toUpperCase()))
			showAnimationPane();

		window.revalidate();
	}

	private void showGeneralPane()
	{
		choicePane = new JPanel();
		choicePane.setOpaque(false);
		choicePane.setLayout(new GridLayout(6, 1));
		settingsPane.add(choicePane, BorderLayout.CENTER);

	}

	private void showTilePane()
	{
		choicePane = new JPanel();
		choicePane.setOpaque(false);
		choicePane.add(new JLabel("Tile settings in development..."));
		settingsPane.add(choicePane, BorderLayout.CENTER);
	}

	private void showAnimationPane()
	{
		choicePane = new JPanel();
		choicePane.setOpaque(false);
		choicePane.add(new JLabel("Animation settings in development..."));
		settingsPane.add(choicePane, BorderLayout.CENTER);
	}

	private void quit()
	{
		isEnded = true;
		window.remove(settingsPane);
		synchronized (menuSync)
		{
			menuSync.notify();
		}
	}

	public boolean isEnded()
	{
		return isEnded;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "ESC":
				quit();
				break;
			default:
				showChoicePane(e.getActionCommand());
				categoryPane.requestFocusInWindow();
				break;
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_ESCAPE:
				quit();
				break;
		}
	}

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
