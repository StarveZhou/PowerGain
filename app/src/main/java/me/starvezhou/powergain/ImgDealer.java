package me.starvezhou.powergain;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;

/**
 * Created by StarveZhou on 2018/1/14.
 */

public class ImgDealer {
    private static final String TAG = "ImgDealer::";
    //private static Mat renderedFrame = new Mat();
    public static Mat[] pictures = new Mat[3];
    public static boolean notInitialized = true;

    public static int number;




    public ImgDealer(){}

    public static void initializePicture(Context context, int resourceId){
        if (notInitialized == false){
            notInitialized = true;
            try{
                ImgDealer.pictures[0] = new Mat(new Size(803, 1024), CvType.CV_32FC3);
                ImgDealer.pictures[0] = Utils.loadResource(context, resourceId, Imgcodecs.CV_LOAD_IMAGE_COLOR);
            }
            catch (Exception e){
                Log.w(TAG, "load picture failed ; " + e.toString());
            }
        }
    }


    public static Point minus(Point a, Point b){
        return new Point(a.x - b.x , a.y - b.y);
    }

    public static double distance(Point a, Point b){
        return (a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y);
    }

    public static boolean lessThan(Point a, Point b){
        if (a.x - b.x < 0 || Math.abs(a.x - b.x) < 15 && a.y - b.y < 0) return true;
        else return false;
    }

    public static boolean detMoreThan(Point a, Point b, Point c){
        Point u = new Point();
        Point v = new Point();
        u.x = a.x - b.x; u.y = a.y - b.y;
        v.x = c.x - b.x; v.y = c.y - b.y;
        if (u.x * v.y - u.y * v.x > -15){
            return true;
        }
        else{
            return false;
        }
    }

    public static int[] getConvex(List<Point> pp){
        Point[] p = new Point[pp.size()];
        for (int i=0; i<pp.size(); i++){
            p[i] = new Point(pp.get(i).x, pp.get(i).y);
        }

        if (p.length < 3){
            int[] res = new int[p.length];
            for (int i=0; i<p.length; i++){
                res[i] = i;
            }
            return res;
        }
        else{
            int[] res = new int[2*p.length];
            for (int i=0; i<p.length; i++){
                for (int j=p.length-1; j>i; j--){
                    if (lessThan(p[j], p[j-1]) == true){
                        Point tmp = p[j]; p[j] = p[j-1]; p[j-1] = tmp;
                    }
                }
            }

            String str = "";
            for (int i=0; i<p.length; i++){
                str += p[i].x + "-" + p[i].y + ";;;";
            }
            Log.w(TAG, "sort :" + str);

            int i, j, k;
            res[0] = 0; res[1] = 1;
            for (i=2, j=2; i<p.length; res[j++] = i++){
                while (j > 1 && detMoreThan(p[res[j-2]], p[res[j-1]], p[i]) == true){
                    j --;
                }
            }
            res[k = j++] = p.length-2;
            for (i=p.length-3; i>0; res[j++] = i--){
                while (j > k && detMoreThan(p[res[j-2]], p[res[j-1]], p[i]) == true){
                    j --;
                }
            }
            while (j > k && detMoreThan(p[res[j-2]], p[res[j-1]], p[res[0]]) == true){
                j--;
            }
            return Arrays.copyOf(res, j);
        }
    }

    public static Mat changeTo2Img(CameraBridgeViewBase.CvCameraViewFrame inputFrame){



        int minSize = 130, maxSize = 500, count = 0;

        Mat renderedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Imgproc.threshold(inputFrame.gray(), renderedFrame, 100,255,Imgproc.THRESH_BINARY);
        Imgproc.morphologyEx(renderedFrame, renderedFrame, Imgproc.MORPH_OPEN, new Mat());
        Imgproc.morphologyEx(renderedFrame, renderedFrame, Imgproc.MORPH_CLOSE, new Mat());
        //Imgproc.adaptiveThreshold(inputFrame.gray(), renderedFrame, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 12, 4);

        List<MatOfPoint> oContours = new ArrayList<MatOfPoint>();
        int maxr = 0, maxc = 0;
        Imgproc.findContours(renderedFrame, oContours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        Log.w(TAG, "contous LLLL : " + oContours.size());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Point> centerPoint = new ArrayList<Point>();

        for (int i=0; i<oContours.size(); i++){
            if (oContours.get(i).rows() > minSize && oContours.get(i).rows() < maxSize){
                List<Point> temP = oContours.get(i).toList();
                Point center = new Point(0, 0);
                for (int j=0; j<temP.size(); j++){
                    center.x += temP.get(j).x;
                    center.y += temP.get(j).y;
                }
                center.x = center.x / temP.size();
                center.y = center.y / temP.size();

                double maxDis = -1, minDis = 10000000;
                int maxId = 0, minId = 0;
                for (int j=0; j<temP.size(); j++) {
                    double dis = distance(temP.get(j), center);
                    if (dis > maxDis){
                        maxDis = dis;
                        maxId = j;
                    }
                    if (dis < minDis){
                        minDis = dis;
                        minId = j;
                    }
                }

                if (minDis > maxDis*0.02 &&
                        renderedFrame.get((int)(center.y+0.5), (int)(center.x+0.5))[0] == 0.0 ){

                    Point random = new Point(0.0, 0.0);
                    random.x = (center.x + temP.get(maxId).x) / 2;
                    random.y = (center.y + temP.get(maxId).y) / 2;

                    if (renderedFrame.get((int)(random.y+0.5), (int)(random.x+0.5))[0] == 0.0){
                        contours.add(oContours.get(i));
                        centerPoint.add(center);
                    }
                    //Log.w(TAG, "mid pos :: " + center.x + ";;;" + center.y + ";;;" + renderedFrame.rows() + ";;;" + renderedFrame.cols());
                    //Log.w(TAG, "mid color :: " + renderedFrame.get((int)(center.y), (int)(center.x))[0]);
                    //Log.w(TAG, "mid color :: " + renderedFrame.get(0, 0).length);
                }
            }
        }

        Mat ori = inputFrame.rgba().clone();
        if (centerPoint.size() >= 4){
            /*int[] convexId = getConvex(centerPoint);
            List<Point> convexList = new ArrayList<Point>();
            for (int i=0; i<convexId.length; i++){
                convexList.add(centerPoint.get(convexId[i]));
            }
            List<MatOfPoint> convexPool = new ArrayList<MatOfPoint>();
            MatOfPoint convex = new MatOfPoint();
            convex.fromList(convexList);
            convexPool.add(convex);

            contours.add(convex);

            Log.w(TAG, "contours size : " + contours.size());*/
            number = contours.size();
            int[] convexId = {-1, -1, -1, -1};
            for (int i=0; i<contours.size(); i++){
                if (convexId[1] == -1 || centerPoint.get(i).x < centerPoint.get(convexId[1]).x){
                    convexId[1] = i;
                }
                if (convexId[3] == -1 || centerPoint.get(i).x > centerPoint.get(convexId[3]).x){
                    convexId[3] = i;
                }
            }

            for (int i=0; i<contours.size(); i++){
                if (i == convexId[1] || i == convexId[3]) continue;
                if (convexId[0] == -1 || centerPoint.get(i).y < centerPoint.get(convexId[0]).y){
                    convexId[0] = i;
                }
                if (convexId[2] == -1 || centerPoint.get(i).y > centerPoint.get(convexId[2]).y){
                    convexId[2] = i;
                }
            }

            Log.w(TAG, "node : " + String.valueOf(distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[3]))) + ";" + String.valueOf(distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[2]))));
/*
            if (Math.abs(centerPoint.get(convexId[0]).y - centerPoint.get(convexId[1]).y) < 100 || Math.abs(centerPoint.get(convexId[0]).y - centerPoint.get(convexId[3]).y) < 100) {
                if (distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[1])) > distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[2]))) {
                    int tmpI = convexId[1];
                    convexId[1] = convexId[2];
                    convexId[2] = tmpI;
                }

                if (distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[3])) > distance(centerPoint.get(convexId[0]), centerPoint.get(convexId[2]))) {
                    Log.w(TAG, "??????????");
                    int tmpI = convexId[3];
                    convexId[3] = convexId[2];
                    convexId[2] = tmpI;
                }
            }
*/
            List<Point> convexList = new ArrayList<Point>();
            for (int i=0; i<convexId.length; i++){
                convexList.add(centerPoint.get(convexId[i]));
            }
            List<MatOfPoint> convexPool = new ArrayList<MatOfPoint>();
            MatOfPoint convex = new MatOfPoint();
            Mat m = new Mat();
            convex.fromList(convexList);
            convexPool.add(convex);

            contours.add(convex);


            /*MatOfPoint2f oriConvex = new MatOfPoint2f();
            List<Point> oriConvexList = new ArrayList<Point>();
            oriConvexList.add(new Point(320, 720));
            oriConvexList.add(new Point(0, 720));
            oriConvexList.add(new Point(0, 0));
            oriConvexList.add(new Point(320, 0));
            oriConvex.fromList(oriConvexList);*/
            //矩阵变换

            MatOfPoint2f convex2f = new MatOfPoint2f();
            //List<Point> convex2fList = new ArrayList<Point>();
            //convex2fList = convex.toList();
            convex2f.fromList(convexList);

            //Mat M = Imgproc.getPerspectiveTransform(oriConvex, convex2f);
            //Mat chgMat = new Mat();
            //Imgproc.warpPerspective(ori.submat(0, 720, 0, 320), chgMat, M, ori.size(), Imgproc.INTER_LINEAR);
            //Core.addWeighted(ori, 0.5, chgMat, 0.5, 0, ori);




            //反解相机


            Point3[] point3D = new Point3[4];
            MatOfPoint3f points3D = new MatOfPoint3f();
            List<Point3> points3DList = new ArrayList<Point3>();
            point3D[0] = new Point3();
            point3D[0].x = 5.3; point3D[0].y = 2.3; point3D[0].z = 0;
            points3DList.add(point3D[0]);
            point3D[1] = new Point3();
            point3D[1].x = -5.3; point3D[1].y = 2.3; point3D[1].z = 0;
            points3DList.add(point3D[1]);
            point3D[2] = new Point3();
            point3D[2].x = -5.3; point3D[2].y = -2.3; point3D[2].z = 0;
            points3DList.add(point3D[2]);
            point3D[3] = new Point3();
            point3D[3].x = 5.3; point3D[3].y = -2.3; point3D[3].z = 0;
            points3DList.add(point3D[3]);
            //point3D.x = 0.0; point3D.y = 0.0; point3D.z = 0;
            //points3DList.add(point3D);
            points3D.fromList(points3DList);

            Mat rvec1 = new Mat(new Size(3, 3), CvType.CV_64FC1);
            Mat tvec = new Mat(new Size(1, 3), CvType.CV_64FC1);
            Mat cameraMatrix = new Mat(new Size(3, 3), CvType.CV_64FC1);
            double[] cameraMatrixArray = new double[9];
            cameraMatrixArray[0] = 1050.213958887031;cameraMatrixArray[1] = 0;cameraMatrixArray[2] = 640.0;
            cameraMatrixArray[3] = 0;cameraMatrixArray[4] = 1050.213958887031;cameraMatrixArray[5] = 360.0;
            cameraMatrixArray[6] = 0;cameraMatrixArray[7] = 0;cameraMatrixArray[8] = 1.0;
            cameraMatrix.put(0, 0, cameraMatrixArray);


            String p3Str = "points3D : ";
            for (int i=0; i<points3D.rows(); i++){
                for (int j=0; j<points3D.cols(); j++){
                    p3Str += points3D.get(i, j)[0] + ", " + points3D.get(i, j)[1] + ", " + points3D.get(i, j)[2] + ";";
                }
                p3Str += "\n";
            }
            Log.w(TAG, p3Str);

            String p2Str = "points2D : ";
            for (int i=0; i<convex2f.rows(); i++){
                for (int j=0; j<convex2f.cols(); j++){
                    p2Str += convex2f.get(i, j)[0] + ", " + convex2f.get(i, j)[1] + ", " + ";";
                }
                p2Str += "\n";
            }
            Log.w(TAG, p2Str);


            boolean bo = Calib3d.solvePnP(points3D, convex2f, cameraMatrix, new MatOfDouble(), rvec1, tvec);
            //Calib3d.solveP3P(new Mat().setTo(points3D).t(), new Mat().setTo(convex2f).t(), cameraMatrix, new Mat(), rvec1List, tvecList, Calib3d.SOLVEPNP_P3P);
            //for (int ii=0; ii<rvec1List.size(); ii++){
            Mat rvec = new Mat();

            Calib3d.Rodrigues(rvec1, rvec);
            Log.w(TAG, "rvec size : " + rvec.rows() + ";" + rvec.cols() + ";" + rvec.type() + ";" + rvec.get(0, 0)[0]);
            Log.w(TAG, "tvec size : " + tvec.rows() + ";" + tvec.cols() + ";" + tvec.type());
            //Log.w(TAG, "M Info : " + M.rows() + ";;" + M.cols());


            double[] tmpData = new double[12];
            int cnt = 0;
            for (int i=0; i<3; i++){
                for (int j=0; j<4; j++){
                    if (j < 3){
                        tmpData[cnt++] = rvec.get(i, j)[0];
                    }
                    else{
                        tmpData[cnt++] = tvec.get(i, 0)[0];
                    }
                }
            }

            Mat cvec = new Mat(new Size(4, 3), CvType.CV_64FC1);
            cvec.put(0, 0, tmpData);

            String str = "";
            for (int i=0; i<3; i++){
                for (int j=0; j<4; j++){
                    str = str + cvec.get(i, j)[0] + " ";
                }
                str += "\n";
            }
            Log.w(TAG, str);

            MatOfPoint upConvex = new MatOfPoint();
            Mat coef = new Mat();
            Core.gemm(cameraMatrix, cvec, 1, new Mat(), 0, coef);

            String str2 = "";
            for (int i=0; i<coef.rows(); i++){
                for (int j=0; j<coef.cols(); j++){
                    str2 += coef.get(i, j)[0] + "  ";
                }
                str2 += "\n";
            }

            Log.w(TAG, "coef  ?::" +str2);


            List<Point> upConvexList = new ArrayList<Point>();
            for (int i=0; i<4; i++){
                Mat tmpDst = new Mat();
                Mat tmpSrc = new Mat(new Size(1, 4), CvType.CV_64FC1);
                double[] putIn = new double[4];
                putIn[0] = (i == 0 || i == 3) ? 5.3 : -5.3;
                putIn[1] = (i == 0 || i == 1) ? 2.3 : -2.3;
                putIn[2] = 4.0;putIn[3] = 1.0;
                tmpSrc.put(0,0,putIn);

                Core.gemm(coef, tmpSrc, 1, new Mat(), 0, tmpDst);
                Point tmpP = new Point();
                tmpP.x = tmpDst.get(0, 0)[0] / tmpDst.get(2, 0)[0];
                tmpP.y = tmpDst.get(1, 0)[0] / tmpDst.get(2, 0)[0];

                Log.w(TAG, "left : " + i + "\n" + tmpSrc.get(0, 0)[0] + ", "+ tmpSrc.get(1, 0)[0] + ", "+ tmpSrc.get(2, 0)[0] + ", "+ tmpSrc.get(3, 0)[0] + ", ");
                Log.w(TAG, "op : " + i + "\n" + tmpP.x + ", " + tmpP.y);
                Log.w(TAG, "ori : " + i + "\n" + tmpDst.get(0, 0)[0] + ", " + tmpDst.get(1, 0)[0] + ", " + tmpDst.get(2, 0)[0]);
                //tmpP.x = tmpDst.get(0, 0)[0];
                //tmpP.y = tmpDst.get(1, 0)[0];
                upConvexList.add(tmpP);
            }
            upConvex.fromList(upConvexList);
            //contours.add(upConvex);
            List<MatOfPoint> upContours = new ArrayList<MatOfPoint>();
            upContours.add(upConvex);

            List<Point> leftContoursList = new ArrayList<Point>();
            MatOfPoint leftContour = new MatOfPoint();
            leftContoursList.add(convexList.get(2));
            leftContoursList.add(upConvexList.get(2));
            leftContoursList.add(upConvexList.get(1));
            leftContoursList.add(convexList.get(1));
            leftContour.fromList(leftContoursList);

            List<Point> rightContoursList = new ArrayList<Point>();
            MatOfPoint rightContour = new MatOfPoint();
            rightContoursList.add(convexList.get(2));
            rightContoursList.add(upConvexList.get(2));
            rightContoursList.add(upConvexList.get(3));
            rightContoursList.add(convexList.get(3));
            rightContour.fromList(rightContoursList);

            upContours.add(leftContour);
            upContours.add(rightContour);

/*
            Imgproc.putText(ori, "v" + convex2f.get(0, 0)[0] + ";" + convex2f.get(0, 0)[1] , new Point(100, 100), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255));
            Imgproc.putText(ori, "v" + convex2f.get(1, 0)[0] + ";" + convex2f.get(1, 0)[1] , new Point(100, 150), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255));
            Imgproc.putText(ori, "v" + convex2f.get(2, 0)[0] + ";" + convex2f.get(2, 0)[1] , new Point(100, 200), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255));
            Imgproc.putText(ori, "v" + convex2f.get(3, 0)[0] + ";" + convex2f.get(3, 0)[1] , new Point(100, 250), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255));

            Imgproc.putText(ori, "u" + upConvexList.get(0).x+";" + upConvexList.get(0).y, new Point(100, 300), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0));
            Imgproc.putText(ori, "u" + upConvexList.get(1).x+";" + upConvexList.get(1).y, new Point(100, 350), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0));
            Imgproc.putText(ori, "u" + upConvexList.get(2).x+";" + upConvexList.get(2).y, new Point(100, 400), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0));
            Imgproc.putText(ori, "u" + upConvexList.get(3).x+";" + upConvexList.get(3).y, new Point(100, 450), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0));
            */
            Scalar upColor;
            switch (number){
                case 4:{
                    upColor = new Scalar(255, 0, 0);
                    break;
                }
                case 6:{
                    upColor = new Scalar(0, 255, 0);
                    break;
                }
                case 9:{
                    upColor = new Scalar(0, 0, 255);
                    break;
                }
                default:{
                    upColor = new Scalar(122, 122, 122);
                    break;
                }
            }

            Imgproc.fillPoly(ori, upContours, upColor);

            double[] colD = new double[3];
            Scalar col = new Scalar(0, 0, 0);

            //Imgproc.polylines(ori, upContours, true, col, 5);
            Imgproc.line(ori, convexList.get(1), upConvexList.get(1), col, 5);
            Imgproc.line(ori, convexList.get(2), upConvexList.get(2), col, 5);
            Imgproc.line(ori, convexList.get(3), upConvexList.get(3), col, 5);

            Imgproc.line(ori, convexList.get(1), convexList.get(2), col, 5);
            Imgproc.line(ori, convexList.get(3), convexList.get(2), col, 5);

            Imgproc.line(ori, upConvexList.get(0), upConvexList.get(1), col, 5);
            Imgproc.line(ori, upConvexList.get(1), upConvexList.get(2), col, 5);
            Imgproc.line(ori, upConvexList.get(2), upConvexList.get(3), col, 5);
            Imgproc.line(ori, upConvexList.get(3), upConvexList.get(0), col, 5);


            //逼近物体
/*
            Point diagVec = new Point();
            diagVec.x = convexList.get(0).x - convexList.get(2).x;
            diagVec.y = convexList.get(0).y - convexList.get(2).y;

            Point lVec = new Point(), sVec = new Point();
            lVec.x = convexList.get(0).x - convexList.get(1).x;
            lVec.y = convexList.get(0).y - convexList.get(1).y;
            sVec.x = convexList.get(0).x - convexList.get(3).x;
            sVec.y = convexList.get(0).y - convexList.get(3).y;

            double lLen, sLen;
            lLen = Math.sqrt(lVec.x * lVec.x + lVec.y * lVec.y);
            sLen = Math.sqrt(sVec.x * sVec.x + sVec.y * sVec.y);
            if (lLen < sLen){
                Point tmP = lVec;lVec = sVec;sVec = tmP;
                double tmL = lLen;lLen = sLen;sLen = tmL;
            }
            double cosTheta, sinTheta;
            if (lLen * sLen > 1e-1){
                cosTheta = -(lVec.x * sVec.x + lVec.y * sVec.y) / (lLen * sLen);
            }
            else{
                cosTheta = 0;
            }
            Log.w(TAG, "cos : " + cosTheta);
            sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);

            Point upVec = new Point();
            upVec.x = cosTheta * lVec.x + sinTheta * sVec.x;
            upVec.y = cosTheta * lVec.y + sinTheta * sVec.y;

            double oriRatio = 0.43396;
            double ratioL, ratio;
            ratioL = sLen / lLen;
            if (ratioL >= 0.43396){
                //y = 1 - oriRatio / x
                ratio = 1 - oriRatio / ratioL;
            }
            else{
                //y = 1 - ratioL / oriRatio
                ratio = 1 - ratioL / oriRatio;
            }

            upVec.x = upVec.x * ratio*2;
            upVec.y = upVec.y * ratio*2;

            Point[] upPoint = new Point[4];
            for (int i=0; i<4; i++){
                upPoint[i] = new Point();
                upPoint[i].x = upVec.x + convexList.get(i).x;
                upPoint[i].y = upVec.y + convexList.get(i).y;
            }

            Imgproc.line(ori, upConvexList.get(0), convexList.get(0), new Scalar(0.0, 0.0, 255), 5);
            Imgproc.line(ori, upConvexList.get(1), convexList.get(1), new Scalar(0.0, 122, 122), 5);
            Imgproc.line(ori, upConvexList.get(2), convexList.get(2), new Scalar(0.0, 255, 0), 5);
            Imgproc.line(ori, upConvexList.get(3), convexList.get(3), new Scalar(255, 0, 0), 5);
*/
            //Imgproc.polylines(ori, contours, true, new Scalar(0.0,255.0,0.0,1.0), 5);
            //M.release();
            //oriConvex.release();
            convex.release();
        }

        Log.w(TAG, "count ::: " + count);
        renderedFrame.release();
        return ori;
    }
}
