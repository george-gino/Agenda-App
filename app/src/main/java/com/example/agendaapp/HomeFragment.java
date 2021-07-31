/**
 * This is the fragment that holds the RecyclerView.
 *
 * @uthor Joshua Au
 * @version 1.0
 * @since 6/24/2020
 */

package com.example.agendaapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewGroupCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaapp.Data.Assignment;
import com.example.agendaapp.Data.DateInfo;
import com.example.agendaapp.Data.Platform;
import com.example.agendaapp.Platform.GoogleClassroom;
import com.example.agendaapp.RecyclerAdapters.AssignmentRecyclerAdapter;
import com.example.agendaapp.Utils.DateUtils;
import com.example.agendaapp.Utils.ItemMoveCallback;
import com.example.agendaapp.Data.ListModerator;
import com.example.agendaapp.Data.SaveInfo;
import com.example.agendaapp.Data.Serialize;
import com.example.agendaapp.Utils.Utility;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.Hold;
import com.google.android.material.transition.MaterialElevationScale;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private Context context;

    private BottomAppBar bottomAppBar;
    private FloatingActionButton fab;

    private RecyclerView recyclerView;
    private AssignmentRecyclerAdapter recyclerViewAdapter;

    // ArrayList of priority assignments
    public static ArrayList<Assignment> priority;
    // ArrayList of upcoming assignments
    public static ArrayList<Assignment> upcoming;
    // ListModerator for the priority and upcoming array s
    public static ListModerator<Assignment> assignmentModerator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstance) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.home_toolbar);
        toolbar.setTitle(getString(R.string.home_title));

        ((AppCompatActivity) getActivity()).setSupportActionBar((BottomAppBar) view.findViewById(R.id.bottom_app_bar));

        setHasOptionsMenu(true);

        getParentFragmentManager().setFragmentResultListener(Utility.HOME_RESULT_KEY, this,
                new ResultListener());

        init(view, onSavedInstance);

        initListeners();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle onSavedInstance) {
        ViewGroupCompat.setTransitionGroup(view.findViewById(R.id.home_ll_root), true);

        // setExitTransition(new MaterialElevationScale(false)); // can be laggy
        setExitTransition(new Hold());
        setReenterTransition(new MaterialElevationScale(true));

        postponeEnterTransition();

        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();

                return true;
            }
        });
    }

    /**
     * Inits the views
     * @param view Inflated Fragment
     */
    private void init(View view, Bundle onSavedInstance) {
        context = getContext();

        initArrays(onSavedInstance);

        bottomAppBar = (BottomAppBar) view.findViewById(R.id.bottom_app_bar);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        recyclerView = (RecyclerView) view.findViewById(R.id.home_recycler_view);

        setArrayAdapter();

        ItemTouchHelper.Callback callback = new ItemMoveCallback((ItemMoveCallback.ItemTouchHelperContract) recyclerViewAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(recyclerViewAdapter);

        update();
    }

    /**
     * Inits the priority and upcoming arrays
     */
    private void initArrays(Bundle onSavedInstance) {
        ArrayList[] serialized = (ArrayList[]) Serialize.deserialize(context.getFilesDir() + "/" + Utility.SERIALIZATION_ASSIGNMENT_FILE);

        if(serialized != null) {
            priority = serialized[Utility.SERIALIZATION_PRIORITY];
            upcoming = serialized[Utility.SERIALIZATION_UPCOMING];
        } else {
            priority = new ArrayList<Assignment>();
            upcoming = new ArrayList<Assignment>();
        }

        assignmentModerator = new ListModerator<Assignment>(priority, upcoming);
    }

    /**
     * Initializes the onClickListeners
     */
    private void initListeners() {
        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch(item.getItemId()) {
                case R.id.home_import :
                    setExitTransition(null);

                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_left);
                    transaction.replace(R.id.fragment_container, new ImportFragment());
                    transaction.addToBackStack(Utility.IMPORT_FRAGMENT);
                    transaction.commit();

                    return true;
            }

            return false;
        });

        fab.setOnClickListener(view -> {
            setExitTransition(null);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_left);
            transaction.replace(R.id.fragment_container, EditFragment.newInstance(context));
            transaction.addToBackStack(Utility.EDIT_FRAGMENT);
            transaction.commit();
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Updates the lists
     */
    private void update() {
        // TODO: HOW DO THE ARRAYS SAVE AFTER UPDATE?

        //TODO: DOESN'T ACTUALLY WORK? MAKE SURE TO REMOVE UPCOMING ASSIGNMENT AND SAVE???

        // Move from upcoming to priority if necessary
        for(int i = 0; i < upcoming.size(); i++) {
            if(DateUtils.inPriorityRange(context, upcoming.get(i).getDateInfo())) {
                int pos = addToList(priority, upcoming.remove(i));
                recyclerViewAdapter.notifyItemMoved(i + 2 + priority.size(), pos);
            }
        }

        if(ImportFragment.platforms == null)
            ImportFragment.platforms = ImportFragment.getSavedPlatforms(context, getActivity());

        for(Platform p : ImportFragment.platforms) {
            p.getNewAssignments(assignments -> {
                for(Assignment a : assignments) {
                    int pos = 0;

                    if(DateUtils.inPriorityRange(context, a.getDateInfo()))
                        pos = addToList(priority, a);
                    else
                        pos = addToList(upcoming, a);

                    recyclerViewAdapter.notifyItemInserted(pos);
                }

                Utility.serializeArrays(context, priority, upcoming);
            });
        }

        Utility.serializeArrays(context, priority, upcoming);
    }

    /**
     * Sets the array adapter for the RecyclerView
     */
    private void setArrayAdapter() {
        recyclerViewAdapter = new AssignmentRecyclerAdapter(context, priority, upcoming);
    }

    /**
     * Updates the array adapter with new arrays
     */
    private void updateArrayAdapter() {
        recyclerViewAdapter.setArrays(priority, upcoming);
    }

    /**
     * Serializes the arrays
     * @param bundle The bundle to save from
     */
    private void save(Bundle bundle) {
        SaveInfo info = bundle.getParcelable(Utility.SAVE_INFO);

        if(!info.getCreateNew())
            assignmentModerator.removeOverall(info.getPosition());

        if(info.getIsPriority())
            addToList(priority, info.getAssignment());
        else
            addToList(upcoming, info.getAssignment());

        Utility.serializeArrays(context, priority, upcoming);

        updateArrayAdapter();
    }

    /**
     * Adds an assignment to a specified list
     * @param list The list to be added to
     * @param assignment The assignment to add
     * @return Returns the position of the assignment in the list
     */
    public int addToList(List<Assignment> list, Assignment assignment) {
        for(int i = 0; i < list.size(); i++) {
            DateInfo fromArray = list.get(i).getDateInfo();

            boolean moved = false;

            if (i != list.size() - 1)
                moved = DateUtils.compareDates(fromArray, list.get(i + 1).getDateInfo()) == DateInfo.FURTHER;

            if (!moved && DateUtils.compareDates(fromArray, assignment.getDateInfo()) == DateInfo.FURTHER) {
                list.add(i, assignment);

                return i;
            }
        }

        list.add(assignment);

        return list.size() - 1;
    }

    /*
      For Fragment communication
    */
    class ResultListener implements FragmentResultListener {
        @Override
        public void onFragmentResult(String key, Bundle bundle) {
            save(bundle);
        }
    }
}