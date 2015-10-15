package xizz.runtracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class RunManager {
	public static final String ACTION_LOCATION = "xizz.runtracker.ACTION_LOCATION";

	private static final String TAG = "RunManager";
	private static final String PREFS_FILE = "runs";
	private static final String PREF_CURRENT_RUN_ID = "RunManager.currentRunId";

	private static RunManager sRunManager;
	private Context mAppContext;
	private LocationManager mLocationManager;

	private RunDatabaseHelper mHelper;
	private SharedPreferences mPrefs;
	private long mCurrentRunId;

	private RunManager(Context appContext) {
		mAppContext = appContext;
		mLocationManager = (LocationManager) mAppContext.getSystemService(Context
				.LOCATION_SERVICE);
		mHelper = new RunDatabaseHelper(mAppContext);
		mPrefs = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		mCurrentRunId = mPrefs.getLong(PREF_CURRENT_RUN_ID, -1);
	}

	public static RunManager get(Context c) {
		if (sRunManager == null)
			sRunManager = new RunManager(c.getApplicationContext());
		return sRunManager;
	}

	public boolean isTrackingRun(Run run) { return run != null && run.id == mCurrentRunId; }

	public Run startNewRun() {
		Run run = insertRun();
		startTrackingRun(run);
		return run;
	}

	public void stopRun() {
		stopLocationUpdates();
		mCurrentRunId = -1;
		mPrefs.edit().remove(PREF_CURRENT_RUN_ID).commit();
	}

	public void startTrackingRun(Run run) {
		mCurrentRunId = run.id;
		mPrefs.edit().putLong(PREF_CURRENT_RUN_ID, mCurrentRunId).commit();
		startLocationUpdates();
	}

	public void insertLocation(Location location) {
		if (mCurrentRunId != -1)
			mHelper.insertLocation(mCurrentRunId, location);
		else
			Log.e(TAG, "Location received with no tracking run; ignoring.");
	}

	public Run getRun(long id) {
		Run run = null;
		RunDatabaseHelper.RunCursor cursor = mHelper.queryRun(id);
		cursor.moveToFirst();
		if (!cursor.isAfterLast())
			run = cursor.getRun();
		cursor.close();
		return run;
	}

	public List<Location> getAllLocationsForRun(long runId) {
		List<Location> locations = new LinkedList<>();
		RunDatabaseHelper.LocationCursor cursor = mHelper.queryAllLocationsForRun(runId);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			locations.add(cursor.getLocation());
			cursor.moveToNext();
		}
		cursor.close();
		return locations;
	}

	public RunDatabaseHelper.RunCursor queryRuns() {
		return mHelper.queryRuns();
	}


	private void startLocationUpdates() {
		String provider = LocationManager.GPS_PROVIDER;

		Location lastKnown = mLocationManager.getLastKnownLocation(provider);
		if (lastKnown != null) {
			lastKnown.setTime(System.currentTimeMillis());
			broadcastLocation(lastKnown);
		}
		PendingIntent pendingIntent = getLocationPendingIntent(true);
		mLocationManager.requestLocationUpdates(provider, 0, 0, pendingIntent);
	}

	private void stopLocationUpdates() {
		PendingIntent pi = getLocationPendingIntent(false);
		if (pi != null) {
			mLocationManager.removeUpdates(pi);
			pi.cancel();
		}
	}

	private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
		Intent broadcast = new Intent(ACTION_LOCATION);
		broadcast.putExtra(RunActivity.EXTRA_RUN_ID, mCurrentRunId);
		int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
		return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
	}

	private void broadcastLocation(Location location) {
		Intent broadcast = new Intent(ACTION_LOCATION);
		broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
		broadcast.putExtra(RunActivity.EXTRA_RUN_ID, mCurrentRunId);
		mAppContext.sendBroadcast(broadcast);
	}

	private Run insertRun() {
		Run run = new Run();
		run.id = mHelper.insertRun(run);
		return run;
	}
}
