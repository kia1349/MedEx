package com.example.android.medex;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alexfu.countdownview.CountDownView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.special.ResideMenu.ResideMenu;
import com.google.firebase.Timestamp;

import java.security.acl.LastOwnerException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CountDownFragment extends Fragment {

    private static final String TAG = "CountDown Firebase";
    private View parentView;
    private ResideMenu resideMenu;
    private HomeActivity parentActivity;

    CountDownView countDownView;
    TextView quizCountText;
    Date nextQuiz;
    Date current;

    QuizSet quizSet;
    List QuizList;

    FirebaseFirestore db;
    ProgressBar progressBar;
    ListenerRegistration listenerRegistration;

    public CountDownFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        parentView = inflater.inflate(R.layout.countdown_fragment, container, false);
        setupViews();
        setFirebaseListner();
        setQuiz();
        return parentView;
    }

    @Override
    public void onDetach() {
        super.onDetach();listenerRegistration.remove();
    }

    private void setupViews() {

        parentActivity = (HomeActivity) getActivity();
        Typeface raleway_bold = Typeface.createFromAsset(getActivity().getAssets(),"fonts/Raleway-Bold.ttf" );
        Typeface raleway_regular = Typeface.createFromAsset(getActivity().getAssets(),"fonts/Raleway-Regular.ttf" );

        Button button = parentActivity.findViewById(R.id.menu_button);
        TextView heading = parentActivity.findViewById(R.id.heading);
        progressBar = parentActivity.findViewById(R.id.progressbarHome);
        progressBar.setVisibility(View.VISIBLE);
        heading.setText("Quiz");
        resideMenu = parentActivity.getResideMenu();

        countDownView = parentView.findViewById(R.id.countDownView);
        quizCountText = parentView.findViewById(R.id.quizTitle);

        heading.setTypeface(raleway_bold);
        quizCountText.setTypeface(raleway_regular);
        quizCountText.setTextSize(30);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });
    }
    /* Firebase listener for listening change in quiz set */
    private void setFirebaseListner() {
        db = FirebaseFirestore.getInstance();

        final CollectionReference quizRef = db.collection("quizes");

        listenerRegistration = quizRef.orderBy("scheduledTime", Query.Direction.ASCENDING)
                .whereEqualTo("started", false)
                .limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            Log.d(TAG, "Change in scheduled time");
                            for (QueryDocumentSnapshot doc : value) {
                                if (doc.get("scheduledTime") != null) {
                                    Timestamp timestamp = doc.getTimestamp("scheduledTime");
                                    nextQuiz = timestamp.toDate();
                                    Log.d("Quiz time changed", nextQuiz.toString());
                                    Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_window);
                                    if(fragment instanceof CountDownFragment) {
                                        Log.d(TAG,"Count Down Fragment updated");
                                        getCurrentTime();
                                    }
                                }
                            }
                        } else {
                            countDownView.setVisibility(View.INVISIBLE);
                            quizCountText.setText("No Quizes Found");
                        }
                    }
                });
    }
    /* Setting count down timer for next quiz */
    private void setQuiz() {

        QuizList = parentActivity.getQuizList();
        if(QuizList.isEmpty()) {
            progressBar.setVisibility(View.INVISIBLE);
            countDownView.setVisibility(View.INVISIBLE);
            quizCountText.setText("No Data Found");
            quizCountText.setTextSize(24);
        } else {
            quizSet = (QuizSet) QuizList.get(0);
            getCurrentTime();
        }
    }
    /* getting current server time using firebase callable functions */
    private void getCurrentTime() {

        FirebaseFunctions.getInstance().getHttpsCallable("getTime")
                .call().addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
            @Override
            public void onSuccess(HttpsCallableResult httpsCallableResult) {
                long timestamp = (long) httpsCallableResult.getData();
                current = new Date(timestamp);
                try{
                    Log.d(TAG, current.toString());
                } catch (NullPointerException e) {
                    Log.d(TAG, "Current date null");
                }

                if(current != null && nextQuiz != null) {
                    Log.w("Current Time", current.toString());
                    Log.w("Next Quiz Time", nextQuiz.toString());

                    long different = nextQuiz.getTime() - current.getTime();

                    long secondsInMilli = 1000;
                    long minutesInMilli = secondsInMilli * 60;
                    long hoursInMilli = minutesInMilli * 60;
                    long daysInMilli = hoursInMilli * 24;

                    long elapsedDays = different / daysInMilli;
                    different = different % daysInMilli;

                    long elapsedHours = different / hoursInMilli;
                    different = different % hoursInMilli;

                    long elapsedMinutes = different / minutesInMilli;
                    different = different % minutesInMilli;

                    long elapsedSeconds = different / secondsInMilli;

                    elapsedHours = elapsedHours + (elapsedDays * 24);

                    long total = (elapsedHours * 60 * 60000) + (elapsedMinutes * 60000) + ((elapsedSeconds) * 1000);

                    progressBar.setVisibility(View.INVISIBLE);

                    if(total <= 0) {
                        countDownView.setVisibility(View.INVISIBLE);
                        quizCountText.setText("Waiting for quiz initialization");
                        quizCountText.setTextSize(24);
                    } else {
                        countDownView.setVisibility(View.VISIBLE);
                        quizCountText.setText(getString(R.string.next_quiz));
                        quizCountText.setTextSize(30);
                        countDownView.reset();
                        countDownView.setStartDuration(total);
                        countDownView.start();
                    }
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(parentActivity, "Check after some time", Toast.LENGTH_SHORT).show();
                    countDownView.setVisibility(View.INVISIBLE);
                    quizCountText.setText("No data found");
                    quizCountText.setTextSize(24);
                }
            }
        });
    }
}
