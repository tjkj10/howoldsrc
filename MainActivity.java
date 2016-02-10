package com.example.liujing.howold;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;


public class MainActivity extends Activity implements View.OnClickListener {
    private static final int PICK_CODE = 0x110;
    private static final int TAKE_PHOTO = 0x113;
    private ImageView imageView;
    private Button getButton;
    private Button decButton;
    private Button camButton;
    private TextView textview;
    private View mWaitting;
    private String mCurrentPhotoStr;
    private Bitmap mphotimage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initviews();
        initEvent();
    }

    private void initEvent() {
        getButton.setOnClickListener(this);
        decButton.setOnClickListener(this);
        camButton.setOnClickListener(this);
    }

    private void initviews() {
        imageView = (ImageView) findViewById(R.id.id_imageview);
        getButton = (Button) findViewById(R.id.id_getimage);
        decButton = (Button) findViewById(R.id.id_detect);
        camButton = (Button) findViewById(R.id.id_camera);
        textview = (TextView) findViewById(R.id.id_text);
        mWaitting = findViewById(R.id.id_waiting);

    }

    private static final int MSG_SUCESS = 0X111;
    private static final int MSG_ERROR = 0X112;

    public Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SUCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject obj = (JSONObject) msg.obj;
                    prepareRsBitmap(obj);
                    imageView.setImageBitmap(mphotimage);
                    break;
                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errMsg)) {
                        textview.setText("error!");
                    } else {
                        textview.setText(errMsg);
                    }
                    break;
            }
        }
    };

    private void prepareRsBitmap(JSONObject obj) {
        Bitmap bitmap = Bitmap.createBitmap(mphotimage.getWidth(), mphotimage.getHeight(), mphotimage.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mphotimage, 0, 0, null);
        Paint mPaint = new Paint();
        mPaint.setColor(Color.rgb(255,114,86));
        mPaint.setStrokeWidth(10);
        try {
            JSONArray faces = obj.getJSONArray("face");
            int faceCount = faces.length();
            textview.setText("find " + faceCount);
            for (int i = 0; i < faceCount; i++) {
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();

                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();

                canvas.drawLine(x - w / 2, y + h / 2, x - w / 2, y - h / 2, mPaint);
                canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);
                canvas.drawLine(x + w / 2, y + h / 2, x + w / 2, y - h / 2, mPaint);

                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String sex = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap agebitmap = buildAgeBitmap(age, "Male".equals(sex));

                int ageWith = agebitmap.getWidth();
                int ageHeight = agebitmap.getHeight();

                if (bitmap.getWidth() < mphotimage.getWidth() && bitmap.getHeight() < mphotimage.getHeight()) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / mphotimage.getWidth(),
                            bitmap.getHeight() * 1.0f / mphotimage.getHeight());
                    agebitmap = Bitmap.createScaledBitmap(agebitmap, (int) (ageWith * ratio),
                            (int) (ageHeight * ratio), false);

                }
                canvas.drawBitmap(agebitmap, x - agebitmap.getWidth() / 2,
                        y - h / 2 - agebitmap.getHeight(), null);
                mphotimage = bitmap;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) mWaitting.findViewById(R.id.id_age_and_gender);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }

        tv.setDrawingCacheEnabled(true);
        tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }

    private Uri imageUri;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_getimage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                break;

            case R.id.id_camera:
                File outputImage = new File(Environment.getExternalStorageDirectory(), "output_image.jpg");
                if (outputImage.exists()) {
                    outputImage.delete();
                }
                try {
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage);
                Intent intentcamrea = new Intent("android.media.action.IMAGE_CAPTURE");
                //intentcamrea.setDataAndType(imageUri,"image/*");
                intentcamrea.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                //intentcamrea.putExtra("scale",true);
                startActivityForResult(intentcamrea, TAKE_PHOTO);


                break;


            case R.id.id_detect:
                mWaitting.setVisibility(View.VISIBLE);

                if (mCurrentPhotoStr != null && !mCurrentPhotoStr.trim().equals("")) {
                    resizePhoto();
                } else {
                    mphotimage = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
                }
                FaceppDetect.detect(mphotimage, new FaceppDetect.callBack() {
                    @Override
                    public void sucess(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCESS;
                        msg.obj = result;
                        mhandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getErrorMessage();
                        mhandler.sendMessage(msg);
                    }
                });
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_CODE) {
            if (intent != null) {
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cursor.getString(index);
                //Log.e("TAG",mCurrentPhotoStr+"*********");
                cursor.close();
                resizePhoto();
                imageView.setImageBitmap(mphotimage);
                textview.setText("click--->detect");
            }
        }
        if (requestCode == TAKE_PHOTO) {
            mCurrentPhotoStr =imageUri.getPath();
            //Log.e("TAG",imageUri.getPath());
                resizePhoto();
                imageView.setImageBitmap(mphotimage);
                textview.setText("click--->detect");
        }
    }


    /*
     *压缩图片
     * 这里我们可以使用BitmapFactory.decodeFile(mCurrentPhotoStr)方法将一个图片路径转换为一个Bitmap
     * 类型的数据供程序使用
     *
     * options.inJustDecodeBounds = true;将此属性设置为true的时候我们就可以在内存加载图片时只加载图片的宽高,
     * 避免内存溢出
     *
     * ratio为给图片设定一个压缩值,然后通过 options.inSampleSize = (int) Math.ceil(ratio);将图片进行压缩.
     *
     * options.inJustDecodeBounds = false;这里将图片属性设置为false之后我们在让图片可以正常加载到内存之中,
     * mphotimage = BitmapFactory.decodeFile(mCurrentPhotoStr, options);同样适用这个方法就可以把图片
     * 进行加载了
     *
     */
    private void resizePhoto() {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoStr, options);
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mphotimage = BitmapFactory.decodeFile(mCurrentPhotoStr, options);
    }
}
