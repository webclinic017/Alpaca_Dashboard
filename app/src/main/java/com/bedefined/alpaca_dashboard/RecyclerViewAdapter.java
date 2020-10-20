package com.bedefined.alpaca_dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.bar.Bar;
import net.jacobpeterson.domain.alpaca.position.Position;
import net.jacobpeterson.domain.polygon.dailyopenclose.DailyOpenCloseResponse;
import net.jacobpeterson.domain.polygon.dailyopenclose.DailyOpenCloseTrade;
import net.jacobpeterson.domain.polygon.lastquote.LastQuoteResponse;
import net.jacobpeterson.domain.polygon.websocket.PolygonStreamMessage;
import net.jacobpeterson.domain.polygon.websocket.quote.QuoteMessage;
import net.jacobpeterson.polygon.PolygonAPI;
import net.jacobpeterson.polygon.rest.exception.PolygonAPIRequestException;
import net.jacobpeterson.polygon.websocket.listener.PolygonStreamListenerAdapter;
import net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType;

import org.w3c.dom.Text;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static androidx.core.content.ContextCompat.createDeviceProtectedStorageContext;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    Properties props = new Properties();

    // data is passed into the constructor
    RecyclerViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {

        MainActivity mainActivity = new MainActivity();

        String stockName = mData.get(position);
        holder.stock_name.setText(stockName);

        Thread thread = new Thread(() -> {

            PolygonAPI polygonAPI = new PolygonAPI();
            AlpacaAPI alpacaAPI = new AlpacaAPI();

            // Get Last value
            float close = 0;
            LastQuoteResponse curr = null;
            try {
                close = polygonAPI.getPreviousClose(mData.get(position), false).getResults().get(0).getC().floatValue();
            } catch (PolygonAPIRequestException e) {
                e.printStackTrace();
            }

            // Get Amount of shares owned
            Position shrOwned = null;
            try {
                shrOwned = alpacaAPI.getOpenPositionBySymbol(mData.get(position));
            } catch (AlpacaAPIRequestException e) {
                e.printStackTrace();
            }

            Position finalShrOwned = shrOwned;
            mainActivity.runOnUiThread(() -> {

                if (finalShrOwned.getQty().equals("1")) {
                    holder.sharesOwned.setText(finalShrOwned.getQty() + " share owned");
                } else {
                    holder.sharesOwned.setText(finalShrOwned.getQty() + " shares owned");
                }
            });

            // Get the last quote
            try {
                curr = polygonAPI.getLastQuote(mData.get(position));

            } catch (PolygonAPIRequestException e) {
                e.printStackTrace();
            }

            LastQuoteResponse finalCurr = curr;
            float finalClose = close;
            mainActivity.runOnUiThread(() -> {

                if (finalCurr != null && finalClose != 0) {
                    holder.priceOfStock.setText(String.format("$%.2f", finalCurr.getLast().getAskprice().floatValue()));
                    float temp = (finalCurr.getLast().getAskprice().floatValue() - finalClose) / finalClose * 100;

                    // Sets the precision and adds to view, then changes color
                    if (temp >= 0) {
                        holder.percentChange.setText(String.format("+%.2f%%", temp));
                        mClickListener.switchColors(holder, true);

                    } else {
                        holder.percentChange.setText(String.format("%.2f%%", temp));
                        mClickListener.switchColors(holder, true);
                    }

                }
            });
        });
        thread.start();

        /*String marketStatus = null;
        try {
            marketStatus = polygonAPI.getMarketStatus().getMarket();
        } catch (PolygonAPIRequestException e) {
            e.printStackTrace();
        }

        if (marketStatus.equals("open")) {

            // Start stream of values
            float finalClose = close;
//            Thread thread2 = new Thread(() -> {

                polygonAPI.addPolygonStreamListener(new PolygonStreamListenerAdapter(String.valueOf(mData.get(position)), PolygonStreamMessageType.QUOTE) {

                    float askingPrice = 0;
                    float percentChange = 0;

                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onStreamUpdate(PolygonStreamMessageType streamMessageType, PolygonStreamMessage streamMessage) {

                        // Get current asking price
                        askingPrice = ((QuoteMessage) streamMessage).getAp().floatValue();

                        // Update the percent change in the recycler view
                        // Update the percent change in the recycler view
                        percentChange = (finalClose - askingPrice) / finalClose * 100;

//                        if (percentChange >= 0) {
//                            holder.percentChange.setText(String.format("+%.2f%%", percentChange));
//                            holder.percentChange.setTextColor(Color.parseColor("#52E3C2"));
//                        } else {
//                            holder.percentChange.setText(String.format("%.2f%%", percentChange));
//                            holder.percentChange.setTextColor(Color.parseColor("#FF4495"));
//                        }
                    }
                });*/
//            });
//            thread2.start();
//    }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView stock_name;
        TextView percentChange;
        TextView sharesOwned;
        TextView priceOfStock;
        CardView stockCard;

        ViewHolder(View itemView) {
            super(itemView);
            stock_name = itemView.findViewById(R.id.stockName);
            percentChange = itemView.findViewById(R.id.currentPrice);
            sharesOwned = itemView.findViewById(R.id.sharesOwned);
            priceOfStock = itemView.findViewById(R.id.priceOfTicker);
            stockCard = itemView.findViewById(R.id.positionCard);
            stockCard.setOnClickListener(this);
            itemView.setOnClickListener(this);
            percentChange.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {
            if (view.getId() == stock_name.getId()) {

            }
            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

        void switchColors(ViewHolder view, boolean pos);
    }
}

