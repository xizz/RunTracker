package xizz.runtracker;

import android.content.Context;
import android.location.Location;

import java.util.List;

public class LocationListLoader extends DataLoader<List<Location>> {

	private long mRunId;

	public LocationListLoader(Context context, long runId) {
		super(context);
		mRunId = runId;
	}

	@Override
	public List<Location> loadInBackground() {
		return RunManager.get(getContext()).getAllLocationsForRun(mRunId);
	}
}
