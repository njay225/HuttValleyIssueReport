package com.example.school.huttvalleyissuereport;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

import static android.content.ContentValues.TAG;

/**
 * Created by School on 10/08/2017.
 */


@SuppressWarnings("deprecation")
class TaskAdapter extends ArrayAdapter<Report> {
    //Initialises a layout inflater which is used to get the report card view
    private static LayoutInflater layoutInflater;
    //A public variable is initialised so that any report which is clicked can be accessed by the
    //report view activity
    public static Report clickedReport;

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
        if(Objects.equals(reportCard.getUrgency(), getContext().getResources().getString(R.string.very_urgent_urgency)) && !reportCard.getResolved()){
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

        if(reportCard.getResolved()){
            card.setBackgroundResource(R.drawable.report_resolved);
            voteIcon.setImageResource(R.drawable.resolved_issues_icon);
        }

        //An onclick listener is added to the report card
        reportCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The public clicked report is set to the clicked report
                clickedReport = reportCard;
                //The reportview activity is then opened
                Intent intent = new Intent(getContext(), ReportViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        });

        //The report card is then added to the list view
        return reportCardView;

    }
}
