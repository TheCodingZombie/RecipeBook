package com.example.recipebook;

import android.content.*;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    RecipeDatabase db;
    RecipeAdapter adapter;
    EditText search;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        View root=findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            android.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            v.setPadding(v.getPaddingLeft(),bars.top,v.getPaddingRight(),bars.bottom);
            return insets;
        });

        db = new RecipeDatabase(this);
        search = findViewById(R.id.searchInput);

        RecyclerView grid=findViewById(R.id.recipeGrid);
        grid.setLayoutManager(new GridLayoutManager(this, getResources().getConfiguration().screenWidthDp>=600?3:2));

        adapter=new RecipeAdapter(this,(r,shared)->{
            Intent i=new Intent(this,RecipeViewActivity.class); i.putExtra("id",r.id);
            // TODO make it so the card actually expands into the screen, rather than just a fade
            startActivity(i, android.app.ActivityOptions.makeSceneTransitionAnimation(this,shared,"recipeCard").toBundle());
        });
        grid.setAdapter(adapter);

        search.addTextChangedListener(
                new TextWatcher(){
                    public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                    public void onTextChanged(CharSequence s,int st,int before,int count){
                        refresh();
                    }
                    public void afterTextChanged(Editable e){}
                }
        );

        findViewById(R.id.clearSearch).setOnClickListener(v->search.setText(""));
        findViewById(R.id.newRecipe).setOnClickListener(v->startActivity(new Intent(this,RecipeEditActivity.class)));
    }
    @Override protected void onResume(){
        super.onResume();
        if (db!=null) refresh();
    }
    void refresh(){
        if (adapter==null) return;

        adapter.setItems(db.search(search.getText().toString()));
        findViewById(R.id.emptyMessage).setVisibility(adapter.getItemCount()==0?View.VISIBLE:View.GONE);
    }
}
