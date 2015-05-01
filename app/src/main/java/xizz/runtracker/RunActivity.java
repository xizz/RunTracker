package xizz.runtracker;

import android.app.Fragment;

public class RunActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() { return new RunFragment(); }

}
