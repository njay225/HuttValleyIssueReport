package com.example.school.huttvalleyissuereport;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

public class SavedReportActivity extends AppCompatActivity {

    //Variable to log to console
    private static final String TAG = "SavedReportsActivity";

    //Arraylist to hold Saved Reports
    public static ArrayList<Report> savedReportsArrayList = new ArrayList<>();

    //Listview to display saved reports
    private ListView savedReportListView;

    //Public variable to saved clicked report
    public static Report clickedSavedReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_report);

        getSupportActionBar().setTitle("Saved Reports");

        savedReportListView = (ListView) findViewById(R.id.savedReportListView);
        try {
            FileInputStream fileInputStream = openFileInput(getResources().getString(R.string.saved_reports_array_list_location));
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            savedReportsArrayList = (ArrayList<Report>) objectInputStream.readObject();
            Log.d(TAG, "saveReport: " + savedReportsArrayList.size());
            objectInputStream.close();
            fileInputStream.close();

            for(int i = 0; i<savedReportsArrayList.size(); i++){
                if(savedReportsArrayList.get(i) == null){
                    savedReportsArrayList.remove(i);
                }
            }

            TaskAdapter adapter = new TaskAdapter(this, savedReportsArrayList);
            savedReportListView.setAdapter(adapter);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        try {
            FileInputStream fileInputStream = openFileInput(getResources().getString(R.string.saved_reports_array_list_location));
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            savedReportsArrayList = (ArrayList<Report>) objectInputStream.readObject();
            Log.d(TAG, "saveReport: " + savedReportsArrayList.size());
            objectInputStream.close();
            fileInputStream.close();

            for(int i = 0; i<savedReportsArrayList.size(); i++){
                if(savedReportsArrayList.get(i) == null){
                    savedReportsArrayList.remove(i);
                }
            }

            TaskAdapter adapter = new TaskAdapter(this, savedReportsArrayList);
            savedReportListView.setAdapter(adapter);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    class TaskAdapter extends ArrayAdapter<Report> {
        //Initialises a layout inflater which is used to get the report card view
        private LayoutInflater layoutInflater;

        public TaskAdapter(Context context, ArrayList<Report> task) {
            super(context, 0, task);
            //Sets up the layoutinflater
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View reportCardView, ViewGroup parent) {
            // Get the data item for this position
            final Report reportCard = getItem(position);

            //If the view for the report card is empty
            if(reportCardView == null) {
                //the view is set to the report card
                reportCardView = layoutInflater.inflate(R.layout.report_card, parent, false);
            }

            //A data format is initialised
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

            //The date from the report class is then converted to the date format
            String reportDate = dateFormat.format(reportCard.getDateAdded());

            //Views within the report card layout are initialised
            TextView reportTitle = (TextView) reportCardView.findViewById(R.id.titleTextView);
            TextView address = (TextView) reportCardView.findViewById(R.id.locationTextView);
            TextView category = (TextView) reportCardView.findViewById(R.id.categoryTextView);
            //TextView description = (TextView) reportCardView.findViewById(R.id.descriptionTextView);
            TextView date = (TextView) reportCardView.findViewById(R.id.dateTextView);
            TextView votes = (TextView) reportCardView.findViewById(R.id.votesTextView);
            RelativeLayout card = (RelativeLayout) reportCardView.findViewById(R.id.reportCardRelativeLayout);
            ImageView voteIcon = (ImageView) reportCardView.findViewById(R.id.voteIconImageView);

            //Data from the report card class is added to the views within the report card
            reportTitle.setText(reportCard.title);
            address.setText(reportCard.address);
            category.setText(reportCard.category);
            //description.setText(reportCard.description);
            votes.setText(Integer.toString(reportCard.votes) + " Votes");
            date.setText(reportDate);

            votes.setVisibility(View.GONE);
            voteIcon.setVisibility(View.GONE);

            //Checks if the number of votes is greater than 1000
            if(reportCard.votes > getContext().getResources().getInteger(R.integer.report_trending_limit)){
                //If it is then add a trending icon
                voteIcon.setImageResource(R.drawable.report_trending_up);
            }else{
                //If not, then add a flat line icon
                voteIcon.setImageResource(R.drawable.report_trending_flat);
            }

            Log.d(TAG, "getView: " + reportCard.getUrgency());

            //Checks if the urgency of the report is "Very Urgent"
            if(Objects.equals(reportCard.getUrgency(), getContext().getResources().getString(R.string.very_urgent_urgency))){
                //The background colour of the report card is changed
                card.setBackgroundResource(R.drawable.report);
                reportTitle.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                address.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                category.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                date.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                date.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                votes.setTextColor(getContext().getResources().getColor(R.color.veryUrgent));
                voteIcon.setColorFilter(getContext().getResources().getColor(R.color.veryUrgent));
            }else{
                //Other wise the standard colour is used
                Log.d(TAG, "getView: Report is not very urgent" + reportCard.getUrgency());
                card.setBackgroundResource(R.drawable.report);
                reportTitle.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                address.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                category.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                date.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                date.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                votes.setTextColor(getContext().getResources().getColor(R.color.colorPrimary));
                voteIcon.setColorFilter(getContext().getResources().getColor(R.color.colorPrimary));
            }

            //An onclick listener is added to the report card
            reportCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //The public clicked report is set to the clicked report
                    clickedSavedReport = reportCard;
                    //The reportview activity is then opened
                    Intent intent = new Intent(getContext(), CreateReportActivity.class);
                    intent.putExtra("savedReport", "true");
                    getContext().startActivity(intent);
                }
            });

            //The report card is then added to the list view
            return reportCardView;

        }
    }
}
