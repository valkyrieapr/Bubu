package com.example.brigitta.bub;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
    TextView number, score, status, subject;
    Button send;
    private String[] options = new String[]{"A", "B", "C", "D", "E"};
    private int questionCount = 30;

    private Rect rect;
    private List<Integer> answers, correctAnswers;
    private List<MatOfPoint> bubbles, contours;
    private Mat PaperSheet, hierarchy;
    private Mat nDilated, nGray, nThresh, nBlur, nCanny, nAdaptiveThresh;
    private Mat mErode, mGray, mBlur, mThresh, mCanny, mAdaptiveThresh;

    Bitmap Paper;
    String id, name, KKM, email, email_id, email_name, email_email, email_subject, email_status, email_score, qrFinalResult, answerKey, updateScore;
    Integer FinalScore; Barcode qrResult;
    Intent studentIntent1;

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
        status = (TextView) findViewById(R.id.statusText);
        subject = (TextView) findViewById(R.id.subjectText);

        // get fixed Paper Sheet from previous activity
        long PaperCopy = getIntent().getLongExtra("AnswerSheet", 0);
        Mat TempPaperCopy = new Mat (PaperCopy);
        PaperSheet = TempPaperCopy.clone();

        findContours.run();

        hierarchy = new Mat();
        contours = new ArrayList<>();
        correctAnswers = new ArrayList<>();
        answers = new ArrayList<>();
        bubbles = new ArrayList<>();

        getAnswers.run();

        for (int i = 0; i < answers.size(); i++) {
            Integer optionIndex = answers.get(i);
            System.out.println ((i + 1) + ". " + (optionIndex == null ? "NULL" : options[optionIndex]));
            //System.out.println((i + 1) + "a: " + options[correctAnswers.get(i)]);
        }

        //Imgproc.cvtColor(PaperSheet, PaperSheet, Imgproc.COLOR_BGR2RGB);

        Paper = Bitmap.createBitmap(PaperSheet.cols(), PaperSheet.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(PaperSheet, Paper);
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(Paper);

        getBarcode.run();

        send = (Button) findViewById(R.id.sendEmailbtn);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getStudentAllData();
                Toast.makeText(getApplicationContext(), "Email sent.", Toast.LENGTH_LONG).show();
                Paper.recycle();
                startActivity(new Intent(AnswerScanner.this, MainActivity.class));
                finish();
            }
        });

        //get Student Name from database
        Intent studentIntent1 = getIntent();
        id = studentIntent1.getStringExtra(Configuration.STD_ID);
        getStudent();

        if (studentIntent1.getStringExtra(Configuration.STD_ID) == null) {
            Toast.makeText(getApplicationContext(), "Student ID is empty.", Toast.LENGTH_LONG).show();
            System.out.println("ANSWERSCANNER >>>> ERROR: Student ID is empty.");
            startActivity(new Intent(AnswerScanner.this, MainActivity.class));
            Paper.recycle();
            finish();
        }

        hierarchy.release();
        mErode.release();
        mGray.release();
        mBlur.release();
        mThresh.release();
        mAdaptiveThresh.release();

        nDilated.release();
        nGray.release();
        nThresh.release();
        nBlur.release();
        nCanny.release();
        nAdaptiveThresh.release();
    }

    public Runnable findContours = new Runnable() {
        @Override
        public void run() {
            if (PaperSheet.empty()) {
                System.out.println("ANSWERSCANNER >>>> ERROR: Answer sheet is not detected.");
                finish();
            } else {
                try {
                    //to find bubble
                    mErode = new Mat();
                    Imgproc.erode(PaperSheet, mErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
                    mGray = new Mat();
                    Imgproc.cvtColor(mErode, mGray, Imgproc.COLOR_BGR2GRAY);
                    mBlur = new Mat();
                    Imgproc.blur(mGray, mBlur, new Size(5, 5));
                    mThresh = new Mat();
                    Imgproc.threshold(mGray, mThresh, 140, 255, THRESH_BINARY);
                    mCanny = new Mat();
                    Imgproc.Canny(mBlur, mCanny, 130, 20);
                    mAdaptiveThresh = new Mat();
                    Imgproc.adaptiveThreshold(mBlur, mAdaptiveThresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);

                    //to find black wrap for parent rectangle
                    nDilated = new Mat(PaperSheet.size(), CV_8UC1);
                    Imgproc.dilate(PaperSheet, nDilated, getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
                    nGray = new Mat(nDilated.size(), CV_8UC1);
                    Imgproc.cvtColor(nDilated, nGray, Imgproc.COLOR_BGR2GRAY);
                    nThresh = new Mat(nGray.rows(), nGray.cols(), nGray.type());
                    Imgproc.threshold(nGray, nThresh, 150, 255, THRESH_BINARY);
                    nBlur = new Mat(nGray.size(), CV_8UC1);
                    Imgproc.blur(nGray, nBlur, new Size(5., 5.));
                    nCanny = new Mat(nBlur.size(), CV_8UC1);
                    Imgproc.Canny(nBlur, nCanny, 160, 20);
                    nAdaptiveThresh = new Mat(nCanny.rows(), nGray.cols(), nGray.type());
                    Imgproc.adaptiveThreshold(nCanny, nAdaptiveThresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);
                } catch (Exception e) {
                    System.out.println("ANSWERSCANNER >>>> ERROR: " + e.getMessage());
                }
            }
        }
    };

    public Runnable getAnswers = new Runnable() {
        @Override
        public void run() {
            if (PaperSheet.empty()) {
                System.out.println("ANSWERSCANNER >>>> ERROR: Answer is not detected.");
                finish();
            } else {
                try {
                    findMainWrapper();
                    findBubbles();
                    findAnswers();
                } catch (Exception e) {
                    System.out.println("ANSWERSCANNER >>>> ERROR: " + e.getMessage());
                }
            }
        }
    };

    public Runnable getBarcode = new Runnable() {
        @Override
        public void run() {
            BarcodeDetector detector = new BarcodeDetector.Builder(getApplicationContext()).setBarcodeFormats(Barcode.QR_CODE).build();
            if (!detector.isOperational()) {
                Toast.makeText(getApplicationContext(), "Could not set up the detector!", Toast.LENGTH_SHORT).show();
                System.out.println("ANSWERSCANNER >>>> ERROR: Could not set up the detector!");
                doNewIntent();
            } else {
                Frame frame = new Frame.Builder().setBitmap(Paper).build();
                SparseArray <Barcode> barcodes = detector.detect(frame);

                System.out.println("ANSWERSCANNER >>>> Barcode Size: " + barcodes.size());

                if (barcodes.size() == 0) {
                    Toast.makeText(AnswerScanner.this, "Could not detect the QR Code. Try again.", Toast.LENGTH_SHORT).show();
                    System.out.println("ANSWERSCANNER >>>> ERROR: Could not detect the QR Code. Try again.");
                    doNewIntent();
                } else {
                    qrResult = barcodes.valueAt(0);
                    qrFinalResult = qrResult.rawValue.toString();

                    new GetAnswerKey(new ResponseInterface() {
                        @Override
                        public void getResponse(String data) {
                            for (int i = 0; i < answers.size(); i++) {
                                Integer optionIndex = answers.get(i);
                                System.out.println ((i + 1) + ". " + (optionIndex == null ? "NULL" : options[optionIndex]) + " = " + options[correctAnswers.get(i)]);
                                //System.out.println((i + 1) + "a: " + options[correctAnswers.get(i)]);
                            }

                            if (correctAnswers.size() != answers.size()) {
                                Toast.makeText(getApplicationContext(), "Number of problem doesn't match.", Toast.LENGTH_SHORT).show();
                                System.out.println("ANSWERSCANNER >>>> ERROR: Number of problem doesn't match.");
                                doNewIntent();
                            }

                            int correct = 0;
                            for (int j = 0; j < correctAnswers.size(); j++) {
                                if (answers.get(j) == correctAnswers.get(j)) {
                                    correct++;
                                }
                            }

                            FinalScore = correct * 10 / 3;
                            System.out.println("ANSWERSCANNER >>>> Correct problems: " + correct);
                            System.out.println("ANSWERSCANNER >>>> Total problems: " + correctAnswers.size());
                            System.out.println("ANSWERSCANNER >>>> Score: " + FinalScore);

                            score.setText(String.valueOf(FinalScore));
                            getKKM();
                            updateScore();
                        }
                    }).execute();
                }
            }
        }
    };

    private void findMainWrapper() {
        Imgproc.findContours(nAdaptiveThresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //Find rectangles
        HashMap <Double, MatOfPoint> rectangles = new HashMap<>();
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for(int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double contourSize = Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, approxCurve, contourSize * 0.02, true);

            if(approxCurve.toArray().length == 4){
                rectangles.put((double) i, contours.get(i));
            }
        }

        //find rectangle done with approxPolyDP
        int parentIndex = -1;

        // choose hierarchical rectangle which is our main wrapper rect
        for (Map.Entry <Double, MatOfPoint> rectangle : rectangles.entrySet()) {
            double index = rectangle.getKey();
            double[] idx = hierarchy.get(0, (int) index);
            double nextID = idx[0];
            double previousID = idx[1];

            if (nextID != -1 && previousID != -1) continue;

            int c = (int) index;
            int p = 0;

            while (hierarchy.get(0, c)[2] != -1) {
                c = (int) hierarchy.get(0, c)[2];
                p++;
            }

            if(hierarchy.get(0, c)[2] != -1) p++;

            if (p >= 3){
                parentIndex = (int) index;
            }
        }

        if (parentIndex < 0){
            System.out.println("ANSWERSCANNER >>>> ERROR: No parent index.");
            doNewIntent();
        }

        rect = Imgproc.boundingRect(contours.get(parentIndex));
        wrapperPadding();

        //Imgproc.rectangle(PaperSheet, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 0), 2);
    }

    private void findBubbles() {
        contours.clear();
        Imgproc.findContours(mCanny.submat(rect), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() == 0) {
            Toast.makeText(getApplicationContext(), "Couldn't detect bubbles. Try again.", Toast.LENGTH_SHORT).show();
            System.out.println("ANSWERSCANNER >>>> ERROR: Couldn't detect bubbles. Try again.");
            doNewIntent();
        } else {
            List<MatOfPoint> drafts = new ArrayList<>();
            //To determine which regions of the image are bubbles, we first loop over each of the individual contours.
            for (MatOfPoint contour : contours) {
                //Compute the bounding box of the contour, then use the bounding box to derive the aspect ratio
                Rect box = boundingRect(contour);
                int w = box.width;
                int h = box.height;
                double ratio = Math.max(w, h) / Math.min(w, h);

                //System.out.println("Circles Found | w: " + w + ", h: " + h);

                //In order for a contour area to be considered a bubble, the region should:
                //1. Be sufficiently wide and tall (in this case, at least 20-30 pixels in both dimensions).
                //2. Have an aspect ratio that is approximately equal to 1.
                //As long as these checks hold, we can update our drafts list and mark the region as a bubble.

                if (ratio >= 0.9 && ratio <= 1.1)
                    if(Math.max(w, h) < 30 && Math.min(w, h) > 20) {
                        drafts.add(contour);
                        //Imgproc.drawContours(PaperSheet, contours, contours.indexOf(contour), new Scalar(0, 0, 255), 1);
                    }
            }

            System.out.println("ANSWERSCANNER >>>> Bubbles Size: " + drafts.size());

            if(drafts.size() != questionCount * options.length) {
                Toast.makeText(getApplicationContext(), "Couldn't capture all bubbles. Try again.", Toast.LENGTH_SHORT).show();
                System.out.println("ANSWERSCANNER >>>> ERROR: Couldn't capture all bubbles. Try again.");
                doNewIntent();
            } else {
                //First, we must sort our drafts from top-to-bottom.
                // This will ensure that rows of questions that are closer to the top of the exam will appear first in the sorted list.
                TopLeftToBottomRight(drafts);

                bubbles = new ArrayList<>();

                //The reason this methodology works is because we have already sorted our contours from top-to-bottom.
                //We know that the 5 bubbles for each question will appear sequentially in our list — but we do not know whether these bubbles will be sorted from left-to-right.
                //Each question has 5 possible answers.
                //Sort the current set of contours from left to right.
                for (int j = 0; j < drafts.size(); j += options.length * 2) {
                    List<MatOfPoint> row = drafts.subList(j, j + options.length * 2);
                    LeftToRight(row);
                    bubbles.addAll(row);
                }
            }
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
                Mat mask = new Mat(nThresh.rows(), nThresh.cols(), CV_8UC1);
                Imgproc.drawContours(mask.submat(rect), list, -1, new Scalar(255, 0, 0), -1);

                //Apply the mask to the thresholded image, then count the number of non-zero pixels in the bubble area.
                Mat conjunction = new Mat(nThresh.size(), CV_8UC1);
                Core.bitwise_and(nThresh, mask, conjunction); //countNonZero counts black pixel in the mask

                int countNonZero = Core.countNonZero(conjunction);

                // filter for improper mask causing a high pixel-value
                if (countNonZero > 5000) {
                    countNonZero = 0;
                }

                //i= bubbles + 5, j= rows keberapa
                //System.out.println(i + ":" + j + " | countNonZero: " + countNonZero);

                filled[j] = new int[]{countNonZero, i, j};
            }

            System.out.println(Arrays.deepToString(filled));

            int[] selection = chooseFilledBubbles(filled);

            if (selection != null) {
                Imgproc.drawContours(PaperSheet.submat(rect), Arrays.asList(rows.get(selection[2])), -1, new Scalar(0, 255, 0), 2);
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

        System.out.println ("ANSWERSCANNER >>>> Answer Size:" + answers.size());
    }

    private void wrapperPadding() {
        int padding = 30;

        rect.x += padding;
        rect.y += padding;
        rect.width -= 2 * padding;
        rect.height -= 2 * padding;
    }

    private int[] chooseFilledBubbles(int[][] rows){
        //rows is 1 problem, 5 bubbles
        //rows[i] selects which bubble you're examining for that one problem
        //rows[i][0], that is, the 0th index, is the pixel-value
        // other indices (such as rows[i][1]) are data that we don't need in this method

        double mean = 0;

        // take the total sum pixel-value of all (5) answers in a row
        for(int i = 0; i < rows.length; i++){
            mean += rows[i][0];
        }

        // divide by the # of options (5) to get the mean pixel-value
        mean = 1.0d * mean / options.length;

        // zero pixel-value indicates improper mask
        boolean hasZero = false;

        // determine whether special calculations have to be made
        for(int i = 0; i < rows.length; i++){
            if (rows[i][0] == 0) {
                hasZero = true;
            }
        }

        if (hasZero) {
            // add a anomalouses point for each answer pixel-value above the mean
            // should total to 4 for a normal answer selection since
            // the selected answer has a lower pixel-value
            int anomalouses = 0;

            for (int i = 0; i < rows.length; i++){
                if (rows[i][0] > mean) anomalouses++;
            }

            // covers 2 scenarios:
            // (1) one 0, one mid and three high pixel-values : anomalouses == 3
            // (2) one 0 and four high pixel-values : anomalouses == 4
            if (anomalouses >= options.length - 2){
                // "lower" holds the bubble with the lowest pixel-value
                int[] lower = null;

                // loop through bubbles
                for (int i = 0; i < rows.length; i++){
                    // go through answer choices and find lowest pixel-value answers
                    if (rows[i][0] < mean){
                        if (lower != null){ // if a low pixel-value has been found before
                            if (lower[0] == 0){ // if the previous low pixel-value was 0
                                lower = rows[i];
                            }
                        } else { // if this is the first low pixel-value found
                            lower = rows[i];
                        }
                    }
                }
                return lower;
            } else {
                return null;
            }
        } else {
            // add a anomalouses point for each answer pixel-value above the mean
            // should total to 4 for a normal answer selection since
            // the selected answer has a lower pixel-value
            int anomalouses = 0;

            // check that one answer/bubble was selected
            for (int i = 0; i < rows.length; i++){
                if (rows[i][0] > mean) anomalouses++;
            }

            // if a bubble was selected, continue
            if (anomalouses == options.length - 1){
                // "lower" holds the bubble with the lowest pixel-value
                int[] lower = null;

                // loop through bubbles
                for (int i = 0; i < rows.length; i++){
                    // go through answer choices and find lowest pixel-value answer
                    if (lower == null || rows[i][0] < lower[0]){
                        lower = rows[i];
                    }
                }
                return lower;
            } else {
                return null;
            }
        }
    }

    private static void TopLeftToBottomRight(List<MatOfPoint> drafts) {
        // top-left to right-bottom sort
        Collections.sort(drafts, (e1, e2) -> {
            Point o1 = new Point(e1.get(0, 0));
            Point o2 = new Point(e2.get(0, 0));
            return o1.y > o2.y ? 1 : -1;
        });
    }

    public static void LeftToRight(List<MatOfPoint> row){
        // left to right sort
        Collections.sort(row, (e1, e2) -> {
            Point o1 = new Point(e1.get(0, 0));
            Point o2 = new Point(e2.get(0, 0));
            return o1.x > o2.x ? 1 : -1;
        });
    }

    //GET STUDENT NAME FROM ID
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
            System.out.println("ANSWERSCANNER >>>> Student Name: " + name);
            System.out.println("ANSWERSCANNER >>>> Student ID: " + id);
            number.setText(name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //GET ANSWER KEY
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
            if (qrFinalResult.equals("answerkey_mathematics")) {
                answerKey = Configuration.URL_GET_ANSWER_MATH;
                subject.setText("Mathematics");
                System.out.println("ANSWERSCANNER >>>> Subject: MATH");
            }
            if (qrFinalResult.equals("answerkey_english")) {
                answerKey = Configuration.URL_GET_ANSWER_ENGLISH;
                subject.setText("English");
                System.out.println("ANSWERSCANNER >>>> Subject: ENGLISH");
            }
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

    //GET KKM
    private void getKKM(){
        class GetKKM extends AsyncTask <Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                showKKM(s);
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandler rh = new RequestHandler();
                String s = rh.sendGetRequest(Configuration.URL_GET_KKM);
                return s;
            }
        }
        GetKKM gk = new GetKKM();
        gk.execute();
    }

    private void showKKM(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray result = jsonObject.getJSONArray(Configuration.TAG_JSON_ARRAY);
            JSONObject c = result.getJSONObject(0);
            KKM = c.getString(Configuration.TAG_KKM);
            System.out.println("ANSWERSCANNER >>>> Score: " + FinalScore + " | KKM: " + KKM);
            if (FinalScore >= Integer.parseInt(KKM)) {
                status.setText("PASSED");
            } else {
                status.setText("FAILED");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //UPDATE SCORE
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
                if (qrFinalResult.equals("answerkey_mathematics")) {
                    updateScore = Configuration.URL_UPDATE_SCORE_MATH;
                }
                if (qrFinalResult.equals("answerkey_english")) {
                    updateScore = Configuration.URL_UPDATE_SCORE_ENGLISH;
                }
                String s = rh.sendPostRequest(updateScore, hashMap);
                return s;
            }
        }
        UpdateScore us = new UpdateScore();
        us.execute();
    }

    //GET ALL DATA TO SEND EMAIL
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
                            "Student Name: " + email_name + "\n" + "Student ID: " + email_id + "\n" +
                            "Test Subject: " + email_subject + "\n" + "Score: " + FinalScore + "\n" +
                            "Minimum Score: " + KKM + "\n" + "Status: " + email_status,
                            "testresult.thesis@Gmail.com", email_email);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),"Email Error.", Toast.LENGTH_LONG).show();
                            System.out.println("ANSWERSCANNER >>>> Email Error.");
                            doNewIntent();
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
            if (FinalScore >= Integer.parseInt(KKM)) {
                email_status = "PASSED";
            } else {
                email_status = "FAILED";
            }
            if (qrFinalResult.equals("answerkey_mathematics")) {
                email_subject = "Mathematics";
            }
            if (qrFinalResult.equals("answerkey_english")) {
                email_subject = "English";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void doNewIntent() {
        Intent newIntent = new Intent(getApplicationContext(), PaperScanner.class);
        newIntent.putExtra(Configuration.STD_ID, id);
        Paper.recycle();
        startActivity(newIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(getApplicationContext(), PaperScanner.class);
        backIntent.putExtra(Configuration.STD_ID, id);
        Paper.recycle();
        startActivity(backIntent);
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        onCreate(new Bundle());
    }

}