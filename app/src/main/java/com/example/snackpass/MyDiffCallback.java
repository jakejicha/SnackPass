package com.example.snackpass;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

//DiffUtil implementation to automate animations for changing trends
public class MyDiffCallback extends DiffUtil.Callback {
    Product[] oldProducts;
    Product[] newProducts;

    public MyDiffCallback(Product[] oldProducts, Product[] newProducts) {
        this.oldProducts = oldProducts;
        this.newProducts = newProducts;
    }

    @Override
    public int getOldListSize(){
        return oldProducts.length;
    }

    @Override
    public int getNewListSize(){
        return newProducts.length;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldProducts[oldItemPosition].name.equals(newProducts[newItemPosition].name);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldProducts[oldItemPosition].getMeta().equals(newProducts[newItemPosition].getMeta());
    }


    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        //you can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }

}
