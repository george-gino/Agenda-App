/**
 * Fragment for selecting new platforms to add to the import list
 */

package com.example.agendaapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaapp.Data.Platform;
import com.example.agendaapp.Data.PlatformInfo;
import com.example.agendaapp.RecyclerAdapters.PlatformSelectRecyclerAdapter;
import com.example.agendaapp.Utils.Utility;

import java.util.ArrayList;
import java.util.HashMap;

public class PlatformSelectFragment extends Fragment {

    private Context context;

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstance) {
        View view = inflater.inflate(R.layout.fragment_platform_select, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.select_toolbar);
        toolbar.setTitle("Import");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        setHasOptionsMenu(true);

        init(view);

        return view;
    }

    /**
     * Inits the views (constructs)
     * @param view The inflated fragment
     */
    public void init(View view) {
        context = getContext();

        recyclerView = (RecyclerView) view.findViewById(R.id.select_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new PlatformSelectRecyclerAdapter(context,
                new PlatformInfo(getResources().getDrawable(R.drawable.ic_google_classroom_32dp), getString(R.string.google_classroom))));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_select_platform, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home :
                FragmentTransaction homeTransaction = getParentFragmentManager().beginTransaction();
                homeTransaction.replace(R.id.fragment_container, new ImportFragment());
                homeTransaction.addToBackStack(Utility.IMPORT_FRAGMENT);
                homeTransaction.commit();

                return true;
            case R.id.select_done :
                addPlatforms(((PlatformSelectRecyclerAdapter) recyclerView.getAdapter()).getSelectedPlatforms());

                FragmentTransaction doneTransaction = getParentFragmentManager().beginTransaction();
                doneTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                doneTransaction.replace(R.id.fragment_container, new ImportFragment());
                doneTransaction.addToBackStack(Utility.IMPORT_FRAGMENT);
                doneTransaction.commit();

                return true;
        }

        return false;
    }

    /**
     * Adds to the list of platforms
     * @param platformMap Map of platforms with the name and number of that platform to add
     *                    (ex. Google Classroom, 4)
     */
    public void addPlatforms(HashMap<String, Integer> platformMap) {
        for(String key : platformMap.keySet()) {
            int index = 0;

            while(index < ImportFragment.platforms.size() && ImportFragment.platforms.get(index).getPlatformName().compareTo(key) <= 0)
                index++;

            for(int i = 0; i < platformMap.get(key); i++) {
                Platform p = ImportFragment.getPlatformFromName(context, key, getActivity());

                ImportFragment.platforms.add(index, p);
            }
        }

        ImportFragment.savePlatforms(context);
    }
}
