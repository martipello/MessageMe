package com.sealstudios.aimessage.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.telecom.Call;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.sealstudios.aimessage.ContactsActivity;
import com.sealstudios.aimessage.Database.CallObject;
import com.sealstudios.aimessage.Database.ContactRepository;
import com.sealstudios.aimessage.Database.DatabaseCalls;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.Database.LiveDatabaseBuilder;
import com.sealstudios.aimessage.Database.LiveDatabaseContactsDao;
import com.sealstudios.aimessage.Database.LiveDbOpenHelper;
import com.sealstudios.aimessage.R;
import com.sealstudios.aimessage.Utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.MODE_PRIVATE;

public class LiveCallsAdapter extends RecyclerView.Adapter<LiveCallsAdapter.MyViewHolder>{
    private ArrayList<CallObject> callList = new ArrayList<>();
    private ContactsActivity.OnItemTouchListener onItemTouchListener;
    private ContactsActivity.OnImageTouchListener onImageTouchListener;
    private Context context;
    private String userId;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView contact_name,recent_time;
        public CircleImageView contact_image;
        public ImageButton call_back;
        public ImageView call_status;
        public ConstraintLayout holder;

        public MyViewHolder(View view) {
            super(view);
            contact_name = view.findViewById(R.id.contact_name);
            contact_image = view.findViewById(R.id.contact_image);
            recent_time = view.findViewById(R.id.message_time);
            call_back = view.findViewById(R.id.call_back);
            call_status = view.findViewById(R.id.call_status);
            contact_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onImageTouchListener.onImageClick(v, getPosition());
                }
            });
            call_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemTouchListener.onCardClick(v, getPosition());
                }
            });
            call_back.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemTouchListener.onCardLongClick(v, getPosition());
                    return false;
                }
            });
        }
    }


    public LiveCallsAdapter(ArrayList<CallObject> callList, Context context, ContactsActivity.OnItemTouchListener onItemTouchListener, ContactsActivity.OnImageTouchListener onImageTouchListener){
        this.callList = callList;
        this.onItemTouchListener = onItemTouchListener;
        this.context = context;
        this.onImageTouchListener = onImageTouchListener;
        SharedPreferences pref = context.getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        userId = pref.getString(Constants.FS_ID, "id");
    }

    public ArrayList<CallObject> getList(){
        return this.callList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.call_holder, parent, false);
        return new MyViewHolder(itemView);
    }

    public void refreshMyList(ArrayList<CallObject> list){
        this.callList.clear();
        this.callList.addAll(list);
        this.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        CallObject callObject = callList.get(position);
        holder.contact_name.setText(callObject.getCall().getCall_caller_name());
        holder.recent_time.setText(getSmsTodayYestFromMilli(callObject.getCall().getCall_time_stamp().getTime()));
        switch (callObject.getCall().getCall_status()){
            case Constants.CALL_MISSED :
                holder.call_status.setImageResource(R.drawable.baseline_call_missed_24);
                break;
            case Constants.CALL_RECEIVED :
                if (callObject.getCall().getCall_caller_id().equals(userId)){
                    holder.call_status.setImageResource(R.drawable.baseline_call_made_24);
                }else{
                    holder.call_status.setImageResource(R.drawable.baseline_call_received_24);
                }
                break;
            case Constants.CALL_REJECTED :
                holder.call_status.setImageResource(R.drawable.baseline_call_missed_24);
                break;
        }
        Glide.with(context)
                .load(callObject.getProfileImage())
                .apply(new RequestOptions()
                        .dontAnimate().placeholder(R.drawable.contact_placeholder))
                .into(holder.contact_image);

    }

    public void refreshSingleItem(int position){
        notifyItemChanged(position);
    }

    private String getSmsTodayYestFromMilli(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Currunt time
        Calendar now = Calendar.getInstance();

        final String strTimeFormate = "h:mm aa";
        final String strDateFormate = "dd/MM/yyyy h:mm aa";

        if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                &&
                ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                &&
                ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {

            return "today at " + DateFormat.format(strTimeFormate, messageTime);

        } else if (
                ((now.get(Calendar.DATE) - messageTime.get(Calendar.DATE)) == 1)
                        &&
                        ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                        &&
                        ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {
            return "yesterday at " + DateFormat.format(strTimeFormate, messageTime);
        } else {
            return DateFormat.format(strDateFormate, messageTime).toString();
        }
    }

    @Override
    public int getItemCount(){
        if (callList != null)
            return callList.size();
        else return 0;
    }
}

