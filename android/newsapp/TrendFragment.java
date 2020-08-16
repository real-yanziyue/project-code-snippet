package com.example.newsapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class TrendFragment extends Fragment {

    LineChart lineChart;
    EditText trendKeyword;
    List<Integer> data;
    String keyword;
    ProgressBar spinner;
    TextView progressMessage;

    public TrendFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_trend, container, false);
        lineChart = inflate.findViewById(R.id.lineChart);
        trendKeyword = inflate.findViewById(R.id.trendKeyword);
        spinner = inflate.findViewById(R.id.trendprogressBar);
        progressMessage = inflate.findViewById(R.id.trendprogressInfo);
        keyword = trendKeyword.getHint().toString();
        populateChart(keyword);
        spinner.setVisibility(View.VISIBLE);
        progressMessage.setVisibility(View.VISIBLE);


       trendKeyword.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    keyword = trendKeyword.getText().toString();
                    spinner.setVisibility(View.VISIBLE);
                    progressMessage.setVisibility(View.VISIBLE);
                    populateChart(keyword);
                    return true;
                }
                return false;
            }
        });





        return inflate;
    }

    private void populateChart(final String word){
        String url = "http://ec2-54-173-221-8.compute-1.amazonaws.com:4000/googleTrend/"+word;
        data = new ArrayList<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray objArr = response.getJSONObject("default").getJSONArray("timelineData");
                            for(int i = 0; i < objArr.length(); i++){
                                int trend = (int) objArr.getJSONObject(i).getJSONArray("value").get(0);
                                data.add(trend);
                            }
                            List<Entry> entries = new ArrayList<Entry>();
                            for(int i = 0 ; i < data.size(); i++){
                                entries.add(new Entry(i,data.get(i)));

                            }
                            LineDataSet dataSet = new LineDataSet(entries, "Trending Chart for " + word);
                            dataSet.setColor(getResources().getColor(R.color.colorPrimaryDark));
                            dataSet.setCircleColor(R.color.colorPrimaryDark);
                            Legend legend = lineChart.getLegend();
                            legend.setTextColor(Color.BLACK);
                            legend.setTextSize(18f);
                            LineData lineData = new LineData(dataSet);
                            lineChart.setData(lineData);
                            lineChart.getXAxis().setDrawGridLines(false);
                            lineChart.getAxisRight().setDrawGridLines(false);
                            lineChart.getAxisLeft().setDrawGridLines(false);
                            lineChart.invalidate();
                            spinner.setVisibility(View.GONE);
                            progressMessage.setVisibility(View.GONE);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error);
                        error.printStackTrace();
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonObjectRequest);

    }

}
