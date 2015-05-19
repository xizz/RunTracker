package xizz.runtracker;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.List;

public class RunMapFragment extends MapFragment
		implements LoaderManager.LoaderCallbacks<List<Location>> {
	private static final String TAG = "RunMapFragment";
	private static final String ARG_RUN_ID = "RUN_ID";
	private static final int LOAD_LOCATIONS = 0;

	private List<Location> mLocations;
	private GoogleMap mGoogleMap;
	private BroadcastReceiver mLocationReceiver = new MyLocationReceiver();

	public static RunMapFragment newInstance(long runId) {
		Bundle args = new Bundle();
		args.putLong(ARG_RUN_ID, runId);
		RunMapFragment rf = new RunMapFragment();
		rf.setArguments(args);
		return rf;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			long runId = args.getLong(ARG_RUN_ID, -1);
			if (runId != -1) {
				LoaderManager loaderManager = getLoaderManager();
				loaderManager.initLoader(LOAD_LOCATIONS, args, this);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		mGoogleMap = getMap();
		if (mGoogleMap != null)
			mGoogleMap.setMyLocationEnabled(true);
		return v;
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

	@Override
	public Loader<List<Location>> onCreateLoader(int id, Bundle args) {
		return new LocationListLoader(getActivity(), args.getLong(ARG_RUN_ID, -1));
	}

	@Override
	public void onLoadFinished(Loader<List<Location>> loader, List<Location> locations) {
		mLocations = locations;
		updateUI();
	}

	@Override
	public void onLoaderReset(Loader<List<Location>> loader) { }

	private void updateUI() {
		if (mGoogleMap == null || mLocations == null)
			return;

		mGoogleMap.clear();

		PolylineOptions line = new PolylineOptions();
		LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
		Resources r = getResources();

		for (int i = mLocations.size() - 1; i >= 0; --i) {
			LatLng latLng =
					new LatLng(mLocations.get(i).getLatitude(), mLocations.get(i).getLongitude());
			if (i == mLocations.size() - 1) {
				String startDate = new Date(mLocations.get(i).getTime()).toString();
				MarkerOptions startMarkerOptions = new MarkerOptions()
						.position(latLng)
						.title(r.getString(R.string.run_started_at_format, startDate));
				mGoogleMap.addMarker(startMarkerOptions);
			} else if (i == 0 && mLocations.size() > 1) {
				String endDate = new Date(mLocations.get(i).getTime()).toString();
				MarkerOptions finishMarkerOptions = new MarkerOptions()
						.position(latLng)
						.title(r.getString(R.string.run_finish))
						.snippet(r.getString(R.string.run_finished_at_format, endDate));
				mGoogleMap.addMarker(finishMarkerOptions);
			}
			line.add(latLng);
			latLngBuilder.include(latLng);
		}

		mGoogleMap.addPolyline(line);
		LatLngBounds latLngBounds = latLngBuilder.build();
		Point size = new Point();
		getActivity().getWindowManager().getDefaultDisplay().getSize(size);
		CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBounds,
				size.x, size.y, 15);
		mGoogleMap.moveCamera(movement);
	}

	private class MyLocationReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle myArgs = getArguments();
			if (myArgs != null) {
				long runId = myArgs.getLong(ARG_RUN_ID, -1);
				if (runId != -1 && runId == intent.getLongExtra(RunActivity.EXTRA_RUN_ID, -1)) {
					LoaderManager loaderManager = getLoaderManager();
					loaderManager.restartLoader(LOAD_LOCATIONS, myArgs, RunMapFragment.this);
				}
			}
		}
	}
}
