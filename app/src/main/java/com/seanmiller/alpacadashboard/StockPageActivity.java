package com.seanmiller.alpacadashboard;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.robinhood.spark.SparkView;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.enums.BarsTimeFrame;
import net.jacobpeterson.alpaca.enums.Direction;
import net.jacobpeterson.alpaca.enums.OrderStatus;
import net.jacobpeterson.alpaca.enums.PortfolioPeriodUnit;
import net.jacobpeterson.alpaca.enums.PortfolioTimeFrame;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.bar.Bar;
import net.jacobpeterson.domain.alpaca.calendar.Calendar;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.polygon.PolygonAPI;
import net.jacobpeterson.polygon.rest.exception.PolygonAPIRequestException;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.LinearMarginDecoration;

import static com.seanmiller.alpacadashboard.DashboardFragment.ticker;

@RequiresApi(api = Build.VERSION_CODES.O)
public class StockPageActivity extends AppCompatActivity implements RecyclerViewAdapterStocks.ItemClickListener {

    private SparkView sparkViewStock;
    private StockAdapter selectedAdapterStock;
    private RecyclerView recyclerOrdersStock;
    public TickerView tickerViewStock;
    private RecyclerViewAdapterOrders recycleAdapterOrders;
    private Button percentChangeStock;
    private ArrayList<Order> ordersStock;
    private ArrayList<Order> order;
    private RecyclerViewAdapterOrders recycleAdapterOrdersStock;
    private SwipeRefreshLayout swipeRefreshStock;
    private FloatingActionButton fab;
    private TextView numPos;
    private MaterialButton oneDayStock;
    private MaterialButton oneWeekStock;
    private MaterialButton oneMonthStock;
    private MaterialButton threeMonthStock;
    private MaterialButton oneYearStock;
    private MaterialButton selectedButton;
    private AtomicInteger posOrNegColorLight;
    private StockAdapter oneDayStockAdapter;
    private StockAdapter oneWeekStockAdapter;
    private StockAdapter oneMonthStockAdapter;
    private StockAdapter threeMonthStockAdapter;
    private StockAdapter oneYearStockAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        Utils.startTheme(this, new SharedPreferencesManager(this).retrieveInt("theme", Utils.THEME_DEFAULT));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_page);

        // Initializations
        PolygonAPI polygonAPI = new PolygonAPI();
        AlpacaAPI alpacaAPI = new AlpacaAPI();

        // Set title
        TextView totalEquity = findViewById(R.id.stockTradedStock);
        totalEquity.setText(ticker.get());

        // Set percent change
        percentChangeStock = findViewById(R.id.percentChangeStock);

        // Set fab
        fab = findViewById(R.id.placeOrder);
        fab.setOnClickListener(v -> {
            PlaceOrderFragment dialogFrag = new PlaceOrderFragment();
            dialogFrag.setParentFab(fab);
            dialogFrag.show(getSupportFragmentManager(), dialogFrag.getTag());

        });

        // Bottom navigation
        /*BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        // Create viewpager for bottombar fragments
        viewPagerStocks = findViewById(R.id.viewPager);
        viewPagerStocks.setPagingEnabled(false);
        pagerAdapterStocks = new BottomBarAdapter(getSupportFragmentManager());

        bottomNavigation.bringToFront();
        bottomNavigation.setOnNavigationItemSelectedListener(this);

        Thread t1 = new Thread(() -> {

            pagerAdapterStocks.addFragments(MainActivity.dashboardFragment);
            pagerAdapterStocks.addFragments(MainActivity.searchFragment);
            pagerAdapterStocks.addFragments(MainActivity.profileFragment);
            pagerAdapterStocks.addFragments(MainActivity.emergencyFragment);

            runOnUiThread(() -> {
                viewPagerStocks.setAdapter(pagerAdapterStocks);
                viewPagerStocks.setCurrentItem(0);
            });
        });
        t1.start()*/
        ;

        Thread t3 = new Thread(() -> {

            // Set number of stocks textview
            numPos = findViewById(R.id.numberOfStocks);
            String numPosition = null;
            try {
                numPosition = alpacaAPI.getOpenPositionBySymbol(ticker.get()).getQty();
                String finalNumPosition = numPosition;
                runOnUiThread(() -> numPos.setText(finalNumPosition + " shares owned"));
            } catch (AlpacaAPIRequestException e) {
                runOnUiThread(() -> numPos.setText(0 + " shares owned"));
                e.printStackTrace();
            }
        });
        t3.start();

        // Set swipeRefresh
        swipeRefreshStock = findViewById(R.id.refreshStock);
        swipeRefreshStock.setTranslationZ(100);
        swipeRefreshStock.bringToFront();

        // Ticker information
        tickerViewStock = findViewById(R.id.tickerViewStock);
        tickerViewStock.setCharacterLists(TickerUtils.provideNumberList());

        Thread t2 = new Thread(() -> {

            // The sparkline graph itself
            // Set button group for timeframe
            oneDayStock = findViewById(R.id.oneDayStock);
            oneWeekStock = findViewById(R.id.oneWeekStock);
            oneMonthStock = findViewById(R.id.oneMonthStock);
            threeMonthStock = findViewById(R.id.threeMonthsStock);
            oneYearStock = findViewById(R.id.oneYearStock);
            oneYearStock = findViewById(R.id.oneYearStock);
            selectedButton = oneDayStock;

            // The sparkline graph data
            sparkViewStock = findViewById(R.id.sparkviewStock);
            try {
                oneDayStockAdapter = new StockAdapter(ticker, 1, PortfolioPeriodUnit.DAY, PortfolioTimeFrame.ONE_MIN);
                oneWeekStockAdapter = new StockAdapter(ticker, 1, PortfolioPeriodUnit.WEEK, PortfolioTimeFrame.ONE_HOUR);
                oneMonthStockAdapter = new StockAdapter(ticker, 1, PortfolioPeriodUnit.MONTH, PortfolioTimeFrame.ONE_DAY);
                threeMonthStockAdapter = new StockAdapter(ticker, 3, PortfolioPeriodUnit.MONTH, PortfolioTimeFrame.ONE_DAY);
                oneYearStockAdapter = new StockAdapter(ticker, 1, PortfolioPeriodUnit.YEAR, PortfolioTimeFrame.ONE_DAY);
                selectedAdapterStock = oneDayStockAdapter;

            } catch (PolygonAPIRequestException | AlpacaAPIRequestException e) {
                e.printStackTrace();
            }
            sparkViewStock.setAdapter(selectedAdapterStock);

            // Initalize all graphs
            try {
                initializeDashboardValues(1, ZonedDateTime.now().minusDays(1), BarsTimeFrame.FIVE_MINUTE, oneDayStockAdapter);
                initializeDashboardValues(1, ZonedDateTime.now().minusWeeks(1), BarsTimeFrame.FIFTEEN_MINUTE, oneWeekStockAdapter);
                initializeDashboardValues(1, ZonedDateTime.now().minusMonths(1), BarsTimeFrame.ONE_DAY, oneMonthStockAdapter);
                initializeDashboardValues(3, ZonedDateTime.now().minusMonths(3), BarsTimeFrame.ONE_DAY, threeMonthStockAdapter);
                initializeDashboardValues(1, ZonedDateTime.now().minusYears(1), BarsTimeFrame.ONE_DAY, oneYearStockAdapter);
            } catch (AlpacaAPIRequestException e) {
                e.printStackTrace();
            }

            // Set colors on click, for toggle buttons
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.color_positive_light, typedValue, true);
            posOrNegColorLight = new AtomicInteger(ContextCompat.getColor(this, typedValue.resourceId));
            getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            int colorPrimary = ContextCompat.getColor(this, typedValue.resourceId);
            oneDayStock.setOnClickListener(v -> {
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                selectedButton = oneDayStock;
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(posOrNegColorLight.get()));
                selectedAdapterStock = oneDayStockAdapter;
                sparkViewStock.setAdapter(selectedAdapterStock);
                setStockValues();
            });
            oneWeekStock.setOnClickListener(v -> {
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                selectedButton = oneWeekStock;
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(posOrNegColorLight.get()));
                selectedAdapterStock = oneWeekStockAdapter;
                sparkViewStock.setAdapter(selectedAdapterStock);
                setStockValues();
            });
            oneMonthStock.setOnClickListener(v -> {
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                selectedButton = oneMonthStock;
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(posOrNegColorLight.get()));
                selectedAdapterStock = oneMonthStockAdapter;
                sparkViewStock.setAdapter(selectedAdapterStock);
                setStockValues();
            });
            threeMonthStock.setOnClickListener(v -> {
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                selectedButton = threeMonthStock;
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(posOrNegColorLight.get()));
                selectedAdapterStock = threeMonthStockAdapter;
                sparkViewStock.setAdapter(selectedAdapterStock);
                setStockValues();
            });
            oneYearStock.setOnClickListener(v -> {
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                selectedButton = oneYearStock;
                selectedButton.setBackgroundTintList(ColorStateList.valueOf(posOrNegColorLight.get()));
                selectedAdapterStock = oneYearStockAdapter;
                sparkViewStock.setAdapter(selectedAdapterStock);
                setStockValues();
            });
//            try {
//                selectedAdapterStock = new StockAdapter(ticker, 0, null, null);
//            } catch (PolygonAPIRequestException | AlpacaAPIRequestException e) {
//                e.printStackTrace();
//            }
//            try {
//                selectedAdapterStock.initializeStock();
//            } catch (AlpacaAPIRequestException e) {
//                e.printStackTrace();
//            }

            AtomicReference<Double> amount = new AtomicReference<>();
            DecimalFormat formatter = new DecimalFormat("#,###.00");

            runOnUiThread(() -> {

                sparkViewStock.setAdapter(selectedAdapterStock);

                // Scrub for chart
                getTheme().resolveAttribute(R.attr.colorPrimaryLight, typedValue, true);
                AtomicInteger color = new AtomicInteger(ContextCompat.getColor(this, typedValue.resourceId));

                // Scrub for chart
                sparkViewStock.setSparkAnimator(null);
                sparkViewStock.setBaseLineColor(color.get());
                sparkViewStock.setScrubListener(value -> {

                    // Format to add commas
                    if (value != null) {
                        amount.set(Double.parseDouble(String.valueOf(value)));

                        tickerViewStock.setText("$" + formatter.format(amount.get()));

                    }
                });

            });

        });
        t2.start();

        // Check if the market is open, stream to ticker
        String marketStatus = null;
        try {
            marketStatus = polygonAPI.getMarketStatus().getMarket();
        } catch (PolygonAPIRequestException e) {
            e.printStackTrace();
        }

        // Stream to chart
        if (marketStatus.equals("open")) {

            // Stream live data for a stock
            MainActivity vars = new MainActivity();
            vars.streamStockData(polygonAPI, ticker, tickerViewStock);

            Thread t4 = new Thread(() -> {

                while (true) {
                    float askingPrice = 0;
                    try {
                        askingPrice = polygonAPI.getLastQuote(ticker.get()).getLast().getAskprice().floatValue();

                    } catch (PolygonAPIRequestException e) {
                        e.printStackTrace();
                    }

                    float finalAskingPrice = askingPrice;
                    runOnUiThread(() -> selectedAdapterStock.addVal(finalAskingPrice));

                    runOnUiThread(this::setStockValues);

                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t4.start();
        } else {

            Thread t5 = new Thread(() -> {
                float askingPrice = 0;
                try {
                    askingPrice = polygonAPI.getLastQuote(ticker.get()).getLast().getAskprice().floatValue();

                } catch (PolygonAPIRequestException e) {
                    e.printStackTrace();
                }

                float finalAskingPrice = askingPrice;
                runOnUiThread(() -> selectedAdapterStock.addVal(finalAskingPrice));
                runOnUiThread(this::setStockValues);
            });
            t5.start();
        }

        Thread thread = new Thread(() -> {

            // Fetch curent orders
            ordersStock = new ArrayList<>();
            order = new ArrayList();
            try {
                ordersStock = alpacaAPI.getOrders(OrderStatus.CLOSED, 100, null, ZonedDateTime.now().plusDays(1), Direction.DESCENDING, false);
            } catch (AlpacaAPIRequestException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < ordersStock.size(); i++) {
                if (ordersStock.get(i).getSymbol().equals(ticker.get())) {
                    order.add(ordersStock.get(i));
                }
            }

            recyclerOrdersStock = findViewById(R.id.ordersStock);
            recycleAdapterOrdersStock = new RecyclerViewAdapterOrders(this, order);

            runOnUiThread(() -> {
                recyclerOrdersStock.setLayoutManager(new LinearLayoutManager(this));
                recyclerOrdersStock.addItemDecoration(LinearMarginDecoration.create(0, LinearLayoutManager.VERTICAL, false, null));
                recyclerOrdersStock.setAdapter(recycleAdapterOrdersStock);
            });


        });
        thread.start();

        swipeRefreshStock.setOnRefreshListener(() -> {
            onRefresh();
            swipeRefreshStock.setNestedScrollingEnabled(false);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onRefresh() {

        swipeRefreshStock.setRefreshing(true);

        Thread thread2 = new Thread(() -> {

            AlpacaAPI alpacaAPI = new AlpacaAPI();

            // Fetch curent orders
            ordersStock = new ArrayList<>();
            try {
                ordersStock = alpacaAPI.getOrders(OrderStatus.CLOSED, 100, null, ZonedDateTime.now().plusDays(1), Direction.DESCENDING, false);
            } catch (AlpacaAPIRequestException e) {
                e.printStackTrace();
            }
            order.clear();
            for (int i = 0; i < ordersStock.size(); i++) {
                if (ordersStock.get(i).getSymbol().equals(ticker.get())) {
                    order.add(ordersStock.get(i));
                }
            }

            // Set number of stocks textview
            String numPosition = null;
            try {
                numPosition = alpacaAPI.getOpenPositionBySymbol(ticker.get()).getQty();
                String finalNumPosition = numPosition;
                runOnUiThread(() -> numPos.setText(finalNumPosition + " shares owned"));
            } catch (AlpacaAPIRequestException e) {
                runOnUiThread(() -> numPos.setText(0 + " shares owned"));
                e.printStackTrace();
            }

            runOnUiThread(() -> recycleAdapterOrdersStock.notifyDataSetChanged());

            swipeRefreshStock.setRefreshing(false);
        });
        thread2.start();
    }

    public void initializeDashboardValues(int periodLength, ZonedDateTime datetime, BarsTimeFrame timeFrame, StockAdapter selectedAdapterInitial) throws AlpacaAPIRequestException {

        // Requests bars and adds to graph
        AlpacaAPI alpacaAPI = new AlpacaAPI();
        PolygonAPI polygonAPI = new PolygonAPI();

        Thread thread = new Thread(() -> {

            Map<String, ArrayList<Bar>> bars = null;

            // Check if market is open
            String marketStatus = null;
            try {
                marketStatus = polygonAPI.getMarketStatus().getMarket();
            } catch (PolygonAPIRequestException e) {
                e.printStackTrace();
            }

            if (marketStatus.equals("open")) {

                if (datetime == ZonedDateTime.now()) {
                    // Fetch todays bars
                    try {
                        bars = alpacaAPI.getBars(timeFrame, ticker.get(), 1000,
                                ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), LocalTime.of(5, 30)), ZoneId.of("UTC-4")),
                                null, null, ZonedDateTime.now());

                    } catch (AlpacaAPIRequestException e) {
                        e.printStackTrace();
                    }

                } else {
                    // Fetch todays bars
                    try {
                        bars = alpacaAPI.getBars(timeFrame, ticker.get(), 1000,
                                datetime, ZonedDateTime.now(), null, null);

                    } catch (AlpacaAPIRequestException e) {
                        e.printStackTrace();
                    }
                }


            } else {

                if (datetime == ZonedDateTime.now()) {

                    // Fetch last open day's information
                    ArrayList<Calendar> calendar = null;
                    try {
                        calendar = alpacaAPI.getCalendar(LocalDate.now().minusWeeks(1), LocalDate.now());
                    } catch (AlpacaAPIRequestException e) {
                        e.printStackTrace();
                    }
                    LocalDate lastOpenDate = LocalDate.parse(calendar.get(calendar.size() - 1).getDate());
                    LocalTime lastOpenTime = LocalTime.parse(calendar.get(calendar.size() - 1).getClose());
                    LocalTime lastOpenTimeStart = LocalTime.parse(calendar.get(calendar.size() - 1).getOpen());

                    try {
                        bars = alpacaAPI.getBars(BarsTimeFrame.FIVE_MINUTE, ticker.get(), 1000, null, null,
                                ZonedDateTime.of(lastOpenDate, lastOpenTimeStart, ZoneId.of("UTC-4")),
                                ZonedDateTime.of(lastOpenDate, lastOpenTime, ZoneId.of("UTC-4")));

                    } catch (AlpacaAPIRequestException e) {
                        e.printStackTrace();
                    }
                } else {

                    try {
                        bars = alpacaAPI.getBars(timeFrame, ticker.get(), 1000, datetime, null, null, ZonedDateTime.now());

                    } catch (AlpacaAPIRequestException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (bars != null) {
                for (Bar bar : Objects.requireNonNull(bars.get(ticker.get()))) {
                    runOnUiThread(() -> selectedAdapterInitial.addVal(bar.getC().floatValue()));
                }
            }

            if (datetime.getYear() < ZonedDateTime.now().getYear()) {
                runOnUiThread(selectedAdapterInitial::smoothGraph);
            }
        });
        thread.start();
    }

    public void setDashboardColors(boolean pos, float profitLoss, float percentageChange) {

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.color_positive_light, typedValue, true);
        AtomicInteger posColorLight = new AtomicInteger(ContextCompat.getColor(this, typedValue.resourceId));
        getTheme().resolveAttribute(R.attr.color_negative_light, typedValue, true);
        AtomicInteger negColorLight = new AtomicInteger(ContextCompat.getColor(this, typedValue.resourceId));

        if (pos) {
            percentChangeStock.setText(String.format("+$%.2f (%.2f%%)", profitLoss, percentageChange));

            getTheme().resolveAttribute(R.attr.color_positive, typedValue, true);
            int color = ContextCompat.getColor(this, typedValue.resourceId);
            percentChangeStock.setTextColor(color);
            percentChangeStock.setBackgroundTintList(ColorStateList.valueOf(posColorLight.get()));
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.arrow_top_right);
            upArrow.setTint(color);
            percentChangeStock.setCompoundDrawablesWithIntrinsicBounds(null, null, upArrow, null);
            sparkViewStock.setLineColor(color);
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(posColorLight.get()));
            oneDayStock.setTextColor(color);
            oneWeekStock.setTextColor(color);
            oneMonthStock.setTextColor(color);
            threeMonthStock.setTextColor(color);
            oneYearStock.setTextColor(color);
            oneDayStock.setRippleColor(ColorStateList.valueOf(color));
            oneWeekStock.setRippleColor(ColorStateList.valueOf(color));
            oneMonthStock.setRippleColor(ColorStateList.valueOf(color));
            threeMonthStock.setRippleColor(ColorStateList.valueOf(color));
            oneYearStock.setRippleColor(ColorStateList.valueOf(color));

            getTheme().resolveAttribute(R.attr.color_positive_light, typedValue, true);
            posOrNegColorLight.set(ContextCompat.getColor(this, typedValue.resourceId));

        } else {
            percentChangeStock.setText(String.format("-$%.2f (%.2f%%)", Math.abs(profitLoss), percentageChange));

            getTheme().resolveAttribute(R.attr.color_negative, typedValue, true);
            int color = ContextCompat.getColor(this, typedValue.resourceId);
            percentChangeStock.setTextColor(color);
            percentChangeStock.setBackgroundTintList(ColorStateList.valueOf(negColorLight.get()));
            Drawable downArrow = ContextCompat.getDrawable(this, R.drawable.arrow_bottom_right);
            downArrow.setTint(color);
            percentChangeStock.setCompoundDrawablesWithIntrinsicBounds(null, null, downArrow, null);
            sparkViewStock.setLineColor(color);
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(negColorLight.get()));
            oneDayStock.setTextColor(color);
            oneWeekStock.setTextColor(color);
            oneMonthStock.setTextColor(color);
            threeMonthStock.setTextColor(color);
            oneYearStock.setTextColor(color);
            oneDayStock.setRippleColor(ColorStateList.valueOf(color));
            oneWeekStock.setRippleColor(ColorStateList.valueOf(color));
            oneMonthStock.setRippleColor(ColorStateList.valueOf(color));
            threeMonthStock.setRippleColor(ColorStateList.valueOf(color));
            oneYearStock.setRippleColor(ColorStateList.valueOf(color));

            getTheme().resolveAttribute(R.attr.color_negative_light, typedValue, true);
            posOrNegColorLight.set(ContextCompat.getColor(this, typedValue.resourceId));
        }
    }

    public void setStockValues() {

        // Set Line and Ticker Info
        float oldVal = selectedAdapterStock.getValue(0);
        float newVal = selectedAdapterStock.getValue(selectedAdapterStock.getCount() - 1);
        float percentageChange = (newVal - oldVal) / oldVal * 100;
        float profitLoss = selectedAdapterStock.getValue(selectedAdapterStock.getCount() - 1) - selectedAdapterStock.getValue(0);

        if (selectedAdapterStock.getCount() != 0) {

            // If positive
            if (profitLoss >= 0) {
                setDashboardColors(true, profitLoss, percentageChange);

            } else {
                setDashboardColors(false, profitLoss, percentageChange);
            }
        }

        DecimalFormat formatter = new DecimalFormat("$#,###.00");
        double amount = Double.parseDouble(String.valueOf(newVal));
        tickerViewStock.setText(formatter.format(amount));
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}