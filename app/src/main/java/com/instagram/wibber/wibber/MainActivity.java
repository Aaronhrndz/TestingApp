package com.instagram.wibber.wibber;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class MainActivity extends Activity implements OnClickListener {

    private Button btnUpload, btnNotificate;
    private ImageView downloadedImg;
    private ProgressDialog simpleWaitDialog;
    private Bitmap userBitmap;
    private String downloadUrl = "https://powr.s3-us-west-1.amazonaws.com/app_images/resizable/92b9a9e1-8190-4aea-9079-67353f4f95a3/Beever_1000.jpg";


    private static final int PICK_IMAGE = 100;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initInterface();

    }

    private void initInterface(){
        setContentView(R.layout.main_interface);
        setWidgetReference();
        bindEventHandlers();
    }

    private void bindEventHandlers() {
        btnUpload.setOnClickListener(this);
        btnNotificate.setOnClickListener(this);
    }

    private void setWidgetReference() {
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnNotificate = (Button) findViewById(R.id.btnNotificate);
    }

    public void onClick(View v) {
        if (v == btnUpload) {
            upload();
        } if (v == btnNotificate){
            new ImageDownloader().execute(downloadUrl);
            downloadedImg = (ImageView) findViewById(R.id.imageView);
        }
    }

    private void upload() {
        selectImage();
    }

    private void createInstagramIntent(String type, Uri imageUri){
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(type);
        share.putExtra(Intent.EXTRA_STREAM, imageUri);
        share.setPackage("com.instagram.android");
        startActivity(share);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
    return Uri.parse(path);
    }

    private void selectImage(){
        Uri imageUri = getImageUri(MainActivity.this, userBitmap);
        String type = "image/*";
        createInstagramIntent(type, imageUri);
    }

   /* private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            String type = "image/*";
            createInstagramIntent(type, imageUri);
        }
    }*/

    private void notificate(){
        NotificationCompat.BigPictureStyle bpStyle = new NotificationCompat.BigPictureStyle();
        bpStyle.bigPicture(userBitmap);
        int mNotificationId = 001;
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent rIntent = new Intent(getPackageManager().getLaunchIntentForPackage("com.instagram.wibber.wibber"));
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, mNotificationId, rIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MainActivity.this)
                                    .setSmallIcon(R.mipmap.icon)
                                    .setContentTitle("Big Picture Notification Example")
                                    .addAction(android.R.drawable.ic_menu_send, "Publicar", pendingIntent)
                                    .setStyle(bpStyle)
                                    .setAutoCancel(true)
                                    .setOngoing(false)
                                    .setLights(11796735, 1000, 500)
                                    .setPriority(NotificationCompat.PRIORITY_MAX);
        mBuilder.build().flags |= Notification.FLAG_AUTO_CANCEL;
        mBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(mNotificationId, mBuilder.build());
    }


    private class ImageDownloader extends AsyncTask<String, String, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... param) {
            // TODO Auto-generated method stub
            return downloadBitmap(param[0]);
        }

        @Override
        protected void onPreExecute() {
            Log.i("Async-Example", "onPreExecute Called");
            simpleWaitDialog = ProgressDialog.show(MainActivity.this,
                    "Wait", "Downloading Image");

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Log.i("Async-Example", "onPostExecute Called");
            downloadedImg.setImageBitmap(result);
            simpleWaitDialog.dismiss();
            notificate();
        }

        private Bitmap downloadBitmap(String url) {
            // initilize the default HTTP client object
            final DefaultHttpClient client = new DefaultHttpClient();

            //forming a HttoGet request
            final HttpGet getRequest = new HttpGet(url);
            try {

                HttpResponse response = client.execute(getRequest);

                //check 200 OK for success
                final int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    Log.w("ImageDownloader", "Error " + statusCode +
                            " while retrieving bitmap from " + url);
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = null;
                    try {
                        // getting contents from the stream
                        inputStream = entity.getContent();

                        // decoding stream data back into image Bitmap that android understands
                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        userBitmap = bitmap;

                        return bitmap;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            } catch (Exception e) {
                // You Could provide a more explicit error message for IOException
                getRequest.abort();
                Log.e("ImageDownloader", "Something went wrong while" +
                        " retrieving bitmap from " + url + e.toString());
            }

            return null;
        }
    }

}
