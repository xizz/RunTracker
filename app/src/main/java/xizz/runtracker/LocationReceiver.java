package xizz.runtracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.Date;

public class LocationReceiver extends BroadcastReceiver {
	public static final String ACTION_LOCATION_SAVED = "xizz.runtracker.ACTION_LOCATION_SAVED";

	private static final String TAG = "LocationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Location loc = intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
		if (loc != null) {
			onLocationReceived(context, loc);
			loc.setTime(new Date().getTime());
			RunManager.get(context).insertLocation(loc);
			Log.d(TAG, "location saved");
			Intent i = new Intent(context, RunActivity.class);
			i.putExtra(RunActivity.EXTRA_RUN_ID, intent.getLongExtra(RunActivity.EXTRA_RUN_ID,
					-1));
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i,
					PendingIntent.FLAG_UPDATE_CURRENT);
			Notification.Builder builder = new Notification.Builder(context)
					.setSmallIcon(android.R.drawable.ic_menu_mylocation)
					.setContentTitle("New Location")
					.setContentText(loc.getLatitude() + ", " + loc.getLongitude())
					.setAutoCancel(true)
					.setContentIntent(pendingIntent);
			NotificationManager notificationManager =
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(0, builder.build());
			broadcastLocation(context, intent.getLongExtra(RunActivity.EXTRA_RUN_ID, -1));
		} else if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
			boolean enabled = intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false);
			onProviderEnabledChanged(enabled);
		}
	}

	protected void onLocationReceived(Context context, Location loc) {
		Log.d(TAG, this + " Got location from " + loc.getProvider() + ": " + loc.getLatitude() +
				", " + loc.getLongitude());
	}

	protected void onProviderEnabledChanged(boolean enabled) {
		Log.d(TAG, "Provider " + (enabled ? "enabled" : "disabled"));
	}

	private void broadcastLocation(Context context, long runId) {
		Intent broadcast = new Intent(ACTION_LOCATION_SAVED);
		broadcast.putExtra(RunActivity.EXTRA_RUN_ID, runId);
		context.sendBroadcast(broadcast);
	}
}
