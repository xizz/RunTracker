package xizz.runtracker;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
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

	private RunManager mRunManager;

	private Run mRun;
	private Location mLastLocation;

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
				mRun = mRunManager.getRun(runId);
				mLastLocation = mRunManager.getLastLocationForRun(runId);
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
				new IntentFilter(RunManager.ACTION_LOCATION));
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

		List<Location> locations = mRunManager.getAllLocationsForRun(mRun.id);
		StringBuilder text = new StringBuilder();
		for (Location l : locations) {
			text.append(new Date(l.getTime()) + "\n" + l.getLatitude() + ", " + l.getLongitude() +
					"\n");
		}
		mStartedTextView.setText(mRun.startDate.toString());
		mRecordsTextView.setText(text);

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
			Location loc = intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
			if (loc != null && mRunManager.isTrackingRun(mRun)) {
				mLastLocation = loc;
				if (isVisible())
					updateUI();
			} else if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
				boolean enabled = intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED,
						false);
				int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
				Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
			}
		}
	}
}
