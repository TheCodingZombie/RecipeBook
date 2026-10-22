package com.example.recipebook;

import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.Holder> {
    public interface Click { void open(Recipe r, View shared); }
    private final Context context; private final Click click; private List<Recipe> items=new ArrayList<>();
    public RecipeAdapter(Context c, Click l) { context=c; click=l; }
    public void setItems(List<Recipe> list) { items=new ArrayList<>(list); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_recipe,p,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int pos) {
        Recipe r=items.get(pos); h.name.setText(r.title);
        h.image.setImageResource(R.drawable.recipe_placeholder);
        if(r.mainImage!=null&&!r.mainImage.isEmpty()) try { h.image.setImageURI(Uri.parse(r.mainImage)); } catch(Exception ignored){}
        h.itemView.setOnClickListener(v->click.open(r,h.itemView));
        h.open.setOnClickListener(v->click.open(r,h.itemView));
    }
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder {
        ImageView image; TextView name; Button open;
        Holder(View v){super(v);image=v.findViewById(R.id.recipeImage);name=v.findViewById(R.id.recipeName);open=v.findViewById(R.id.openRecipe);}
    }
}
