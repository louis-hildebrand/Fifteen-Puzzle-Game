package fifteenpuzzle;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.time.StopWatch;

public class PuzzleTimer
{
	private static final Font TIMER_FONT = new Font("Helvetica", Font.BOLD, 80);
	private final static Color TIMER_BACK_COLOR = Color.BLACK;
	public final static Color TIMER_TEXT_COLOR_SOLVE = Color.YELLOW;
	public final static Color TIMER_TEXT_COLOR_INSPECTION = Color.GREEN;

	private static JLabel timerImage;
	private static StopWatch stopwatch;
	private Thread timerThread;

	public PuzzleTimer(JPanel timerPane, int distFromTop)
	{
		stopwatch = new StopWatch();
		timerImage = new JLabel(" 0:00:00.0 ");
		timerImage.setAlignmentX(Component.CENTER_ALIGNMENT);
		timerImage.setOpaque(true);
		timerImage.setBackground(TIMER_BACK_COLOR);
		timerImage.setFont(TIMER_FONT);
		timerImage.setForeground(TIMER_TEXT_COLOR_SOLVE);

		timerPane.add(Box.createRigidArea(new Dimension(10, distFromTop)));
		timerPane.add(timerImage);
		timerPane.revalidate();
		timerPane.repaint();
	}

	public void startTimer()
	{
		stopwatch.start();
		SyncObject sync = new SyncObject();

		timerThread = new Thread(new Runnable()
		{
			public void run()
			{
				while (true)
				{
					setTimerText(millisToString(stopwatch.getTime()));

					synchronized (sync)
					{
						try
						{
							sync.wait(100);
						}
						catch (InterruptedException e)
						{
							break;
						}
					}
				}

				System.out.println("Ended TimerThread");
			}
		});
		timerThread.setName("TimerThread");
		timerThread.start();
	}

	public void halt()
	{
		stopwatch.stop();
		timerThread.interrupt();
		setTimerText(millisToString(stopwatch.getTime()));
	}

	public void reset()
	{
		stopwatch.reset();
		setTimerText(" 0:00:00.0 ");
	}

	public void inspection(TimedSession session)
	{
		timerImage.setForeground(TIMER_TEXT_COLOR_INSPECTION);
		setTimerText(millisToString(Settings.INSPECTION_TIME));
		stopwatch.start();

		SyncObject inspectionSync = new SyncObject();
		while (session.getStatus() == SolveStatus.INSPECT && stopwatch.getTime() < Settings.INSPECTION_TIME)
		{
			synchronized (inspectionSync)
			{
				long remainingTime = 1000 * (1 + (Settings.INSPECTION_TIME - stopwatch.getTime()) / 1000);
				setTimerText(millisToString(remainingTime));

				try
				{
					inspectionSync.wait(100);
				}
				catch (InterruptedException e)
				{
				}
			}
		}

		stopwatch.stop();
		stopwatch.reset();
		timerImage.setForeground(TIMER_TEXT_COLOR_SOLVE);
		setTimerText(" 0:00:00.0 ");
		System.out.println("Finished inspection mode");
	}

	/**
	 * Gets the time elapsed since the beginning of the solve
	 * 
	 * @return Elapsed time (in milliseconds)
	 */
	public long getCurrentTime()
	{
		return stopwatch.getTime();
	}

	public static String millisToString(long millis)
	{
		int hours = (int) (millis / (60 * 60 * 1000));
		millis %= 60 * 60 * 1000;

		int minutes = (int) (millis / (60 * 1000));
		millis %= 60 * 1000;

		int seconds = (int) (millis / 1000);
		millis %= 1000;

		return String.format(" %01d:%02d:%02d.%01d ", hours, minutes, seconds, millis / 100);
	}

	private void setTimerText(String str)
	{
		// System.out.println("Setting text to " + str + "in thread " + Thread.currentThread().getName());

		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				timerImage.setText(str);
			}
		});
	}
}
