package com.sealstudios.aimessage;


import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class ContactPreviewFragment extends DialogFragment {

    private CircleImageView icon;
    private TextView profileName, profileStatus, numberText, recentMessage, blocked;
    private SwitchCompat blockSwitch;
    private Button close;
    private String userId;
    private String contactId;
    private Resources res;
    private FirebaseFirestore db;
    private final static int loader = 101;
    private ContactsViewModel contactViewModel;

    public ContactPreviewFragment() {
        // Required empty public constructor
    }
    public static ContactPreviewFragment newInstance(String userId,String contactId) {
        ContactPreviewFragment frag = new ContactPreviewFragment();
        Bundle args = new Bundle();
        args.putString(Constants.USER_NAME, userId);
        args.putString(Constants.DB_USER_NAME, contactId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setStyle(STYLE_NO_TITLE, 0);
        return dialog;
    }

    @Override
    public void onViewCreated(View view,@Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);

        db = FirebaseFirestore.getInstance();
        res = getResources();
        final Bundle b;
        if (getArguments() != null) {
            b = getArguments();
            userId = b.getString(Constants.USER_NAME);
            contactId = b.getString(Constants.DB_USER_NAME);
        }
        profileName = view.findViewById(R.id.field_phone_name);
        profileStatus = view.findViewById(R.id.status_text);
        numberText = view.findViewById(R.id.phone_number);
        recentMessage = view.findViewById(R.id.recent_message);
        blocked = view.findViewById(R.id.blocked);
        icon = view.findViewById(R.id.icon);
        close = view.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close this fragment
                dismiss();
            }
        });
        blockSwitch = view.findViewById(R.id.block_switch);
        blockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Map<String, Object> blockMap = new HashMap<>();
                blockMap.put(Constants.FS_BLOCKED, isChecked);
                DocumentReference messageSenderRef = db.collection(Constants.USERS)
                        .document(userId).collection(Constants.CONTACTS).document(contactId);
                messageSenderRef.update(blockMap);
                DocumentReference messageRecipientRef = db.collection(Constants.USERS)
                        .document(contactId).collection(Constants.CONTACTS).document(userId);
                messageRecipientRef.update(blockMap);
                if (isChecked) {
                    blocked.setText(String.format(res.getString(R.string.blocked), ""));
                    Snackbar.make(buttonView, String.format(res.getString(R.string.blocked), ""), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(buttonView, String.format(res.getString(R.string.blocked), "not"), Snackbar.LENGTH_SHORT).show();
                    blocked.setText(String.format(res.getString(R.string.blocked), "not"));
                }
            }
        });
        contactViewModel = ViewModelProviders.of(this).get(ContactsViewModel.class);
        contactViewModel.getContactById(contactId).observe(this, new Observer<DatabaseContacts>() {
            @Override
            public void onChanged(@Nullable DatabaseContacts databaseContacts) {
                populateViews(databaseContacts);
            }
        });
        //getContact();
    }

    public void populateViews(DatabaseContacts data){
        profileName.setText(String.format(res.getString(R.string.contact_name), data.getUser_name()));
        profileStatus.setText(String.format(res.getString(R.string.contact_status), data.getUser_status()));
        numberText.setText(data.getUser_number());
        recentMessage.setText(String.format(res.getString(R.string.contact_recent_messages), data.getUser_recent_message()));
        if (data.getBlocked()) {
            blockSwitch.setChecked(true);
        } else {
            blockSwitch.setChecked(false);
        }
        Glide.with(getActivity())
                .load(data.getUser_image())
                .apply(new RequestOptions()
                        .dontAnimate().placeholder(R.drawable.contact_placeholder))
                .into(icon);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.contact_preview, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_main_blank, menu);

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
