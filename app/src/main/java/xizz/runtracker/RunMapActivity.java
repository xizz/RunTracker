package xizz.runtracker;

import android.app.Fragment;

public class RunMapActivity extends SingleFragmentActivity {
	public static final String EXTRA_RUN_ID = "xizz.runtracker.run_id";

	@Override
	protected Fragment createFragment() {
		long runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
		if (runId != -1)
			return RunMapFragment.newInstance(runId);
		else
			return new RunMapFragment();
	}
}
