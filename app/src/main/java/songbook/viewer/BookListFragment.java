package songbook.viewer;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import songbook.viewer.interfaces.OnIndexSelectedListener;

public class BookListFragment extends ListFragment {

	private OnIndexSelectedListener mListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.booklist_fragment, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnIndexSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnArticleSelectedListener");
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {

		mListener.onIndexSelected((String) list.getItemAtPosition(position));

	}
}
