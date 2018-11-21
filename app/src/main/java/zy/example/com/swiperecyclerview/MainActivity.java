package zy.example.com.swiperecyclerview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ArrayList<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SwipeView view=findViewById(R.id.swipe_view);

//        initRecyclerView(view);
        initListView(view);
    }

    private void initRecyclerView(SwipeView view) {
        RecyclerView recyclerView=(RecyclerView)view.getContentView();
        Log.i(TAG, "onCreate: recyclerView="+recyclerView.getClass());
        list = new ArrayList<>(10);
        for (int i=0;i<20;i++){
            list.add("id"+i);
        }
        LinearLayoutManager manager=new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(manager);
        LetterAdapter adapter=new LetterAdapter(list);
        recyclerView.setAdapter(adapter);
    }

    private void initListView(SwipeView view) {
        ListView rvList=(ListView)view.getContentView();
        list = new ArrayList<>(10);
        for (int i=0;i<20;i++){
            list.add("id"+i);
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,list);
        rvList.setAdapter(adapter);
    }
}
