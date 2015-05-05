package xizz.runtracker;

import android.app.Fragment;


public class RunListActivity extends SingleFragmentActivity {
	@Override
	protected Fragment createFragment() {
		return new RunListFragment();
	}
}
