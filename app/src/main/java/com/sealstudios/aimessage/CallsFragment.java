package com.sealstudios.aimessage;



import android.app.SearchManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sealstudios.aimessage.Database.CallObject;
import com.sealstudios.aimessage.Database.DatabaseCalls;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.CallsViewModel;
import com.sealstudios.aimessage.adapters.LiveCallsAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CallsFragment extends Fragment {
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private TextView results;
    private Context mContext;
    private LiveCallsAdapter adapter;
    private CallsViewModel callsViewModel;
    private String mSearchString, userId;
    private FrameLayout smallContainer;

    public CallsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calls, container, false);
        setHasOptionsMenu(true);
        mContext = getContext();
        setHasOptionsMenu(true);
        recyclerView = rootView.findViewById(R.id.calls_list_view);
        results = rootView.findViewById(R.id.results);
        userId = MainActivity.userId;
        mSearchString = "";
        smallContainer = rootView.findViewById(R.id.small_container);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        ContactsActivity.OnItemTouchListener itemTouchListener = new ContactsActivity.OnItemTouchListener() {
            @Override
            public void onCardClick(View view, int position) {
                CallObject call = adapter.getList().get(position);
                DatabaseCalls newCall = new DatabaseCalls();
                //launch a video call
                if ((getActivity()) != null) {
                    ((MainActivity)getActivity()).makeCall(view , call.getCall().getCall_caller_id());
                    if (userId.equals(call.getCall().getCall_caller_id())){
                        //we were the caller and will be again
                        //this new call will be the same
                        newCall.setCall_caller_id(call.getCall().getCall_caller_id());
                        newCall.setCall_caller_name(call.getCall().getCall_caller_name());
                        newCall.setCall_called_id(call.getCall().getCall_called_id());
                        newCall.setCall_called_name(call.getCall().getCall_called_name());
                    }else{
                        //we were not the caller but will be this time
                        newCall.setCall_caller_id(call.getCall().getCall_called_id());
                        newCall.setCall_caller_name(call.getCall().getCall_called_name());
                        newCall.setCall_called_id(call.getCall().getCall_caller_id());
                        newCall.setCall_called_name(call.getCall().getCall_caller_name());
                    }
                    newCall.setCall_id(String.valueOf(new Date().getTime()));
                    newCall.setCall_status(Constants.CALL_MADE);
                    newCall.setCall_time_stamp(new Date());

                    callsViewModel.insertMyCall(newCall);
                }
            }
            @Override
            public void onCardLongClick(View view, int position) {
            }
        };
        ContactsActivity.OnImageTouchListener imageTouchListener = new ContactsActivity.OnImageTouchListener() {
            @Override
            public void onImageClick(View view, int position) {
                CallObject call = adapter.getList().get(position);
                showEditDialog(userId, call.getCall().getCall_caller_id());
            }
        };
        ArrayList<CallObject> databaseCallsArrayList = new ArrayList<>();
        adapter = new LiveCallsAdapter(databaseCallsArrayList, getActivity(), itemTouchListener, imageTouchListener);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        callsViewModel = ViewModelProviders.of(this).get(CallsViewModel.class);
        callsViewModel.getCallsWithImage().observe(this, new Observer<List<CallObject>>() {
            @Override
            public void onChanged(@Nullable List<CallObject> databaseCalls) {
                ArrayList<CallObject> tempList = new ArrayList<>();
                tempList.addAll(databaseCalls);
                adapter.refreshMyList(tempList);
                if (tempList.size() < 1) {
                    results.setVisibility(View.VISIBLE);
                } else {
                    results.setVisibility(View.GONE);
                }
            }
        });
        return rootView;

    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void showEditDialog(String userId, String contactId) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        ContactPreviewFragment contactPreviewFragment = ContactPreviewFragment.newInstance(userId, contactId);
        contactPreviewFragment.show(fm, "fragment_contact");
    }


    private void searchDatabase(String searchString) {
        callsViewModel.setUserName(searchString);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchDatabase(newText);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.preferences) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
