package songbook.viewer;

import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import songbook.viewer.data.SQLHelper;

public class ResultFragment extends ListFragment {

    // This is the Adapter being used to display the list's data.
    SimpleCursorAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    private final String TAG = ResultFragment.class.getCanonicalName();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView called");

        return inflater.inflate(R.layout.result_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.song_list_item, null, new String[]{
                SQLHelper.tblSongs_NUMCODE, SQLHelper.tblSongs_TITLE,
                SQLHelper.tblSongs_ARTIST}, new int[]{
                R.id.songListItem_numCode, R.id.songListItem_title,
                R.id.songListItem_artist}, 0);

        setListAdapter(mAdapter);

    }

}
