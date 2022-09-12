package com.example.imagepro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.MSER;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    Mat returnPre = new Mat();
    boolean clickou = false, clockOver = false, topLeft = false, topRight = false, bottomLeft = false, bottomRight = false;
    int imgW, imgH;
//    int w = fl.getMeasuredWidth(); int h = fl.getMeasuredHeight();
//    Rect rimgTst = new Rect(0, 0, w, h);
    Mat imgTst;
    //Imgproc.circle(imgTst, new Point(w/2, h/2), 2, new Scalar(255, 255, 0), 2);

    SeekBar seekbar;
    Button startTstBtn, tlbtn, trbtn, blbtn, brbtn;
    TextView contagem;
    int tresh;

    private CascadeClassifier cascadeClassifier;
    private CascadeClassifier cascadeClassifier_eye;
    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    static Point relativeEdge = new Point();

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView=findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // load the model
        try{
            InputStream is =getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir=getDir("cascade", Context.MODE_PRIVATE);  // creating a folder
            File mCascadeFile =new File(cascadeDir,"haarcascade_frontalface_alt.xml"); // creating file on that folder
            FileOutputStream os=new FileOutputStream(mCascadeFile);
            byte[] buffer=new byte[4096];
            int byteRead;
            // writing that file from raw folder
            while((byteRead =is.read(buffer)) != -1){
                os.write(buffer,0,byteRead);
            }
            is.close();
            os.close();

            // loading file from  cascade folder created above
            cascadeClassifier=new CascadeClassifier(mCascadeFile.getAbsolutePath());
            // model is loaded

            // load eye haarcascade classifier
            InputStream is2 =getResources().openRawResource(R.raw.haarcascade_eye);
            // created before
            File mCascadeFile_eye =new File(cascadeDir,"haarcascade_eye.xml"); // creating file on that folder
            FileOutputStream os2=new FileOutputStream(mCascadeFile_eye);
            byte[] buffer1=new byte[4096];
            int byteRead1;
            // writing that file from raw folder
            while((byteRead1 =is2.read(buffer1)) != -1){
                os2.write(buffer1,0,byteRead1);
            }
            is2.close();
            os2.close();

            // loading file from  cascade folder created above
            cascadeClassifier_eye=new CascadeClassifier(mCascadeFile_eye.getAbsolutePath());
        }
        catch (IOException e){
            Log.i(TAG,"Cascade file not found");
        }

        seekbar = (SeekBar)findViewById(R.id.seekBar2);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int prog, boolean b) {
                tresh = prog;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        contagem = (TextView)findViewById(R.id.contagem);
        startTstBtn = (Button)findViewById(R.id.startTstBtn);

        tlbtn = (Button)findViewById(R.id.tlbtn);
        trbtn = (Button)findViewById(R.id.trbtn);
        blbtn = (Button)findViewById(R.id.blbtn);
        brbtn = (Button)findViewById(R.id.brbtn);

        startTstBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startTstBtn.setEnabled(false);
                startTstBtn.setVisibility(View.GONE);

                seekbar.setEnabled(false);
                seekbar.setVisibility(View.GONE);

                clickou = true;



                //Imgproc.circle(returnPre, new Point(imgW/2, imgH/2), 2, new Scalar(255, 0, 0), 2);

//                new CountDownTimer(3000,1000){
//
//                    @Override
//                    public void onTick(long l) {
//                        String left = Long.toString(l / 1000);
//                        contagem.setText(left);
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        contagem.setVisibility(View.GONE);
//                        clockOver = true;
//                    }
//                }.start();

            }
        });

        tlbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topLeft = true;
                Log.d("Edges", "Top Left: " + relativeEdge.x + ", " + relativeEdge.y);
            }
        });
        trbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topRight = true;
                Log.d("Edges", "Top Right: " + relativeEdge.x + ", " + relativeEdge.y);
            }
        });
        blbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomLeft = true;
                Log.d("Edges", "Bottom Left: " + relativeEdge.x + ", " + relativeEdge.y);
            }
        });
        brbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomRight = true;
                Log.d("Edges", "Bottom Right: " + relativeEdge.x + ", " + relativeEdge.y);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }

    int i = 0;
    Point point = new Point(0,0);

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        i++;
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();
        Random rx = new Random(), ry = new Random();
        imgW = (int)mRgba.size().width;
        imgH = (int)mRgba.size().height;
        // in processing pass mRgba to cascaderec class

        Rect rmRgba = new Rect(0, 0, imgW, imgH);
        Point tl = rmRgba.tl(), br = rmRgba.br();
        Point center = new Point(br.x/2, br.y/2);

        //Point randPoint = new Point(rx.nextInt((int)br.x+1), ry.nextInt((int)br.y+1));
        //point.x = rx.nextInt((int)br.x+1); point.y = ry.nextInt((int)br.y+1);

        Core.flip(mRgba,mRgba,1);

        mRgba=CascadeRec(mRgba);

        returnPre = mRgba;
        //mRgba.copyTo(returnPre);
        //imgTst = new Mat((int)mRgba.size().width, (int)mRgba.size().height, CvType.CV_32FC1, new Scalar(255, 255, 0));
        Scalar preto = new Scalar(0, 0, 0);
        //if(clickou) returnPre.setTo(preto);

//        if(clockOver && i!=30){
//            Imgproc.circle(mRgba, point, 2, new Scalar(255, 0, 0), 2);
//        }else {
//            i = 0;
//            point.x = rx.nextInt((int)br.x+1); point.y = ry.nextInt((int)br.y+1);
//        }



        if(!topLeft){
            point.x = tl.x; point.y = tl.y;
        }else if(!topRight){
            point.x = br.x; point.y = tl.y;
        }else if(!bottomLeft){
            point.x = tl.x; point.y = br.y;
        }else if(!bottomRight){
            point.x = br.x; point.y = br.y;
        }

        Imgproc.circle(returnPre, point, 2, new Scalar(255, 0, 0), 2);




        return returnPre;
    }

    private Mat CascadeRec(Mat mRgba) {
        // original frame is -90 degree so we have to rotate is to 90 to get proper face for detection

        //Core.flip(mRgba.t(),mRgba,-1);
        // convert it into RGB
        Mat mRbg=new Mat();
        Imgproc.cvtColor(mRgba,mRbg,Imgproc.COLOR_RGBA2RGB);

        int height=mRbg.height();
        // minimum size of face in frame
        int absoluteFaceSize=(int) (height*0.1);

        MatOfRect faces=new MatOfRect();
        if(cascadeClassifier !=null){
            //                                 input output                                     // minimum size of output
            cascadeClassifier.detectMultiScale(mRbg,faces,1.1,2,2, new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }

        // loop through all faces
        Rect[] facesArray=faces.toArray();
        for (int i=0;i<facesArray.length;i++){


            // draw face on original frame mRgba
            Imgproc.rectangle(mRgba,facesArray[i].tl(),facesArray[i].br(),new Scalar(0,255,0,255),2);
            // crop face image and then pass it through eye classifier
            // starting point
            Rect roi=new Rect((int)facesArray[i].tl().x,(int)facesArray[i].tl().y, (int)facesArray[i].br().x-(int)facesArray[i].tl().x,(int)facesArray[i].br().y-(int)facesArray[i].tl().y);

            // cropped mat image
            Mat cropped =new Mat(mRgba,roi);
            // create a array to store eyes coordinate but we have to pass MatOfRect to classifier
            MatOfRect eyes=new MatOfRect();
            if(cascadeClassifier_eye!=null){                                                      // find biggest size object
                cascadeClassifier_eye.detectMultiScale(cropped,eyes,1.15,2,2,new Size(100,100),new Size(120,120));

                // now create an array
                Rect[] eyesarray=eyes.toArray();
                // loop through each eye
                for (int j=0;j<eyesarray.length;j++){
                    // find coordinate on original frame mRgba
                    // starting point
                    int x1=(int)(eyesarray[j].tl().x+facesArray[i].tl().x);
                    int y1=(int)(eyesarray[j].tl().y+facesArray[i].tl().y);
                    // width and height
                    int w1=(int)(eyesarray[j].br().x-eyesarray[j].tl().x);
                    int h1=(int)(eyesarray[j].br().y-eyesarray[j].tl().y);
                    // end point
                    int x2=(int)(w1+x1);
                    int y2=(int)(h1+y1);

                    int vy1 = 35, vy2 = -40, vx1 = 20, vx2 = -20;
                    // draw eye on original frame mRgba
                    //input    starting point   ending point   color                 thickness
                    Imgproc.rectangle(mRgba,new Point(x1+vx1,y1+vy1),new Point(x2+vx2,y2+vy2),new Scalar(0,255,0,255),2);


                    // crop eye from face
                    // to reduce cropped eye image
                    Rect eye_roi=new Rect(x1+vx1,y1+vy1,w1-10,h1-40);
                    Mat eye_cropped=new Mat(mRgba,eye_roi);
                    // convert it to gray scale
                    Mat grayscale_eye_image=new Mat();
                    Imgproc.cvtColor(eye_cropped,grayscale_eye_image,Imgproc.COLOR_RGBA2GRAY);
                    // blur image to get better result

                    //Imgproc.blur(grayscale_eye_image,grayscale_eye_image,new Size(5,5));
                    Imgproc.GaussianBlur(grayscale_eye_image,grayscale_eye_image,new Size(5,5),5,5);
                    //Imgproc.medianBlur(grayscale_eye_image,grayscale_eye_image,7);

                    Imgproc.threshold(grayscale_eye_image,grayscale_eye_image,tresh,255,Imgproc.THRESH_BINARY_INV);
                    int k = 7;
                    Mat kernel = Mat.ones(k,k, CvType.CV_32F);
                    Imgproc.erode(grayscale_eye_image,grayscale_eye_image,kernel);
                    Imgproc.morphologyEx(grayscale_eye_image,grayscale_eye_image,Imgproc.MORPH_OPEN,kernel);

                    findAndDrawCountours(eye_cropped, grayscale_eye_image, j);

                    // add this to original cropped eye image
                    Imgproc.cvtColor(grayscale_eye_image,grayscale_eye_image,Imgproc.COLOR_GRAY2RGBA);
                    // input              input      output
                    //Core.add(grayscale_eye_image,eye_cropped,eye_cropped);
                    // replace eye image onto main frame

                    Core.addWeighted(eye_cropped, 1.0, grayscale_eye_image, 0.5, 0.0, eye_cropped);

                    eye_cropped.copyTo(new Mat(mRgba,eye_roi));

                }
            }



        }
        // rotate back original frame to -90 degree
        //Core.flip(mRgba.t(),mRgba,0);

        return mRgba;

    }

    private static void findAndDrawCountours(Mat image, Mat proc, int j) {
        List<MatOfPoint> contours = new ArrayList<>(), circles = new ArrayList<>();
        Mat hierarchy = new Mat();
        String eye[] = {"eye1: ", "eye2: "};
        final Point centroid = new Point();
        // find contours
        Imgproc.findContours(proc, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        // if any contour exist...

        for(MatOfPoint c : contours) {
            Moments M = Imgproc.moments(c);
            relativeEdge.x = M.get_m10() / M.get_m00();
            relativeEdge.y = M.get_m01() / M.get_m00();

            Imgproc.circle(image, relativeEdge, 1, new Scalar(0, 255, 0),3);
        }
//        Moments M = Imgproc.moments(contours.get(0));
//        final Point centroid = new Point();
//        centroid.x = M.get_m10() / M.get_m00();
//        centroid.y = M.get_m01() / M.get_m00();

        //Imgproc.circle(image, centroid, 1, new Scalar(138, 150, 98),1);
        //Log.d("Centroid", eye[j] + relativeEdge + "\n");

//        relativeEdge.x = centroid.x;
//        relativeEdge.y = centroid.y;
//
        Imgproc.drawContours(image, contours, 0, new Scalar(250, 0, 0), 2);
    }
}
