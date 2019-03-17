package com.example.brigitta.bub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

public class AnswerScanner extends AppCompatActivity {
    TextView number; TextView score; Button send;
    private String[] options = new String[]{"A", "B", "C", "D", "E"};
    private int questionCount = 30;

    private Rect roi;
    private List<Integer> answers, correctAnswers;
    private List<MatOfPoint> bubbles, contours;
    private Mat PaperSheet, hierarchy;
    private Mat dilated, gray, thresh, blur, canny, adaptiveThresh;
    private Mat mErode, mGray, mBlur, mThresh, mAdaptiveThresh;
    private String id;

    String name, email, email_id, email_name, email_email, email_subject, email_status, email_score;  String qrFinalResult; String answerKey, updateScore;
    Integer FinalScore; Barcode qrResult;
    private String JSON_STRING;
    public interface ResponseInterface {
        public void getResponse(String data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_answer_scanner);

        number = (TextView) findViewById(R.id.studentID);
        score = (TextView) findViewById(R.id.scoreText);

        Intent studentIntent1 = getIntent();
        id = studentIntent1.getStringExtra(Configuration.STD_ID);
        System.out.println("String:" + id);

        getStudent();

        long PaperCopy = getIntent().getLongExtra("PaperSheet", 0);
        Mat TempPaperCopy = new Mat (PaperCopy);
        PaperSheet = TempPaperCopy.clone();

        mErode = new Mat();
        Imgproc.erode(PaperSheet, mErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
        mGray = new Mat();
        Imgproc.cvtColor(mErode, mGray, Imgproc.COLOR_BGR2GRAY);
        mBlur = new Mat();
        Imgproc.blur(mGray, mBlur, new Size(5, 5));
        mThresh = new Mat();
        Imgproc.threshold(mGray, mThresh, 140, 255, THRESH_BINARY);
        mAdaptiveThresh = new Mat();
        Imgproc.adaptiveThreshold(mBlur, mAdaptiveThresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);

        //for finding parent rectangle
        dilated = new Mat(PaperSheet.size(), CV_8UC1);
        Imgproc.dilate(PaperSheet, dilated, getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        gray = new Mat(dilated.size(), CV_8UC1);
        Imgproc.cvtColor(dilated, gray, Imgproc.COLOR_BGR2GRAY);
        thresh = new Mat(gray.rows(), gray.cols(), gray.type());
        Imgproc.threshold(gray, thresh, 150, 255, THRESH_BINARY);
        blur = new Mat(gray.size(), CV_8UC1);
        Imgproc.blur(gray, blur, new Size(5., 5.));
        canny = new Mat(blur.size(), CV_8UC1);
        Imgproc.Canny(blur, canny, 160, 20);
        adaptiveThresh = new Mat(canny.rows(), gray.cols(), gray.type());
        Imgproc.adaptiveThreshold(canny, adaptiveThresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);

        hierarchy = new Mat();
        System.out.println("first:" + hierarchy);
        contours = new ArrayList<>();
        correctAnswers = new ArrayList<>();
        answers = new ArrayList<>();
        bubbles = new ArrayList<>();

        findParentRectangle();

        findBubbles();

        Imgproc.cvtColor(PaperSheet, PaperSheet, Imgproc.COLOR_BGR2RGB);

        Bitmap Paper = Bitmap.createBitmap(PaperSheet.cols(), PaperSheet.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(PaperSheet, Paper);

        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(Paper);

        BarcodeDetector detector = new BarcodeDetector.Builder(getApplicationContext()).setBarcodeFormats(Barcode.QR_CODE).build();

        if(!detector.isOperational()){
            Toast.makeText(getApplicationContext(), "Could not set up the detector!", Toast.LENGTH_SHORT).show();
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(Paper).build();
        SparseArray<Barcode> barcodes = detector.detect(frame);

        qrResult = barcodes.valueAt(0);
        System.out.println("QR CODE:" + qrResult.rawValue);
        qrFinalResult = qrResult.rawValue.toString();

        findAnswers();

        new GetAnswerKey(new ResponseInterface() {
            @Override
            public void getResponse(String data) {
                for (int i = 0; i < answers.size(); i++) {
                    Integer optionIndex = answers.get(i);
                    System.out.println ((i + 1) + ". " + (optionIndex == null ? "NULL" : options[optionIndex]));
                    System.out.println((i + 1) + "a: " + options[correctAnswers.get(i)]);
                }

                if (correctAnswers.size() != answers.size()) {
                    Toast.makeText(getApplicationContext(), "Number of problem doesn't match.", Toast.LENGTH_SHORT).show();
                    //error
                }

                int correct = 0;
                for (int j = 0; j < correctAnswers.size(); j++) {
                    if (answers.get(j) == correctAnswers.get(j)) {
                        correct ++;
                    }
                }
                FinalScore = correct * 10 / 3;
                System.out.println("Correct problems: " + correct);
                System.out.println("Total problems: " + correctAnswers.size());
                System.out.println("Score: " + FinalScore);

                score.setText(String.valueOf(FinalScore));

                updateScore();
            }
        }).execute();

        send = (Button) findViewById(R.id.sendEmailbtn);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getStudentAllData();
                Toast.makeText(getApplicationContext(), "Email sent.", Toast.LENGTH_LONG).show();
                finish();
                startActivity(new Intent(AnswerScanner.this, MainActivity.class));
            }
        });

        mErode.release();
        mGray.release();
        mBlur.release();
        mThresh.release();
        mAdaptiveThresh.release();

        dilated.release();
        gray.release();
        thresh.release();
        blur.release();
        canny.release();
        adaptiveThresh.release();

    }

    private void getStudent(){
        class GetStudent extends AsyncTask <Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                showStudent(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandler rh = new RequestHandler();
                String s = rh.sendGetRequestParam(Configuration.URL_GET_STD, id);
                return s;
            }
        }
        GetStudent ge = new GetStudent();
        ge.execute();
    }

    private void showStudent(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray result = jsonObject.getJSONArray(Configuration.TAG_JSON_ARRAY);

            JSONObject c = result.getJSONObject(0);
            name = c.getString(Configuration.TAG_NAME);
            email = c.getString(Configuration.TAG_EMAIL);
            if (name == "null") {
                Toast.makeText(getApplicationContext(), "Student ID is not registered. Please try again.", Toast.LENGTH_SHORT).show();
                finish();
                startActivity(new Intent(this, MainActivity.class));
            } else {
                System.out.println("Student Name: " + name);
                number.setText(name);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getStudentAllData(){
        class GetStudentAllData extends AsyncTask <Void,Void,String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                showStudentAllData(s);
                //System.out.println(s);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            GMailSender sender = new GMailSender("testresult.thesis@Gmail.com", "Brigitta66");
                            sender.sendMail("Test Result",
                                    "Student Name: " + email_name + "\n" + "Student ID: " + email_id + "\n" + "Test Subject: " + email_subject + "\n" + "Score: " + email_score + "\n" + "Minimum Score: " + "..." + "\n" + "Status: " + email_status,
                                    "testresult.thesis@Gmail.com", email_email);

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),"Error", Toast.LENGTH_LONG).show();
                        }
                    }
                }).start();
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandler rh = new RequestHandler();
                String s = rh.sendGetRequestParam(Configuration.URL_GET_ALL_STD, id);
                return s;
            }
        }
        GetStudentAllData gs = new GetStudentAllData();
        gs.execute();
    }


    private void showStudentAllData(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray result = jsonObject.getJSONArray(Configuration.TAG_JSON_ARRAY);
            JSONObject c = result.getJSONObject(0);
            email_id = c.getString(Configuration.TAG_ID);
            email_name = c.getString(Configuration.TAG_NAME);
            email_email = c.getString(Configuration.TAG_EMAIL);
            email_status = c.getString(Configuration.TAG_STATUS);
            email_score = c.getString(Configuration.TAG_SCORE);

            if (Integer.parseInt(email_score) <= 75) {
                email_status = "FAILED";
            } else {
                email_status = "PASSED";
            }

            if (qrFinalResult.equals("answerkey_mathematics")) {
                email_subject = "Mathematics";
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void findParentRectangle() {

        Imgproc.findContours(adaptiveThresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println ("getParentRectangle > hierarchy data:\n" + hierarchy.dump());

        // find rectangles
        HashMap<Double, MatOfPoint> rectangles = new HashMap<>();
        for(int i = 0; i < contours.size(); i++) {
            MatOfPoint2f approxCurve = new MatOfPoint2f( contours.get(i).toArray() );
            Imgproc.approxPolyDP(approxCurve, approxCurve, 0.02 * arcLength(approxCurve, true), true);

            if(approxCurve.toArray().length == 4){
                rectangles.put((double) i, contours.get(i));
            }
        }

        //System.out.println ("getParentRectangle > contours.size: " + contours.size());
        // System.out.println ("getParentRectangle > rectangles.size: " + rectangles.size());

        //find rectangle done with approxPolyDP
        int parentIndex = -1;

        // choose hierarchical rectangle which is our main wrapper rect
        for (Map.Entry <Double, MatOfPoint> rectangle : rectangles.entrySet()) {
            double index = rectangle.getKey();

            double[] ids = hierarchy.get(0, (int) index);
            double nextId = ids[0];
            double previousId = ids[1];

            if (nextId != -1 && previousId != -1) continue;

            int k = (int) index;
            int c = 0;

            while (hierarchy.get(0, k)[2] != -1) {
                k = (int) hierarchy.get(0, k)[2];
                c++;
            }

            if(hierarchy.get(0, k)[2] != -1) c++;

            if (c >= 3){
                parentIndex = (int) index;
            }

            //System.out.println ("getParentRectangle > index: " + index + ", c: " + c);

        }

        //System.out.println ("getParentRectangle > parentIndex: " + parentIndex);

        if(parentIndex < 0){
            Toast.makeText(getApplicationContext(), "Couldn't capture main wrapper", Toast.LENGTH_SHORT).show();
        }

        roi = Imgproc.boundingRect(contours.get(parentIndex));

        //System.out.println ("getParentRectangle > original roi.x: " + roi.x + ", roi.y: " + roi.y);
        //System.out.println ("getParentRectangle > original roi.width: " + roi.width + ", roi.height: " + roi.height);

        int padding = 30;

        roi.x += padding;
        roi.y += padding;
        roi.width -= 2 * padding;
        roi.height -= 2 * padding;

        //Imgproc.rectangle(PaperSheet, new Point(roi.x, roi.y), new Point(roi.x + roi.width,roi.y + roi.height), new Scalar(0, 0, 0), 2);
    }

    private void findBubbles() {

        contours.clear();

        Imgproc.findContours(mAdaptiveThresh.submat(roi), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint> drafts = new ArrayList<>();
        //To determine which regions of the image are bubbles, we first loop over each of the individual contours.
        for (MatOfPoint contour : contours) {
            //Compute the bounding box of the contour, then use the bounding box to derive the aspect ratio
            Rect box = boundingRect(contour);
            int w = box.width;
            int h = box.height;
            double ratio = Math.max(w, h) / Math.min(w, h);

            //In order for a contour area to be considered a bubble, the region should:
            //1. Be sufficiently wide and tall (in this case, at least 20-30 pixels in both dimensions).
            //2. Have an aspect ratio that is approximately equal to 1.
            //As long as these checks hold, we can update our drafts list and mark the region as a bubble.

            if (ratio >= 0.9 && ratio <= 1.1) {
                if(Math.max(w, h) < 30 && Math.min(w, h) > 20){
                    drafts.add(contour);
                    //Imgproc.drawContours(PaperSheet, contours, contours.indexOf(contour), new Scalar(0, 0, 255), 1);
                }
            }
        }

        if(drafts.size() != questionCount * options.length){
            Toast.makeText(getApplicationContext(), "Couldn't capture all bubbles. Try again. ", Toast.LENGTH_SHORT).show();
        }

        //First, we must sort our drafts from top-to-bottom.
        // This will ensure that rows of questions that are closer to the top of the exam will appear first in the sorted list.
        sortTopLeft2BottomRight(drafts);

        bubbles = new ArrayList<>();

        //The reason this methodology works is because we have already sorted our contours from top-to-bottom.
        //We know that the 5 bubbles for each question will appear sequentially in our list — but we do not know whether these bubbles will be sorted from left-to-right.
        //Each question has 5 possible answers.
        //Sort the current set of contours from left to right.
        for (int j = 0; j < drafts.size(); j += options.length * 2) {
            List<MatOfPoint> row = drafts.subList(j, j + options.length * 2);
            sortLeft2Right(row);
            bubbles.addAll(row);
        }
    }

    //Given a row of bubbles, the next step is to determine which bubble is filled in.
    //We can accomplish this by using our thresh image and counting the number of non-zero pixels (i.e., foreground pixels) in each bubble region:

    private void findAnswers() {

        for (int i = 0; i < bubbles.size(); i += options.length) {
            List<MatOfPoint> rows = bubbles.subList(i, i + options.length);

            int[][] filled = new int[rows.size()][5];

            //Loop over the sorted contours
            for (int j = 0; j < rows.size(); j++) {
                MatOfPoint col = rows.get(j);
                List<MatOfPoint> list = Arrays.asList(col);

                //Construct a mask that reveals only the current "bubble" for the question
                Mat mask = new Mat(mThresh.size(), CV_8UC1);
                Imgproc.drawContours(mask.submat(roi), list, -1, new Scalar(255, 0, 0), -1);

                //Apply the mask to the thresholded image, then count the number of non-zero pixels in the bubble area.
                Mat conjuction = new Mat(mThresh.size(), CV_8UC1);
                Core.bitwise_and(mThresh, mask, conjuction); //countNonZero counts black pixel in the mask

                int countNonZero = Core.countNonZero(conjuction);
                filled[j] = new int[]{countNonZero, i, j};
            }

            int[] selection = chooseFilledCircle(filled);

            if (selection != null) {
                Imgproc.drawContours(PaperSheet.submat(roi), Arrays.asList(rows.get(selection[2])), -1, new Scalar(0, 255, 0), 2);
            }

            answers.add(selection == null ? null : selection[2]);

        }

        List<Integer> odds = new ArrayList<>();
        List<Integer> evens = new ArrayList<>();
        for(int i = 0; i < answers.size(); i++) {
            if(i % 2 == 0) odds.add(answers.get(i));
            if(i % 2 == 1) evens.add(answers.get(i));
        }

        answers.clear();
        answers.addAll(odds);
        answers.addAll(evens);

        System.out.println ("answer:" + answers);
    }

    private class GetAnswerKey extends AsyncTask <Void, Void, String> {
        private ResponseInterface responseInterface;

        public GetAnswerKey(ResponseInterface responseInterface) {
            this.responseInterface = responseInterface;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            JSON_STRING = s;
            showAnswerKey();
            responseInterface.getResponse(s);
        }

        @Override
        protected String doInBackground(Void... params) {
            RequestHandler rh = new RequestHandler();
            System.out.println("QR Final Result: " + qrFinalResult);
            if (qrFinalResult.equals("answerkey_mathematics")) {
                answerKey = Configuration.URL_GET_ANSWER_MATH;
            }
            System.out.println("sendGetRequest: " + answerKey);
            String s = rh.sendGetRequest(answerKey);
            return s;
        }
    }

    private void showAnswerKey () {
        correctAnswers = new ArrayList<Integer>();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray(Configuration.TAG_JSON_ARRAY);

            for (int i = 0; i < result.length(); i++) {
                JSONObject jo = result.getJSONObject(i);
                int answerKey = jo.getInt(Configuration.TAG_ANSWERKEY);
                correctAnswers.add(answerKey);
                //System.out.println((i + 1) + "a. " + options[correctAnswers.get(i)]); //data are there
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateScore() {
        final String score = FinalScore.toString().trim();

        class UpdateScore extends AsyncTask <Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Toast.makeText(AnswerScanner.this, s, Toast.LENGTH_SHORT).show();
            }

            @Override
            protected String doInBackground(Void... params) {
                HashMap <String, String> hashMap = new HashMap<>();
                hashMap.put(Configuration.KEY_STD_ID, id);
                hashMap.put(Configuration.KEY_STD_SCORE, score);

                RequestHandler rh = new RequestHandler();
                System.out.println("QR Final Result: " + qrFinalResult);
                if (qrFinalResult.equals("answerkey_mathematics")) {
                    updateScore = Configuration.URL_UPDATE_SCORE_MATH;
                }
                System.out.println("sendPostRequest: " + updateScore);
                String s = rh.sendPostRequest(updateScore, hashMap);

                return s;
            }
        }

        UpdateScore us = new UpdateScore();
        us.execute();
    }

    private int[] chooseFilledCircle(int[][] rows){

        double mean = 0;
        for(int i = 0; i < rows.length; i++){
            mean += rows[i][0];
        }
        mean = 1.0d * mean / options.length;

        int anomalouses = 0;
        for(int i = 0; i < rows.length; i++){
            if(rows[i][0] > mean) anomalouses++;
        }

        if(anomalouses == options.length - 1){
            int[] lower = null;
            for(int i = 0; i < rows.length; i++){
                if(lower == null || lower[0] > rows[i][0]){
                    lower = rows[i];
                }
            }

            return lower;

        } else {
            return null;
        }
    }

    private static void sortTopLeft2BottomRight(List<MatOfPoint> drafts) {
        // top-left to right-bottom sort
        Collections.sort(drafts, (e1, e2) -> {

            Point o1 = new Point(e1.get(0, 0));
            Point o2 = new Point(e2.get(0, 0));

            return o1.y > o2.y ? 1 : -1;
        });
    }

    public static void sortLeft2Right(List<MatOfPoint> row){
        // left to right sort
        Collections.sort(row, (e1, e2) -> {

            Point o1 = new Point(e1.get(0, 0));
            Point o2 = new Point(e2.get(0, 0));

            return o1.x > o2.x ? 1 : -1;
        });
    }


    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

}