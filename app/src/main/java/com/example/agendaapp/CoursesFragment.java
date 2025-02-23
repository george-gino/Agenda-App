/**
 * Fragment to view all the classes
 */

package com.example.agendaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agendaapp.Data.Course;
import com.example.agendaapp.Data.Platform;
import com.example.agendaapp.RecyclerAdapters.CoursesRecyclerAdapter;
import com.example.agendaapp.Utils.Utility;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CoursesFragment extends Fragment {

    /*
        Course list will be in the form of:

        {
            <courseId>: {
                            subject: (String)
                            // can add in image file path later
                            image: (String - file path)
                        }
        }
     */
    public final static String COURSES_SHARED_PREFERENCES = "Courses Shared Preferences";
    public final static String COURSE_LIST = "Course List";

    public final static String JSON_SUBJECT_NAME = "JSON Subject Name";

    private Context context;

    private RecyclerView recyclerView;
    private CoursesRecyclerAdapter recyclerAdapter;

    public static List<Course> courseList;
    public static JSONObject jsonCourseList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle onSavedInstance) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.courses_toolbar);
        toolbar.setTitle(getString(R.string.courses_title));
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        setHasOptionsMenu(true);

        init(view);

        return view;
    }

    /**
     * Inits the views
     * @param view The inflated fragment layout
     */
    private void init(View view) {
        context = getContext();

        recyclerView = (RecyclerView) view.findViewById(R.id.courses_recycler_view);

        courseList = new ArrayList<Course>();

        initRecyclerAdapter();
    }

    /**
     * Inits the recycler view adapter
     */
    private void initRecyclerAdapter() {
        getCourseList(context, courseList -> {
            CoursesFragment.courseList = courseList;

            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new CoursesRecyclerAdapter(context, courseList));
        });
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
        }

        return false;
    }

    /**
     * Gets and updates the course JSON and adds to a List<Course>
     * @param listener The listener for when the list has finished being processed
     */
    public static void getCourseList(Context context, CoursesProcessedListener listener) {
        SharedPreferences pref = context.getSharedPreferences(COURSES_SHARED_PREFERENCES, Context.MODE_PRIVATE);

        JSONObject old = null;

        try {
            old = new JSONObject(pref.getString(COURSE_LIST, "{}"));
        } catch (JSONException e) {
            old = new JSONObject();
        }

        List<Course> newCourseList = new ArrayList<Course>();
        jsonCourseList = new JSONObject();

        List<Platform> platforms = ImportFragment.getSignedInPlatforms();

        AtomicInteger n = new AtomicInteger(0);

        for(int i = 0; i < platforms.size(); i++) {
            Platform p = platforms.get(i);

            JSONObject finalOld = old;

            p.getCourses(courseList -> {
                for(Course c : courseList) {
                    JSONObject jsonCourse = finalOld.optJSONObject(c.getCourseId());
                    String subject = Utility.getSubject(context, Utility.POSITION_OTHER);

                    try {
                        if(jsonCourse != null)
                            subject = jsonCourse.optString(JSON_SUBJECT_NAME);

                        jsonCourseList.put(c.getCourseId(), subject);
                        newCourseList.add(new Course(c.getCourseId(), p.getPlatformName(), c.getCourseName(),
                                subject, AppCompatResources.getDrawable(context, Utility.getSubjectDrawable(context, subject))));
                    } catch(JSONException e) {
                        Log.e("Error at course JSON", e.toString());

//                        Snackbar.make(getActivity().findViewById(android.R.id.content), context.getString(R.string.import_error), Snackbar.LENGTH_SHORT).show();
                    }
                }

                if(n.incrementAndGet() >= platforms.size())
                    listener.onCoursesProcessed(newCourseList);
            });
        }
    }

    /**
     * Saves the selection to the shared pref in the form of a JSON
     *
     */
    private void saveCoursesList() {
        JSONObject o = new JSONObject();

        for(Course c : courseList) {
            try {
                o.put(c.getCourseId(), new JSONObject().put(JSON_SUBJECT_NAME, c.getCourseSubject()));
            } catch(JSONException e) {
                Log.e("Error saving courseList", e.toString());

                Snackbar.make(getActivity().findViewById(android.R.id.content), getString(R.string.save_error), Snackbar.LENGTH_SHORT).show();
            }
        }

        SharedPreferences pref = context.getSharedPreferences(COURSES_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(COURSE_LIST, o.toString());
        editor.apply();
    }

    /**
     * Gets the subject of the imported course from the save JSON
     * @param context The context
     * @param courseId The course id of the imported assignment
     * @return The subject
     */
    public static String getSubject(Context context, String courseId) {
        JSONObject o = jsonCourseList.optJSONObject(courseId);

        if(o == null)
            return Utility.getSubject(context, Utility.POSITION_OTHER);

        return o.optString(JSON_SUBJECT_NAME);
    }

    @Override
    public void onPause() {
        saveCoursesList();
        Utility.serializeArrays(context, HomeFragment.priority, HomeFragment.upcoming);

        super.onPause();
    }

    /**
     * An interface for the onCoursesProcessed() method
     */
    public interface CoursesProcessedListener {
        /**
         * Callback method for when the courses have been fully retrieved and processed
         */
        public void onCoursesProcessed(List<Course> courseList);
    }
}
