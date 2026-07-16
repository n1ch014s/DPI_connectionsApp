package com.unibas.socialconnections;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.unibas.socialconnections.GUI.PathGraphBuilder;
import com.unibas.socialconnections.GUI.PathGraphView;
import com.unibas.socialconnections.GUI.PathHolder;

public class PathActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);

        PathGraphBuilder.GraphData data = PathHolder.pendingData;
        PathHolder.pendingData = null; // clear after reading, avoid stale reuse

        if (data == null) {
            Toast.makeText(this, "No path data available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PathGraphView pathGraphView = findViewById(R.id.pathGraphView);
        pathGraphView.setGraphData(data);
    }
}