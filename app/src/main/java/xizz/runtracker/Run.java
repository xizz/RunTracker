package xizz.runtracker;

import java.util.Date;

public class Run {
	public long id;
	public Date startDate;

	public Run() {
		id = -1;
		startDate = new Date();
	}

	public static String formatDuration(int durationSeconds) {
		int seconds = durationSeconds % 60;
		int minutes = ((durationSeconds - seconds) / 60) % 60;
		int hours = (durationSeconds - (minutes * 60) - seconds) / 3600;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	public int getDurationSeconds(long endMillis) {
		return (int) ((endMillis - startDate.getTime()) / 1000);
	}

}
