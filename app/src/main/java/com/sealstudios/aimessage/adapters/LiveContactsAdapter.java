package com.sealstudios.aimessage.adapters;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.sealstudios.aimessage.ContactsActivity;
import com.sealstudios.aimessage.Database.DatabaseContacts;
import com.sealstudios.aimessage.R;

import java.util.ArrayList;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class LiveContactsAdapter extends RecyclerView.Adapter<LiveContactsAdapter.MyViewHolder>{
    private ArrayList<DatabaseContacts> contactList = new ArrayList<>();
    private ContactsActivity.OnItemTouchListener onItemTouchListener;
    private ContactsActivity.OnImageTouchListener onImageTouchListener;
    private Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView contact_name,contact_status,unread_count_text,recent_time;
        public CircleImageView contact_image;
        public View unread_holder;
        public ConstraintLayout holder , badge_layout;

        public MyViewHolder(View view) {
            super(view);
            contact_name = view.findViewById(R.id.contact_name);
            contact_status = view.findViewById(R.id.contact_status);
            contact_image = view.findViewById(R.id.contact_image);
            unread_count_text = view.findViewById(R.id.unread_count_text);
            unread_holder = view.findViewById(R.id.unread_count_badge);
            badge_layout = view.findViewById(R.id.unread_holder);
            recent_time = view.findViewById(R.id.message_time);
            contact_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onImageTouchListener.onImageClick(v, getPosition());
                }
            });
            holder = view.findViewById(R.id.holder);
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemTouchListener.onCardClick(v, getPosition());
                }
            });
            holder.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemTouchListener.onCardLongClick(v, getPosition());
                    return false;
                }
            });
        }
    }


    public LiveContactsAdapter(ArrayList<DatabaseContacts> contactList, Context context, ContactsActivity.OnItemTouchListener onItemTouchListener, ContactsActivity.OnImageTouchListener onImageTouchListener){
        this.contactList = contactList;
        this.onItemTouchListener = onItemTouchListener;
        this.context = context;
        this.onImageTouchListener = onImageTouchListener;
    }

    public ArrayList<DatabaseContacts> getList(){
        return this.contactList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_holder, parent, false);
        return new MyViewHolder(itemView);
    }

    public void refreshMyList(ArrayList<DatabaseContacts> list){
        this.contactList.clear();
        this.contactList.addAll(list);
        this.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        DatabaseContacts userObject = contactList.get(position);
        holder.contact_name.setText(userObject.getUser_name());
        if (userObject.getUser_recent_message()!= null){
            holder.recent_time.setText(getTimeDate(userObject.getMsg_time_stamp().getTime()));
            holder.contact_status.setText(userObject.getUser_recent_message());
        }else{
            holder.recent_time.setText(getTimeDate(userObject.getUser_time_stamp().getTime()));
            holder.contact_status.setText(userObject.getUser_status());
        }

        if (userObject.getUnread() > 0){
            holder.unread_holder.setVisibility(View.VISIBLE);
            holder.unread_count_text.setVisibility(View.VISIBLE);
            holder.badge_layout.setVisibility(View.VISIBLE);
            holder.unread_count_text.setText(String.valueOf(userObject.getUnread()));
        }else{
            holder.unread_count_text.setText("");
            holder.unread_holder.setVisibility(View.GONE);
            holder.unread_count_text.setVisibility(View.GONE);
            holder.badge_layout.setVisibility(View.GONE);
        }
        Glide.with(context)
                .load(userObject.getUser_image())
                .apply(new RequestOptions()
                        .dontAnimate().placeholder(R.drawable.contact_placeholder))
                .into(holder.contact_image);

    }

    public void refreshSingleItem(int position){
        notifyItemChanged(position);
    }

    private String getTimeDate(long msgTimeMillis) {

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

            return DateFormat.format(strTimeFormate, messageTime).toString();

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
        if (contactList != null)
            return contactList.size();
        else return 0;
    }
}

