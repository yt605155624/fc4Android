package com.example.xiaotiantian.fc4android;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Created by XiaoTianTian on 2019/3/30.
 */

public class PredictionFC4 {
    private static final String TAG = "PredictionFC4";

    static {
        //加载libtensorflow_inference.so库文件

        System.loadLibrary("tensorflow_inference");

        Log.e(TAG,"libtensorflow_inference.so库加载成功");
    }
    private static final String MODEL_FILE = "file:///android_asset/fc4_2.pb";
    //数据的维度
    private static final int HEIGHT = 512;
    private static final int WIDTH = 512;
    private static final int MAXL = 3;
    private static final String inputName = "images";
    private static final String inputName2 = "dropout";

    //模型中输出变量的名称
    private static final String outputName = "l2_normalize";
    //用于存储模型的输出数据,0-9
    private float[] outputs = new float[MAXL];
    TensorFlowInferenceInterface inferenceInterface;
    PredictionFC4(AssetManager assetManager) {
        //接口定义
        inferenceInterface = new TensorFlowInferenceInterface(assetManager,MODEL_FILE);
        Log.e(TAG,"TensoFlow模型文件加载成功");
    }
    public static float[] bitmapToFloatArray(Bitmap bitmap){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        // 计算缩放比例
        float scaleWidth = ((float) 512) / width;
        float scaleHeight = ((float) 512) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        height = bitmap.getHeight();
        width = bitmap.getWidth();
        Log.e(TAG,"高度为："+String.valueOf(height));
        Log.e(TAG,"宽度为："+String.valueOf(width));
        int mul=height*height;
        float[] result = new float[mul*3];
        int k = 0;
        //行优先
        for(int j = 0;j < height;j++){
            for (int i = 0;i < width;i++){
                int argb = bitmap.getPixel(i,j);
                int r = Color.red(argb);
                int g = Color.green(argb);
                int b = Color.blue(argb);
                //由于是灰度图，所以r,g,b分量是相等的。顺序是BGR
                result[k*3+0] = (float) (Math.pow(b / 255.0f,2.2f)*65535);
                result[k*3+1]=(float) (Math.pow(g / 255.0f,2.2f)*65535);
                result[k*3+2]=(float) (Math.pow(r / 255.0f,2.2f)*65535);
                k++;
            }
        }
        return result;

    }
    public float[]  getAddResult(Bitmap bitmap) {

        float[] pxs =bitmapToFloatArray(bitmap);
        float[] dropouts=new float[1];
        dropouts[0]= (float)1.0;
        inferenceInterface.feed(inputName, pxs,1,512,512,3);
        inferenceInterface.feed(inputName2,dropouts,1,1,1,1);
        String[] outputNames = new String[] {outputName};
        inferenceInterface.run(outputNames);
        inferenceInterface.fetch(outputName, outputs);
        return outputs;
    }
}


