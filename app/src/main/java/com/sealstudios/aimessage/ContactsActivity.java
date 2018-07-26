package com.sealstudios.aimessage;

import android.app.SearchManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.sealstudios.aimessage.adapters.LiveContactsStatusAdapter;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity{
    private RecyclerView recyclerView;
    private LiveContactsStatusAdapter contactsAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<DatabaseContacts> userObjectArrayList;
    private String mSearchString;
    private TextView results;
    private final int databaseLoader = 100;
    private String userId;
    private ContactsViewModel contactViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        SharedPreferences pref = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
        results = findViewById(R.id.results);
        recyclerView = findViewById(R.id.contacts_list_view);
        ImageView imageView = findViewById(R.id.background);
        Glide.with(this).load(R.drawable.message_background)
                .apply(new RequestOptions().centerCrop()).into(imageView);
        //ask for permission first
        mSearchString = "";
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        userObjectArrayList = new ArrayList<>();
        OnItemTouchListener itemTouchListener = new ContactsActivity.OnItemTouchListener() {
            @Override
            public void onCardClick(View view, int position) {
                DatabaseContacts user = contactsAdapter.getList().get(position);
                Bundle b = new Bundle();
                //b.putParcelable("user", user);
                b.putString(Constants.FS_NAME, user.getUser_name());
                b.putString(Constants.FS_ID, user.getUser_id());
                Intent i = new Intent(ContactsActivity.this, MessageListActivity.class);
                i.putExtras(b);
                startActivity(i);
            }

            @Override
            public void onCardLongClick(View view, int position) {
                //clearGrid();
            }
        };
        ContactsActivity.OnImageTouchListener imageTouchListener = new ContactsActivity.OnImageTouchListener() {
            @Override
            public void onImageClick(View view, int position) {
                DatabaseContacts user = contactsAdapter.getList().get(position);
                showEditDialog(userId, user.getUser_id());
            }
        };
        contactsAdapter = new LiveContactsStatusAdapter(userObjectArrayList, this, itemTouchListener, imageTouchListener);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(contactsAdapter);
        //getFromDatabase();
        contactViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        contactViewModel.getAllContacts().observe(this, new Observer<List<DatabaseContacts>>() {
            @Override
            public void onChanged(@Nullable List<DatabaseContacts> databaseContacts) {
                ArrayList<DatabaseContacts> tempList = new ArrayList<>();
                tempList.addAll(databaseContacts);
                contactsAdapter.refreshMyList(tempList);
                if (tempList.size() < 1) {
                    results.setVisibility(View.VISIBLE);
                } else {
                    results.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showEditDialog(String userId, String contactId) {
        FragmentManager fm = this.getSupportFragmentManager();
        ContactPreviewFragment contactPreviewFragment = ContactPreviewFragment.newInstance(userId, contactId);
        contactPreviewFragment.show(fm, "fragment_contact");
    }
    /*
    private void getFromDatabase() {
        getSupportLoaderManager().initLoader(databaseLoader, null, dataLoaderListener);
    }

    private android.support.v4.app.LoaderManager.LoaderCallbacks<ArrayList<ContactObject>> dataLoaderListener
            = new android.support.v4.app.LoaderManager.LoaderCallbacks<ArrayList<ContactObject>>() {
        @NonNull
        @Override
        public Loader onCreateLoader(int id, @Nullable Bundle args) {
            //return new databaseChecker(getActivity(), args);
            return new databaseLoader(ContactsActivity.this);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<ArrayList<ContactObject>> loader, ArrayList<ContactObject> data) {
            setContacts(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader loader) {

        }
    };

    public static class databaseLoader extends
            AsyncTaskLoader<ArrayList<ContactObject>> {
        // You probably have something more complicated
        // than just a String. Roll with me
        private ArrayList<ContactObject> mData;

        public databaseLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            if (mData != null) {
                // Use cached data
                deliverResult(mData);
            } else {
                // load fresh data
                forceLoad();
            }
        }

        @Override
        public ArrayList<ContactObject> loadInBackground() {
            DbOpenHelper dbContactOpenHelper = DatabaseBuilder.getContactDatabase(this.getContext());
            List<DatabaseContacts> dbUser = dbContactOpenHelper.contactDao().getAll();
            ArrayList<ContactObject> databaseUsers = new ArrayList<>();
            for (DatabaseContacts databaseContacts : dbUser) {
                ContactObject userObject = new ContactObject(
                        databaseContacts.getUser_name(),
                        databaseContacts.getUser_status(),
                        databaseContacts.getUser_id(),
                        databaseContacts.getUser_number(),
                        databaseContacts.getUser_image(),
                        databaseContacts.getUser_image(),
                        databaseContacts.getUser_time_stamp(),
                        databaseContacts.getUser_recent_message(),
                        databaseContacts.getMsg_time_stamp(),
                        "",
                        databaseContacts.getBlocked(),
                        databaseContacts.getUnread()
                );
                databaseUsers.add(userObject);
            }
            return databaseUsers;
        }

        @Override
        public void deliverResult(ArrayList<ContactObject> data) {
            // We’ll save the data for later retrieval
            mData = data;
            super.deliverResult(data);
        }
    }
    */

    private void searchDatabase(String searchString) {
        contactViewModel.setUserName(searchString);
    }

    /*
    private void setContacts(ArrayList<ContactObject> users) {
        contactsAdapter.refreshMyList(users);
        if (users.size() < 1) {
            results.setVisibility(View.VISIBLE);
        } else {
            results.setVisibility(View.GONE);
        }
    }
    */
    public interface OnItemTouchListener {
        void onCardClick(View view, int position);

        void onCardLongClick(View view, int position);
    }

    public interface OnImageTouchListener {
        void onImageClick(View view, int position);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mSearchString = newText;
                searchDatabase(mSearchString);
                return false;
            }
        });
        return true;
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
