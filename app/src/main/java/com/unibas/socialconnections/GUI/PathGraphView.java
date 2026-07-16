package com.unibas.socialconnections.GUI;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import java.util.*;

public class PathGraphView extends View {
    private List<PathGraphBuilder.GraphNode> nodes = new ArrayList<>();
    private List<PathGraphBuilder.GraphEdge> edges = new ArrayList<>();
    private final Map<String, PointF> positions = new HashMap<>();

    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PathGraphView(Context context) { super(context); init(); }
    public PathGraphView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        nodePaint.setColor(Color.parseColor("#4A90D9"));
        nodePaint.setStyle(Paint.Style.FILL);

        edgePaint.setColor(Color.LTGRAY);
        edgePaint.setStrokeWidth(4f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setGraphData(PathGraphBuilder.GraphData data) {
        this.nodes = data.nodes;
        this.edges = data.edges;
        computePositions(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computePositions(w, h);
    }

    private void computePositions(int w, int h) {
        positions.clear();
        if (nodes.isEmpty() || w == 0 || h == 0) return;

        int maxLayer = 0;
        for (PathGraphBuilder.GraphNode n : nodes) maxLayer = Math.max(maxLayer, n.layer);

        Map<Integer, List<PathGraphBuilder.GraphNode>> byLayer = new TreeMap<>();
        for (PathGraphBuilder.GraphNode n : nodes) {
            byLayer.computeIfAbsent(n.layer, k -> new ArrayList<>()).add(n);
        }

        float colWidth = (float) w / (maxLayer + 1);

        for (Map.Entry<Integer, List<PathGraphBuilder.GraphNode>> entry : byLayer.entrySet()) {
            List<PathGraphBuilder.GraphNode> layerNodes = entry.getValue();
            float rowHeight = (float) h / (layerNodes.size() + 1);
            float x = colWidth * entry.getKey() + colWidth / 2f;

            for (int i = 0; i < layerNodes.size(); i++) {
                float y = rowHeight * (i + 1);
                positions.put(layerNodes.get(i).id, new PointF(x, y));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (PathGraphBuilder.GraphEdge e : edges) {
            PointF from = positions.get(e.fromId);
            PointF to = positions.get(e.toId);
            if (from != null && to != null) {
                canvas.drawLine(from.x, from.y, to.x, to.y, edgePaint);
            }
        }

        float radius = 60f;
        for (PathGraphBuilder.GraphNode n : nodes) {
            PointF p = positions.get(n.id);
            if (p == null) continue;
            canvas.drawCircle(p.x, p.y, radius, nodePaint);
            canvas.drawText(n.label, p.x, p.y + 10, textPaint);
        }
    }
}

