package com.sealstudios.aimessage.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.IntentPickerSheetView;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.sealstudios.aimessage.Database.DatabaseMessage;
import com.sealstudios.aimessage.R;
import com.sealstudios.aimessage.Utils.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CustomPagerAdapter extends PagerAdapter {

    private Context mContext;
    private ArrayList<DatabaseMessage> userMessages;
    private String src;
    private Resources res;
    private Future<Bitmap> task;
    private FirebaseFirestore db;
    private String stringRef = Constants.STORAGE_REF;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    public CustomPagerAdapter(Context context, ArrayList<DatabaseMessage> userMessages) {
        mContext = context;
        this.userMessages = userMessages;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        res = mContext.getResources();
        final int[] imageHeight = new int[1];
        final int[] imageWidth = new int[1];
        //CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        //ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.image_layout, collection,false);
        //ImageView imageView = (ImageView) layout.findViewById(R.id.imageView);
        BottomSheetLayout bottomSheet = layout.findViewById(R.id.bottomsheet);
        PhotoView photoView = layout.findViewById(R.id.photo_view);
        ImageButton share = layout.findViewById(R.id.share);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //share intent
                downloadImage(userMessages.get(position),bottomSheet);
            }
        });
        ImageButton detail = layout.findViewById(R.id.detail);
        Glide.with(mContext.getApplicationContext())
                .asBitmap()
                .load(userMessages.get(position).getData_url())
                .apply(new RequestOptions().centerCrop().placeholder(R.drawable.placeholder))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                Transition<? super Bitmap> transition) {
                        imageWidth[0] = bitmap.getWidth();
                        imageHeight[0] = bitmap.getHeight();
                        photoView.setImageBitmap(bitmap);
                    }
                });

        collection.addView(layout);
        detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get details and display them
                String sentReceived;
                if (userMessages.get(position).getSent_received() == 2){
                    sentReceived = mContext.getResources().getString(R.string.received);
                }else{
                    sentReceived = mContext.getResources().getString(R.string.sent);
                }
                new MaterialDialog.Builder(mContext)
                        .title(userMessages.get(position).getTime_stamp().toString())
                        .content(String.format(res.getString(R.string.message_detail),
                                userMessages.get(position).getSenderName(),
                                userMessages.get(position).getMessage(),
                                getSmsTodayYestFromMilli(userMessages.get(position).getTime_stamp().getTime()),
                                String.valueOf(imageHeight[0]),//get image height
                                String.valueOf(imageWidth[0]),//get Image width
                                sentReceived,
                                userMessages.get(position).getData_url()))
                        //<string name="message_detail">  \n Received : %s \n Url : %s </string>
                        .positiveText(res.getString(R.string.ok))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        //return CustomPagerEnum.values().length;
        return userMessages.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        //return mContext.getString(customPagerEnum.getTitleResId());
        return userMessages.get(position).getSenderId();
    }

    public String[] getPageDetail(int position) {
        //CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        //return mContext.getString(customPagerEnum.getTitleResId());
        return new String[]{userMessages.get(position).getSenderId(),getSmsTodayYestFromMilli(userMessages.get(position).getTime_stamp().getTime()),userMessages.get(position).getData_url()};

    }

    private void customBottomSheetShare(Intent shareIntent, BottomSheetLayout bottomSheet){

        IntentPickerSheetView intentPickerSheet = new IntentPickerSheetView(mContext, shareIntent, R.string.share_with, new IntentPickerSheetView.OnIntentPickedListener() {
            @Override
            public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                bottomSheet.dismissSheet();
                mContext.startActivity(activityInfo.getConcreteIntent(shareIntent));
            }
        });
// Filter out built in sharing options such as bluetooth and beam.
        /*
        intentPickerSheet.setFilter(new IntentPickerSheetView.Filter() {
            @Override
            public boolean include(IntentPickerSheetView.ActivityInfo info) {
                return !info.componentName.getPackageName().startsWith("com.android");
            }
        });
        */
// Sort activities in reverse order for no good reason except that whatsapp will come out on top
        intentPickerSheet.setSortMethod(new Comparator<IntentPickerSheetView.ActivityInfo>() {
            @Override
            public int compare(IntentPickerSheetView.ActivityInfo lhs, IntentPickerSheetView.ActivityInfo rhs) {
                return rhs.label.compareTo(lhs.label);
            }
        });
        bottomSheet.showWithSheetView(intentPickerSheet);
    }

    public void downloadImage(DatabaseMessage message, BottomSheetLayout bottomSheet){
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl(message.getData_url());
        MaterialDialog materialDialog;
        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext)
                .title(R.string.Please_wait)
                .content("Downloading")
                .progress(true, 0)
                .cancelable(false);
        materialDialog = builder.build();
        materialDialog.show();
        try {
            final File localFile = File.createTempFile(message.getSenderId(), ".png", mContext.getExternalCacheDir());
            localFile.setReadable(true);
            storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    materialDialog.dismiss();
                    Uri photoUri = FileProvider.getUriForFile(mContext,mContext.getPackageName() + ".message_me.provider",new File(localFile.getAbsolutePath()));
                    shareStuff(photoUri,bottomSheet);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Snackbar.make(bottomSheet,"Failed to send this image",Snackbar.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {

                }
            });
        } catch (IOException e) {
        }
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
            return "date : " + DateFormat.format(strDateFormate, messageTime);
        }
    }


    private void shareStuff(Uri uri, BottomSheetLayout bottomSheet){

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //sendIntent.putExtra(Intent.EXTRA_TEXT, "New Picture");
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("image/*");
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContext.startActivity(sendIntent);
        }else {
            customBottomSheetShare(sendIntent , bottomSheet);
        }
    }
}