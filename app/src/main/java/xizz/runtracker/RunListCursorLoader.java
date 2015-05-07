package xizz.runtracker;

import android.content.Context;
import android.database.Cursor;

public class RunListCursorLoader extends SQLiteCursorLoader {

	public RunListCursorLoader(Context context) {
		super(context);
	}

	@Override
	protected Cursor loadCursor() {
		return RunManager.get(getContext()).queryRuns();
	}
}
