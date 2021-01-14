package com.example.snackpass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    Product[] arr;

    public RecyclerViewAdapter(Product[] arr) {
        //constructor for the ViewAdapter
        this.arr = arr;
    }

    @NonNull
    @Override
    //this method creates a view holder that holds one inflated view and sticks into ViewGroup -> parent
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Using the layout inflater method
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_layout,parent,false);
        MyViewHolder myViewHolder = new MyViewHolder(view);
        return myViewHolder;
    }

    //this method happens everytime a new view instance binds to the view holder
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.name.setText(arr[position].getName());
        holder.meta.setText(arr[position].getMeta());
    }

    //simple method that returns the number of views in the viewholder
    @Override
    public int getItemCount() {
        return arr.length;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView name;
        TextView meta;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            meta = itemView.findViewById(R.id.meta);
        }

        //doesn't do anything atm
        @Override
        public void onClick(View v) {
        }
    }

    //updating the recycler view
    public void updateList(Product[] newList){
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(this.arr, newList));
        this.arr = newList;
        diffResult.dispatchUpdatesTo(this);
    }


}
