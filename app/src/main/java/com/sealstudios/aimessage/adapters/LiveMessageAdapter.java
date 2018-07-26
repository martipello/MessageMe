package com.sealstudios.aimessage.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.florent37.shapeofview.ShapeOfView;
import com.sealstudios.aimessage.MessageListActivity;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.R;
import com.sealstudios.aimessage.Utils.Constants;
import com.vanniktech.emoji.EmojiInformation;
import com.vanniktech.emoji.EmojiTextView;
import com.vanniktech.emoji.EmojiUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LiveMessageAdapter extends RecyclerView.Adapter<LiveMessageAdapter.MyViewHolder> {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_CALL_SENT = 3;
    private static final int VIEW_TYPE_CALL_RECEIVED = 4;
    private ArrayList<DatabaseMessage> messageList;
    private FrameLayout fade;
    private ImageView holderImage;
    private MessageListActivity.OnItemTouchListener onItemTouchListener;
    private Context context;
    private String userId;
    private Date dateCheck;
    private SparseBooleanArray selectedItems;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public EmojiTextView message_text;
        public TextView time_stamp;
        public ImageView holderImage;
        public ImageView statusImage;
        public ConstraintLayout holder;
        public View sentReceived;
        public View fade;
        public ProgressBar progressBar;
        public ShapeOfView shapeOfView;
        public ConstraintLayout dateHolder;
        public TextView bigDateText;

        public MyViewHolder(View view) {
            super(view);
            message_text = view.findViewById(R.id.text_message_body);
            time_stamp = view.findViewById(R.id.text_message_time);
            fade = view.findViewById(R.id.fade);
            statusImage = view.findViewById(R.id.call_status);
            progressBar = view.findViewById(R.id.progress);
            holderImage = view.findViewById(R.id.image_view);
            shapeOfView = view.findViewById(R.id.image_view_holder);
            holder = view.findViewById(R.id.holder);
            sentReceived = view.findViewById(R.id.send_received);
            dateHolder = view.findViewById(R.id.date_holder);
            bigDateText = view.findViewById(R.id.big_date_text);
            Linkify.addLinks(message_text, Linkify.ALL);
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemTouchListener.onCardClick(v, getAdapterPosition());
                }
            });
            holder.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemTouchListener.onCardLongClick(v, getAdapterPosition());
                    return false;
                }
            });
        }
    }

    public LiveMessageAdapter(ArrayList<DatabaseMessage> messageList,
                              Context context,
                              MessageListActivity.OnItemTouchListener onItemTouchListener,
                              SparseBooleanArray selectedItems, String userId) {
        this.messageList = messageList;
        this.onItemTouchListener = onItemTouchListener;
        this.context = context;
        this.selectedItems = selectedItems;
        this.userId = userId;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.received_message_holder, parent, false);
            return new MyViewHolder(itemView);
        } else if(viewType == VIEW_TYPE_MESSAGE_SENT) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_message_holder, parent, false);
            return new MyViewHolder(itemView);
        } else if(viewType == VIEW_TYPE_CALL_RECEIVED) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.call_received_message_holder, parent, false);
            return new MyViewHolder(itemView);
        } else if(viewType == VIEW_TYPE_CALL_SENT) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.call_received_message_holder, parent, false);
            return new MyViewHolder(itemView);
        }
        itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.received_message_holder, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        DatabaseMessage message = messageList.get(position);
        if (message.getSenderId().equals(userId)) {
            // If the current user is the sender of the message
            if (message.getData_type().equals(Constants.DATA_TYPE_TEXT)){
                    return VIEW_TYPE_MESSAGE_SENT;
            }else if (message.getData_type().equals(Constants.DATA_TYPE_CALL)){
                    return VIEW_TYPE_CALL_SENT;
            }
        } else {
            if (message.getData_type().equals(Constants.DATA_TYPE_TEXT)){
                    return VIEW_TYPE_MESSAGE_RECEIVED;
            }else if (message.getData_type().equals(Constants.DATA_TYPE_CALL)){
                    return VIEW_TYPE_CALL_RECEIVED;
            }
        }
        return VIEW_TYPE_MESSAGE_RECEIVED;
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            //view.setBackgroundColor(Color.WHITE);
        } else {
            selectedItems.put(pos, true);
            //view.setBackgroundColor(Color.BLUE);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public ArrayList<DatabaseMessage> getSelectedItems() {
        ArrayList<DatabaseMessage> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(messageList.get(selectedItems.keyAt(i)));
        }
        return items;
    }

    public void refreshMyList(ArrayList<DatabaseMessage> list) {
        this.messageList.clear();
        this.messageList.addAll(list);
        this.notifyDataSetChanged();
    }

    public ArrayList<DatabaseMessage> getList() {
        return messageList;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final DatabaseMessage userMessage = messageList.get(position);
        holder.statusImage.setVisibility(View.GONE);
        holder.message_text.setVisibility(View.VISIBLE);
        holder.dateHolder.setVisibility(View.GONE);
        holder.time_stamp.setVisibility(View.VISIBLE);
        holder.shapeOfView.setVisibility(View.GONE);
        holder.sentReceived.setBackground(context.getResources().getDrawable(R.drawable.circle_white));
        holder.bigDateText.setText("");
        holder.fade.setVisibility(View.INVISIBLE);
        final int size = R.dimen.emoji_size_default;
        holder.message_text.setEmojiSizeRes(size, false);

        if (position == 0) {
            //first load show date
            dateCheck = userMessage.getTime_stamp();
            holder.dateHolder.setVisibility(View.VISIBLE);
            holder.bigDateText.setText(getTextDateHolder(dateCheck.getTime()));
            holder.time_stamp.setVisibility(View.VISIBLE);
            holder.time_stamp.setText(getSmsTodayYestFromMilli(userMessage.getTime_stamp().getTime()));
        } else {
            //get time and date from message
            Calendar messageTime = Calendar.getInstance();
            messageTime.setTimeInMillis(userMessage.getTime_stamp().getTime());
            // get time and from date check
            Calendar dateCheckTime = Calendar.getInstance();
            if (dateCheck == null){
                dateCheck = userMessage.getTime_stamp();
            }
            dateCheckTime.setTimeInMillis(dateCheck.getTime());
            //check if they are the same
            if (datesAreEqual(messageTime, dateCheckTime)) {
                holder.dateHolder.setVisibility(View.GONE);
                holder.bigDateText.setText("");
            } else {
                dateCheck = userMessage.getTime_stamp();
            }
            if (timesAreEqual(messageTime, dateCheckTime)) {
                holder.time_stamp.setVisibility(View.GONE);
            } else {
                holder.time_stamp.setVisibility(View.VISIBLE);
                holder.time_stamp.setText(getSmsTodayYestFromMilli(userMessage.getTime_stamp().getTime()));
            }
        }
        //message text length
        if (userMessage.getMessage().length() < 1) {
            holder.message_text.setVisibility(View.GONE);
        } else if (userMessage.getMessage().length() >= 1 && userMessage.getMessage().length() < 10) {
            holder.message_text.setTextSize(22f);
        } else {
            holder.message_text.setTextSize(14f);
        }
        //check the type of message
        //message is an image
        if (userMessage.getData_type().equals(Constants.DATA_TYPE_IMAGE)) {
            holder.shapeOfView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(userMessage.getData_url())
                    .apply(new RequestOptions()
                            .dontAnimate().placeholder(R.drawable.placeholder).centerCrop())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                            return false;
                        }
                    })
                    .into(holder.holderImage);
            //message is a text message
        } else if (userMessage.getData_type().equals(Constants.DATA_TYPE_TEXT)){
            holder.shapeOfView.setVisibility(View.GONE);
            holder.message_text.setText(userMessage.getMessage());
            //message is a call message
        } else if (userMessage.getData_type().equals(Constants.DATA_TYPE_CALL)){
            holder.shapeOfView.setVisibility(View.GONE);
            String callState = "";
            switch (userMessage.getMessage()){
                case Constants.CALL_RECEIVED:
                    callState = context.getString(R.string.call_received);
                    holder.statusImage.setImageResource(R.drawable.baseline_call_received_24);
                    break;
                case Constants.CALL_MADE:
                    callState = context.getString(R.string.call_made);
                    holder.statusImage.setImageResource(R.drawable.baseline_call_made_24);
                    break;
                case Constants.CALL_REJECTED:
                    callState = context.getString(R.string.call_missed);
                    holder.statusImage.setImageResource(R.drawable.baseline_call_made_24);
                    break;
                case Constants.CALL_MISSED:
                    callState = context.getString(R.string.call_missed);
                    holder.statusImage.setImageResource(R.drawable.baseline_call_missed_24);
                    break;
            }
            holder.message_text.setText(callState);
        }

        switch (userMessage.getSent_received()) {
            case 0:
                holder.sentReceived.setBackground(context.getResources().getDrawable(R.drawable.circle_white));
                break;
            case 1:
                holder.sentReceived.setBackground(context.getResources().getDrawable(R.drawable.circle_grey));
                break;
            case 2:
                holder.sentReceived.setBackground(context.getResources().getDrawable(R.drawable.circle_blue));
                break;
        }
        EmojiInformation emojiInformation = EmojiUtils.emojiInformation(userMessage.getMessage());
        int res;
        if (emojiInformation.emojis.size() > 0) {
            if (emojiInformation.isOnlyEmojis && emojiInformation.emojis.size() == 1) {
                res = R.dimen.emoji_size_single_emoji;
            } else if (emojiInformation.isOnlyEmojis && emojiInformation.emojis.size() > 1) {
                res = R.dimen.emoji_size_only_emojis;
            } else {
                res = R.dimen.emoji_size_default;
            }
            holder.message_text.setEmojiSizeRes(res, false);
        }
        if (selectedItems.get(position, false)) {
            holder.fade.setVisibility(View.VISIBLE);
        } else {
            holder.fade.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String getSmsTodayYestFromMilli(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Current time
        Calendar now = Calendar.getInstance();

        final String strTimeFormate = "h:mm aa";
        final String strDateFormate = "dd/MM/yyyy h:mm aa";

        if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                && ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                && ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {

            return DateFormat.format(strTimeFormate, messageTime).toString();

        } else if (
                ((now.get(Calendar.DATE) - messageTime.get(Calendar.DATE)) == 1)
                        && ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                        && ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {
            return DateFormat.format(strTimeFormate, messageTime).toString();
        } else {
            return DateFormat.format(strDateFormate, messageTime).toString();
        }
    }

    private String getTextDateHolder(long msgTimeMillis) {

        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(msgTimeMillis);
        // get Currunt time
        Calendar now = Calendar.getInstance();

        final String strDateFormate = "dd/MM/yyyy";

        if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                && ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                && ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {

            return "TODAY";

        } else if (
                ((now.get(Calendar.DATE) - messageTime.get(Calendar.DATE)) == 1)
                        && ((now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                        && ((now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                ) {
            return "YESTERDAY";
        } else {
            return "" + DateFormat.format(strDateFormate, messageTime);
        }
    }

    private boolean timesAreEqual(Calendar dateCheckTime, Calendar messageTime) {
        boolean areEqual = false;
        if (dateCheckTime.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                && ((dateCheckTime.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                && ((dateCheckTime.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))
                && (dateCheckTime.get(Calendar.HOUR) == messageTime.get(Calendar.HOUR))
                && dateCheckTime.get(Calendar.MINUTE) == messageTime.get(Calendar.MINUTE)) {
            //make sure this is gone if they are the same
            areEqual = true;
        }
        return areEqual;
    }


    private boolean datesAreEqual(Calendar messageTime, Calendar dateCheckTime) {
        boolean areEqual = false;
        if (dateCheckTime.get(Calendar.DATE) == messageTime.get(Calendar.DATE)
                && ((dateCheckTime.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH)))
                && ((dateCheckTime.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)))) {
            //make sure this is gone if they are the same
            areEqual = true;
        }
        return areEqual;
    }
}
