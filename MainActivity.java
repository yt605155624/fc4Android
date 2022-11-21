package com.example.xiaotiantian.fc4android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;
    private Uri imageUri;
    private static final String TAG = "MainActivity";
    //private TextView txt;
    private ImageView imageViewToProcess;
    private Bitmap bitmaptoProcess;
    private Bitmap initbitmap;
    private PredictionFC4 pfc4;
    private int scaleHeight = 512;
    private boolean contrast;
    private ProgressBar mProgressBar;
    private float[] result;
    private TextView mProgressBar_txt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //txt = (TextView) findViewById(R.id.txt_id);
        imageViewToProcess = (ImageView) findViewById(R.id.picture);
        bitmaptoProcess = BitmapFactory.decodeResource(getResources(), R.drawable.img0881_small);
        bitmaptoProcess = changeBitmapSize(bitmaptoProcess);
        initbitmap = bitmaptoProcess;
        imageViewToProcess.setImageBitmap(bitmaptoProcess);
        pfc4 = new PredictionFC4(getAssets());
        contrast = true;
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar_txt=(TextView) findViewById(R.id.progress_bar_txt);
        mProgressBar.setVisibility(View.GONE);
        mProgressBar_txt.setVisibility(View.GONE);

    }


    public void processClick(View v) {

        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar_txt.setVisibility(View.VISIBLE);

        AsyncTask task = new AsyncTask<Object, Integer, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                if (isCancelled()) {
                    return;
                }
            }

            @Override
            protected Void doInBackground(Object... voids) {
                publishProgress();
                result = pfc4.getAddResult(bitmaptoProcess);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String res = "预测结果为：";

                        for (int i = 0; i < result.length; i++) {
                            res = res + String.valueOf(result[i]) + " ";
                        }
                        //txt.setText(res);
                        Log.i("MainActivity", String.valueOf(res));
                        Bitmap bitmap = ColorConstancy(bitmaptoProcess, result);
                        //bitmap=changeBitmapSize(bitmap);
                        bitmaptoProcess = bitmap;
                        imageViewToProcess.setImageBitmap(bitmaptoProcess);
                        mProgressBar.setVisibility(View.GONE);
                        mProgressBar_txt.setVisibility(View.GONE);
                    }
                });

            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        };
        task.execute();


    }

    private Bitmap changeBitmapSize(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newHeight;
        int newWidth;
        Log.e("width", "width:" + width);
        Log.e("height", "height:" + height);//设置想要的大小
        if (width<height)
        {
            newWidth = scaleHeight;
            newHeight = (int) (newWidth / (float) width * height);
        }
        else
        {
            newHeight = scaleHeight;
            newWidth = (int) (newHeight / (float) height * width);

        }
        //计算压缩的比率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;//获取想要缩放的
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.getWidth();
        bitmap.getHeight();
        Log.e("newWidth", "newWidth" + bitmap.getWidth());
        Log.e("newHeight", "newHeight" + bitmap.getHeight());
        return bitmap;
    }

    public Bitmap ColorConstancy(Bitmap bitmap, float[] illums_pooled) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        Log.e(TAG, "高度为：" + String.valueOf(height));
        Log.e(TAG, "宽度为：" + String.valueOf(width));
        Bitmap new_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Log.e(TAG, "new_bitmap高度为：" + String.valueOf(new_bitmap.getHeight()));
        Log.e(TAG, "new_bitmap宽度为：" + String.valueOf(new_bitmap.getWidth()));

        //行优先
        float mean = (illums_pooled[0] + illums_pooled[1] + illums_pooled[2]) / 3;
        Log.e(TAG, "mean：" + String.valueOf(mean));
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int argb = bitmap.getPixel(i, j);
                int r = Color.red(argb);
                int g = Color.green(argb);
                int b = Color.blue(argb);
                int new_r = (int) (Math.min(255, (r / illums_pooled[0] * mean)));
                int new_g = (int) (Math.min(255, (g / illums_pooled[1] * mean)));
                int new_b = (int) (Math.min(255, (b / illums_pooled[2] * mean)));
                //Log.e(TAG,"rgb："+String.valueOf(r)+" "+String.valueOf(g)+" "+String.valueOf(b));
                //Log.e(TAG,"new_rgb："+String.valueOf(new_r)+" "+String.valueOf(new_g)+" "+String.valueOf(new_b));
                new_bitmap.setPixel(i, j, Color.rgb(new_r, new_g, new_b));
                //Log.e(TAG,"new_rgb："+String.valueOf(new_r)+" "+String.valueOf(new_g)+" "+String.valueOf(new_b));
            }
        }
        Log.e(TAG, "finished!");
        return new_bitmap;
    }

    //保存图片
    public void saveClick(View v) {   //根目录
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/";

        //获取内部存储状态  
        String state = Environment.getExternalStorageState();
        //如果状态不是mounted，无法读写  
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        //按照时间命名
        Calendar now = new GregorianCalendar();
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String fileName = simpleDate.format(now.getTime());

        try {
            File file = new File(dir + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmaptoProcess.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Toast toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
            toast.setText("Image Saved!");
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //对比
    public void contrastClick(View v) {
        if (contrast) {
            imageViewToProcess.setImageBitmap(initbitmap);
            contrast = !contrast;
        } else {
            imageViewToProcess.setImageBitmap(bitmaptoProcess);
            contrast = !contrast;
        }
    }

    //拍摄照片
    public void takePhotoClick(View v) {
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT < 24) {
            imageUri = Uri.fromFile(outputImage);
        } else {
            imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.cameraalbumtest.fileprovider", outputImage);
        }
        // 启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    //从相册选择照片
    public void chooseFromAlbumClick(View v) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO); // 打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 将拍摄的照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        //更新要处理的图像
                        bitmap = changeBitmapSize(bitmap);
                        bitmaptoProcess = bitmap;
                        initbitmap=bitmaptoProcess;
                        imageViewToProcess.setImageBitmap(bitmaptoProcess);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 判断手机系统版本号
                    if (Build.VERSION.SDK_INT >= 19) {
                        // 4.4及以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);
                    } else {
                        // 4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath); // 根据图片路径显示图片
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        // 通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            //更新要处理的图像
            bitmap = changeBitmapSize(bitmap);
            bitmaptoProcess = bitmap;
            initbitmap=bitmaptoProcess;
            imageViewToProcess.setImageBitmap(bitmaptoProcess);

        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

}
