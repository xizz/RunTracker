package xizz.runtracker;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

public class RunFragment extends Fragment {
	private static final String TAG = "RunFragment";
	private static final String ARG_RUN_ID = "RUN_ID";
	private static final int LOAD_RUN = 0;
	private static final int LOAD_LOCATION = 1;
	private static final int LOAD_LOCATIONS = 2;

	private RunManager mRunManager;

	private Run mRun;
	private Location mLastLocation;
	private List<Location> mLocations;

	private Button mStartButton, mStopButton;
	private TextView mStartedTextView, mLatitudeTextView,
			mLongitudeTextView, mAltitudeTextView, mDurationTextView, mRecordsTextView;

	private BroadcastReceiver mLocationReceiver = new MyLocationReceiver();

	public static RunFragment newInstance(long runId) {
		Bundle args = new Bundle();
		args.putLong(ARG_RUN_ID, runId);
		RunFragment rf = new RunFragment();
		rf.setArguments(args);
		return rf;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		mRunManager = RunManager.get(getActivity());

		Bundle args = getArguments();
		if (args != null) {
			long runId = args.getLong(ARG_RUN_ID, -1);
			if (runId != -1) {
				LoaderManager loaderManager = getLoaderManager();
				loaderManager.initLoader(LOAD_RUN, args, new RunLoaderCallbacks());
				loaderManager.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
				loaderManager.initLoader(LOAD_LOCATIONS, args, new LocationListLoaderCallbacks());
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_run, container, false);

		mStartedTextView = (TextView) view.findViewById(R.id.run_startedTextView);
		mLatitudeTextView = (TextView) view.findViewById(R.id.run_latitudeTextView);
		mLongitudeTextView = (TextView) view.findViewById(R.id.run_longitudeTextView);
		mAltitudeTextView = (TextView) view.findViewById(R.id.run_altitudeTextView);
		mDurationTextView = (TextView) view.findViewById(R.id.run_durationTextView);
		mRecordsTextView = (TextView) view.findViewById(R.id.run_recordsTextView);

		mStartButton = (Button) view.findViewById(R.id.run_startButton);
		mStartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mRun == null)
					mRun = mRunManager.startNewRun();
				else
					mRunManager.startTrackingRun(mRun);
				updateUI();
			}
		});

		mStopButton = (Button) view.findViewById(R.id.run_stopButton);
		mStopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRunManager.stopRun();
				updateUI();
			}
		});

		updateUI();

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().registerReceiver(mLocationReceiver,
				new IntentFilter(LocationReceiver.ACTION_LOCATION_SAVED));
	}

	@Override
	public void onStop() {
		getActivity().unregisterReceiver(mLocationReceiver);
		super.onStop();
	}

	private void updateUI() {
		boolean trackingThisRun = mRunManager.isTrackingRun(mRun);

		if (mRun == null)
			return;
		if (mLocations != null) {
			StringBuilder text = new StringBuilder();
			for (Location l : mLocations) {
				text.append(new Date(l.getTime()));
				text.append("\n");
				text.append(l.getLatitude());
				text.append(", ");
				text.append(l.getLongitude());
				text.append("\n");
			}
			mStartedTextView.setText(mRun.startDate.toString());
			mRecordsTextView.setText(text);
		}

		int durationSeconds = 0;
		if (mLastLocation != null) {
			durationSeconds = mRun.getDurationSeconds(mLastLocation.getTime());
			mLatitudeTextView.setText(Double.toString(mLastLocation.getLatitude()));
			mLongitudeTextView.setText(Double.toString(mLastLocation.getLongitude()));
			mAltitudeTextView.setText(Double.toString(mLastLocation.getAltitude()));
		}
		mDurationTextView.setText(Run.formatDuration(durationSeconds));

		mStartButton.setEnabled(!trackingThisRun);
		mStopButton.setEnabled(trackingThisRun);
	}

	private class MyLocationReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mRunManager.isTrackingRun(mRun)) {
				Bundle args = getArguments();
				if (args == null)
					args = new Bundle();
				if (args.getLong(ARG_RUN_ID, -1) == -1)
					args.putLong(ARG_RUN_ID, mRun.id);
				Log.d(TAG, "loading loacations");
				getLoaderManager().restartLoader(LOAD_LOCATIONS, args,
						new LocationListLoaderCallbacks());
			} else if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
				boolean enabled = intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED,
						false);
				int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
				Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
			}
		}
	}

	private class RunLoaderCallbacks implements LoaderManager.LoaderCallbacks<Run> {
		@Override
		public Loader<Run> onCreateLoader(int id, Bundle args) {
			return new RunLoader(getActivity(), args.getLong(ARG_RUN_ID));
		}

		@Override
		public void onLoadFinished(Loader<Run> loader, Run data) {
			mRun = data;
			updateUI();
		}

		@Override
		public void onLoaderReset(Loader<Run> loader) { }
	}

	private class LocationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Location> {
		@Override
		public Loader<Location> onCreateLoader(int id, Bundle args) {
			return new LastLocationLoader(getActivity(), args.getLong(ARG_RUN_ID));
		}

		@Override
		public void onLoadFinished(Loader<Location> loader, Location location) {
			mLastLocation = location;
			updateUI();
		}

		@Override
		public void onLoaderReset(Loader<Location> loader) { }
	}

	private class LocationListLoaderCallbacks
			implements LoaderManager.LoaderCallbacks<List<Location>> {
		@Override
		public Loader<List<Location>> onCreateLoader(int id, Bundle args) {
			return new LocationListLoader(getActivity(), args.getLong(ARG_RUN_ID));
		}

		@Override
		public void onLoadFinished(Loader<List<Location>> loader, List<Location> locations) {
			mLocations = locations;
			mLastLocation = locations.get(0);
			updateUI();
		}

		@Override
		public void onLoaderReset(Loader<List<Location>> loader) { }
	}
}
