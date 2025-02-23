package com.example.agendaapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaapp.Data.Platform;
import com.example.agendaapp.Platforms.GoogleClassroom;
import com.example.agendaapp.RecyclerAdapters.ImportRecyclerAdapter;
import com.example.agendaapp.Utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class ImportFragment extends Fragment {

    // TODO: Use action view for add (search for platform with MaterialAutoCompleteTextView)

    public static final String SHARED_PREFERENCES_KEY = "Import Shared Preferences";
    public static final String SP_PLATFORM_JSON = "Shared Preferences Platform JSON";

    public static final String JSON_SAVE_PLATFORM_NAME = "JSON Save Platform Name";
    public static final String JSON_SAVE_AUTH_TOKEN = "JSON Save Auth Token";
    public static final String JSON_SAVE_PROFILE_PIC_URL = "JSON Save Profile Pic URL";

    private Context context;

    private RecyclerView recyclerView;

    private ActionMode actionMode;
    private ActionMode.Callback actionModeCallback;

    public static List<Platform> platforms;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstance) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.import_toolbar);
        toolbar.setTitle(getString(R.string.import_title));
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        setHasOptionsMenu(true);

        init(view);

        initCallbacks();

        return view;
    }

    /**
     * Inits the views (constructs)
     * @param view The inflated fragment
     */
    private void init(View view) {
        context = getContext();

        recyclerView = (RecyclerView) view.findViewById(R.id.import_recycler_view);

        actionMode = null;
        actionModeCallback = null;

        platforms = getSavedPlatforms(context, getActivity());

        for(Platform p : platforms)
            getLifecycle().addObserver(p);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new ImportRecyclerAdapter(this, (ArrayList<Platform>) platforms));
    }

    /**
     * Inits callback methods (ex. onClick)
     */
    private void initCallbacks() {
        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.menu_import_delete, menu);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.import_delete) {
                    SelectionTracker<Long> tracker = ((ImportRecyclerAdapter) recyclerView.getAdapter()).getSelectionTracker();

                    TreeSet<Integer> remove = new TreeSet<Integer>();

                    for (Long l : tracker.getSelection())
                        remove.add(Math.toIntExact(l));

                    for(Iterator i = remove.descendingIterator(); i.hasNext(); ) {
                        int pos = (int) i.next();

                        platforms.remove(pos);
                    }

                    recyclerView.getAdapter().notifyDataSetChanged();

                    tracker.clearSelection();

                    return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                ((ImportRecyclerAdapter) recyclerView.getAdapter()).getSelectionTracker().clearSelection();
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_import, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home :
                FragmentTransaction homeTransaction = getParentFragmentManager().beginTransaction();
                homeTransaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_right);
                homeTransaction.replace(R.id.fragment_container, MainActivity.homeFragment);
                homeTransaction.addToBackStack(Utility.HOME_FRAGMENT);
                homeTransaction.commit();

                return true;
            case R.id.import_add :
                FragmentTransaction addTransaction = getParentFragmentManager().beginTransaction();
                addTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                addTransaction.replace(R.id.fragment_container, new PlatformSelectFragment());
                addTransaction.addToBackStack(Utility.PLATFORM_SELECT_FRAGMENT);
                addTransaction.commit();

                return true;
        }

        return false;
    }

    /**
     * Show the contextual app bar (deleting platforms from the import list)
     */
    public void showContextualAppBar() {
        actionMode = getActivity().startActionMode(actionModeCallback);
        actionMode.setTitle(getString(R.string.import_delete_title));
    }

    /**
     * Hides the contextual app bar (deleting platforms from the import list)
     */
    public void hideContextualAppBar() {
        actionMode.finish();
    }

    /**
     * Saves the platforms to SharedPreferences
     * @param context The fragment context
     */
    public static void savePlatforms(Context context) {
        JSONArray save = new JSONArray();

        for(Platform p : platforms) {
            JSONObject object = new JSONObject();

            try {
                object.put(JSON_SAVE_PLATFORM_NAME, p.getPlatformName());
                object.put(JSON_SAVE_AUTH_TOKEN, p.getToken());

                object.put(JSON_SAVE_PROFILE_PIC_URL, p.getAccountIconURL());
            } catch(JSONException e) {
                Log.e("SAVE ERROR", "Unable to write to JSON");
            }

            save.put(object);
        }

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(SP_PLATFORM_JSON, save.toString());
        editor.apply();
    }

    /**
     * Gets the platform list from shared preferences
     * @param context The context
     * @param activity The activity to be passed in to potentially use to init a Platform
     * @return Returns the list of saved platforms from the json in shared prefs
     */
    public static List<Platform> getSavedPlatforms(Context context, Activity activity) {
        ArrayList<Platform> platforms = new ArrayList<Platform>();

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        String json = preferences.getString(SP_PLATFORM_JSON, "[]");

        try {
            JSONArray array = new JSONArray(json);

            for(int i = 0; i < array.length(); i++) {
                JSONObject o = (JSONObject) array.get(i);

                String name = o.getString(JSON_SAVE_PLATFORM_NAME);
                String authToken = o.getString(JSON_SAVE_AUTH_TOKEN);
                String profilePicURL = o.getString(JSON_SAVE_PROFILE_PIC_URL);

                Platform p = getPlatformFromName(context, name, activity);
                p.setAuthToken(authToken);
                p.setAccountIconURL(profilePicURL);

                p.checkAuthTokenValid();

                platforms.add(p);
            }
        } catch(JSONException e) {
            Log.e("SAVE ERROR", "Unable to parse save json");
        }

        return platforms;
    }

    /**
     * Gets the list of platforms in the list that are actually signed into
     * @return The list of platforms signed into
     */
    public static List<Platform> getSignedInPlatforms() {
        List<Platform> signedIn = platforms;

        for(int i = signedIn.size() - 1; i >= 0; i--) {
            if(signedIn.get(i).getToken().equals(""))
                signedIn.remove(i);
        }

        return signedIn;
    }

    /**
     * Get Platform from the platform name
     * @param context The fragment context
     * @param name The platform name
     * @param activity The fragment's parent activity
     * @return The correct Platform object
     */
    public static Platform getPlatformFromName(Context context, String name, Activity activity) {
        if(name.equals(context.getString(R.string.google_classroom)))
            return new GoogleClassroom(activity);

        return null;
    }

    @Override
    public void onPause() {
        savePlatforms(context);

        super.onPause();
    }
}