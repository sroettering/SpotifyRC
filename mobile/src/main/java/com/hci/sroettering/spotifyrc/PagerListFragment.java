package com.hci.sroettering.spotifyrc;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.hci.sroettering.spotifyrc.adapters.AlbumsListAdapter;
import com.hci.sroettering.spotifyrc.adapters.ArtistsListAdapter;
import com.hci.sroettering.spotifyrc.adapters.CategoriesListAdapter;
import com.hci.sroettering.spotifyrc.adapters.PlaylistsListAdapter;
import com.hci.sroettering.spotifyrc.adapters.SongsListAdapter;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PagerListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PagerListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PagerListFragment extends ListFragment {

    private static final String ARG_PARAM1 = "current_page";

    private int mCurrentPage;

    private OnFragmentInteractionListener mListener;

    private ListView listView;

    public PagerListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param currentPage Parameter 1.
     * @return A new instance of fragment PagerListFragment.
     */
    public static PagerListFragment newInstance(int currentPage) {
        PagerListFragment fragment = new PagerListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, currentPage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentPage = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pager_fragment, container, false);
        listView = (ListView)view.findViewById(R.id.lv_fragment);
        return view;
    }

    public void updateListViewData(List data) {
        switch(mCurrentPage) {
            case 0:
                if(listView.getAdapter() == null) {
                    listView.setAdapter(new PlaylistsListAdapter(getContext(), data));
                } else {
                    ((PlaylistsListAdapter) listView.getAdapter()).updateData(data);
                }
                break;
            case 1:
                if(listView.getAdapter() == null) {
                    listView.setAdapter(new SongsListAdapter(getContext(), data));
                } else {
                    ((SongsListAdapter) listView.getAdapter()).updateData(data);
                }
                break;
            case 2:
                if(listView.getAdapter() == null) {
                    listView.setAdapter(new AlbumsListAdapter(getContext(), data));
                } else {
                    ((AlbumsListAdapter) listView.getAdapter()).updateData(data);
                }
                break;
            case 3:
                if(listView.getAdapter() == null) {
                    listView.setAdapter(new ArtistsListAdapter(getContext(), data));
                } else {
                    ((ArtistsListAdapter) listView.getAdapter()).updateData(data);
                }
                break;
            case 4:
                if(listView.getAdapter() == null) {
                    listView.setAdapter(new CategoriesListAdapter(getContext(), data));
                } else {
                    ((CategoriesListAdapter) listView.getAdapter()).updateData(data);
                }
                break;
        }
    }

    public int getmCurrentPage() {
        return mCurrentPage;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Send the event to the host activity
        if(mListener != null) {
            mListener.onListItemSelected(l, v, position, id, mCurrentPage);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onListItemSelected(ListView l, View v, int position, long id, int page);
    }
}
