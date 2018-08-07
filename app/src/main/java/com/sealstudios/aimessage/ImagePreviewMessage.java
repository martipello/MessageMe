package com.sealstudios.aimessage;


import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.Utils.Constants;
import com.sealstudios.aimessage.ViewModels.ContactsViewModel;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class ImagePreviewMessage extends DialogFragment {

    private CircleImageView send;
    private EmojiEditText messageTextView;
    private ImageView pictureImageView;
    private ImageButton emojiBtn;
    private ConstraintLayout rootLayout;
    private EmojiPopup emojiPopup;
    private Uri uri;
    private String TAG = "emoji keyboard";

    public ImagePreviewMessage() {
        // Required empty public constructor
    }
    public static ImagePreviewMessage newInstance(String messageText , Uri uri) {
        ImagePreviewMessage frag = new ImagePreviewMessage();
        Bundle args = new Bundle();
        args.putString(Constants.MSG_TEXT, messageText);
        args.putString(Constants.MSG_DATA_URL, uri.toString());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // creating the fullscreen dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return dialog;
    }

    @Override
    public void onViewCreated(View view,@Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);
        final Bundle b;
        rootLayout = view.findViewById(R.id.root_layout);
        pictureImageView = view.findViewById(R.id.image_view);
        emojiBtn = view.findViewById(R.id.add_emoji);
        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiPopup.toggle();
            }
        });
        messageTextView = view.findViewById(R.id.message_text);
        if (getArguments() != null) {
            b = getArguments();
            messageTextView.setText(b.getString(Constants.MSG_TEXT));
            uri = Uri.parse(b.getString(Constants.MSG_DATA_URL));
            Glide.with(getActivity().getApplicationContext())
                    .load(uri)
                    .apply(new RequestOptions().centerInside())
                    .into(pictureImageView);
        }
        send = view.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {@Override
            public void onClick(View v) {
                DatabaseMessage databaseMessage = ((MessageListActivity) getActivity())
                        .createMessage(messageTextView.getText().toString(),Constants.DATA_TYPE_IMAGE);
                ((MessageListActivity) getActivity()).validateMessage(databaseMessage, uri);
                dismiss();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.send_picture_preview, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootLayout)
                .setOnEmojiBackspaceClickListener(ignore -> Log.d(TAG, "Clicked on Backspace"))
                .setOnEmojiClickListener((ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                .setOnEmojiPopupShownListener(() -> emojiBtn.setImageResource(R.drawable.baseline_keyboard_black_24))
                .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                .setOnEmojiPopupDismissListener(() -> emojiBtn.setImageResource(R.drawable.baseline_mood_black_24))
                .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                .build(messageTextView);
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
