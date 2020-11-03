package com.seanmiller.alpacadashboard;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import net.jacobpeterson.domain.alpaca.order.Order;

import java.util.List;

public class RecyclerViewAdapterOrders extends RecyclerView.Adapter<RecyclerViewAdapterOrders.ViewHolder> {

    private static List<Order> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerViewAdapterOrders(Context context, List<Order> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_order, parent, false);
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterOrders.ViewHolder holder, int position) {

        holder.stockName.setText(mData.get(position).getSymbol());
//        holder.shrQty.setText(mData.get(position).getQty());
//        holder.timeClosed.setText(mData.get(position).getFilledAt().getMonth().getValue() + "/" + mData.get(position).getFilledAt().getDayOfMonth() + "/" + mData.get(position).getFilledAt().getYear() /*+ "\n" + mData.get(position).getFilledAt().getHour() + ":" + mData.get(position).getFilledAt().getMinute()*/);

        // Fetches 12hour hour:minute format including am/pm
        if (mData.get(position).getFilledAt() != null) {
            int hourTemp = mData.get(position).getFilledAt().toLocalTime().getHour();
            String hour = "";
            String minute = String.valueOf(mData.get(position).getFilledAt().toLocalTime().getMinute());
            if (Integer.parseInt(minute) < 10) {
                minute = "0" + minute;
            }
            minute += "am";
            if (hourTemp > 12) {
                hourTemp -= 12;
                minute = minute.substring(0, minute.length() - 2);
                minute += "pm";
            }
            hour = String.valueOf(hourTemp);
            holder.timeClosed.setText(hour + ":" + minute);
            holder.price.setText("$" + mData.get(position).getFilledAvgPrice());
            holder.pricePlaced.setText(mData.get(position).getSide() + " " + mData.get(position).getQty());

        } else {
            int hourTemp = mData.get(position).getCanceledAt().getHour();
            String hour = "";
            String minute = String.valueOf(mData.get(position).getCanceledAt().getMinute());
            if (Integer.parseInt(minute) < 10) {
                minute = "0" + minute;
            }
            minute += "am";
            if (hourTemp > 12) {
                hourTemp -= 12;
                minute = minute.substring(0, minute.length() - 2);
                minute += "pm";
            }

            hour = String.valueOf(hourTemp);
            holder.timeClosed.setText(hour + ":" + minute);
            holder.price.setText("Cancelled");
            holder.pricePlaced.setText(mData.get(position).getSide() + " " + mData.get(position).getQty());
        }

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //        TextView shrQty;
        Button pricePlaced;
        TextView stockName;
        TextView price;
        TextView timeClosed;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            pricePlaced = itemView.findViewById(R.id.pricePlaced);
//            shrQty = itemView.findViewById(R.id.shrQty);
            stockName = itemView.findViewById(R.id.stockNameOrder);
            price = itemView.findViewById(R.id.price);
            timeClosed = itemView.findViewById(R.id.timeClosed);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    Order getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

