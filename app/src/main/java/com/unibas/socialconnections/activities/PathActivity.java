package com.unibas.socialconnections.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unibas.socialconnections.R;
import com.unibas.socialconnections.GUI.PathGraphBuilder;
import com.unibas.socialconnections.GUI.PathGraphView;
import com.unibas.socialconnections.GUI.PathHolder;

public class PathActivity extends AppCompatActivity {

    private static final int MAX_MEANINGFUL_DISTANCE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);

        PathGraphBuilder.GraphData data = PathHolder.pendingData;
        String otherUsername = PathHolder.otherUsername;
        PathHolder.pendingData = null;
        PathHolder.otherUsername = null;

        if (data == null) {
            Toast.makeText(this, "No path data available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView summaryText = findViewById(R.id.pathSummaryText);
        PathGraphView pathGraphView = findViewById(R.id.pathGraphView);

        int distance = PathGraphBuilder.getConnectionDistance(data);

        if (distance < 0) {
            summaryText.setText("There is more than 4 steps between you! No connections found.");
        } else {
            String name = (otherUsername != null) ? otherUsername : "this user";
            String stepWord = (distance == 1) ? "step" : "steps";
            summaryText.setText("You know " + name + " through " + distance + " " + stepWord +
                    ". Here are all connections of minimum length between you two:");
        }

        pathGraphView.setGraphData(data);
    }
}