package com.sealstudios.aimessage;

import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.sealstudios.aimessage.Widget.MessageMeAppWidget;
import com.sealstudios.aimessage.adapters.LiveContactsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConversationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView results;
    private LiveContactsAdapter contactsAdapter;
    private LinearLayoutManager linearLayoutManager;
    private String mSearchString, userId;
    private FrameLayout smallContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ContactsViewModel contactViewModel;

    public ConversationsFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_conversations, container, false);
        setHasOptionsMenu(true);
        recyclerView = rootView.findViewById(R.id.contacts_list_view);
        results = rootView.findViewById(R.id.results);
        userId = MainActivity.userId;
        smallContainer = rootView.findViewById(R.id.small_container);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        ContactsActivity.OnItemTouchListener itemTouchListener = new ContactsActivity.OnItemTouchListener() {
            @Override
            public void onCardClick(View view, int position) {
                DatabaseContacts user = contactsAdapter.getList().get(position);
                Bundle b = new Bundle();
                b.putString(Constants.FS_NAME, user.getUser_name());
                b.putString(Constants.FS_ID, user.getUser_id());
                Intent i = new Intent(getActivity(), MessageListActivity.class);
                i.putExtras(b);
                startActivity(i);
            }
            @Override
            public void onCardLongClick(View view, int position) {
            }
        };
        ContactsActivity.OnImageTouchListener imageTouchListener = new ContactsActivity.OnImageTouchListener() {
            @Override
            public void onImageClick(View view, int position) {
                DatabaseContacts user = contactsAdapter.getList().get(position);
                showEditDialog(userId, user.getUser_id());
            }
        };
        ArrayList<DatabaseContacts> databaseContactsArrayList = new ArrayList<>();
        contactsAdapter = new LiveContactsAdapter(databaseContactsArrayList, getActivity(), itemTouchListener, imageTouchListener);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(contactsAdapter);
        mSearchString = "%%";
        contactViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        contactViewModel.setUserName(mSearchString);
        contactViewModel.getLiveContactList().observe(this, new Observer<List<DatabaseContacts>>() {
            @Override
            public void onChanged(@Nullable List<DatabaseContacts> databaseContacts) {
                ArrayList<DatabaseContacts> tempList = new ArrayList<>();
                int unread = 0;
                tempList.addAll(databaseContacts);
                contactsAdapter.refreshMyList(tempList);
                if (tempList.size() < 1) {
                    results.setVisibility(View.VISIBLE);
                } else {
                    results.setVisibility(View.GONE);
                    for (DatabaseContacts databaseContacts1 : tempList){
                        if (databaseContacts1.getUnread() > 0){
                            unread += databaseContacts1.getUnread();
                        }
                    }
                    ((MainActivity) Objects.requireNonNull(getActivity())).setUnreadCount(unread);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity().getApplicationContext());
                    Intent intent = new Intent(getActivity().getApplicationContext(), MessageMeAppWidget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getActivity().getApplication(),MessageMeAppWidget.class));
                    appWidgetManager.notifyAppWidgetViewDataChanged(ids,R.id.widget_list);
                    //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    //sendBroadcast(intent);
                    for (int id : ids){
                        MessageMeAppWidget.updateAppWidget(getActivity().getApplicationContext(),appWidgetManager,id);
                    }
                }
            }
        });
        swipeRefreshLayout = rootView.findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((MainActivity) getActivity()).restartMyLoader();
                swipeRefreshLayout.
                        setRefreshing(false);
            }
        });

        return rootView;
    }

    private void setUnread(){

    }

    private void showEditDialog(String userId, String contactId) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        ContactPreviewFragment contactPreviewFragment = ContactPreviewFragment.newInstance(userId, contactId);
        contactPreviewFragment.show(fm, "fragment_contact");
    }


    private void searchDatabase(String searchString) {
        contactViewModel.setUserName("%"+searchString+"%");
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
