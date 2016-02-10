package com.example.liujing.howold;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by liujing on 16/2/9.
 */
public class FaceppDetect {
    public interface callBack {
        void sucess(JSONObject resule);

        void error(FaceppParseException exception);
    }

    public static void detect(final Bitmap bm, final callBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                public static Bitmap createBitmap (Bitmap source, int x, int y, int width, int height)
                从原始位图剪切图像.
                参数说明：
   　　              Bitmap source：要从中截图的原始位图
                    int x:起始x坐标
                    int y：起始y坐标
                    int width：要截的图的宽度
                    int height：要截的图的宽度

                Bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos)
                30 是压缩率，表示压缩70%; 如果不压缩是100，表示压缩率为0

                 */
                try {
                    HttpRequests requests = new HttpRequests(Constant.key, Constant.secret, true, true);

                    Bitmap bmsamll = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmsamll.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays = stream.toByteArray();

                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject jsonObject = requests.detectionDetect(parameters);

                    Log.e("TAG", jsonObject.toString());

                    if (callBack != null) {
                        callBack.sucess(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }
}
