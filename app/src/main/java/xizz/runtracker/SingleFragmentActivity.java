package xizz.runtracker;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.FrameLayout;

public abstract class SingleFragmentActivity extends Activity {

	protected abstract Fragment createFragment();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FrameLayout fl = new FrameLayout(this);
		fl.setId(R.id.fragmentContainer);
		setContentView(fl);

		FragmentManager manager = getFragmentManager();

		if (manager.findFragmentById(R.id.fragmentContainer) == null)
			manager.beginTransaction().add(R.id.fragmentContainer, createFragment()).commit();
	}
}
