package com.money.manager.ex.reports;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.money.manager.ex.R;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.TransactionTypes;
import com.money.manager.ex.currency.CurrencyUtils;
import com.money.manager.ex.database.ViewMobileData;

import java.util.ArrayList;

/**
 * Categories report fragment.
 * Created by Alen Siljak on 06/07/2015.
 */
public class CategoriesReportFragment extends BaseReportFragment {
    private static final int GROUP_ID_CATEGORY = 0xFFFF;
    private LinearLayout mHeaderListView, mFooterListView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setListAdapter(null);
        setShowMenuItemSearch(true);
        //create header view
        mHeaderListView = (LinearLayout) addListViewHeaderFooter(R.layout.item_generic_report_2_columns);
        TextView txtColumn1 = (TextView) mHeaderListView.findViewById(R.id.textViewColumn1);
        TextView txtColumn2 = (TextView) mHeaderListView.findViewById(R.id.textViewColumn2);
        //set header
        txtColumn1.setText(R.string.category);
        txtColumn1.setTypeface(null, Typeface.BOLD);
        txtColumn2.setText(R.string.amount);
        txtColumn2.setTypeface(null, Typeface.BOLD);
        //add to listview
        getListView().addHeaderView(mHeaderListView);
        //create footer view
        mFooterListView = (LinearLayout) addListViewHeaderFooter(R.layout.item_generic_report_2_columns);
        txtColumn1 = (TextView) mFooterListView.findViewById(R.id.textViewColumn1);
        txtColumn2 = (TextView) mFooterListView.findViewById(R.id.textViewColumn2);
        //set footer
        txtColumn1.setText(R.string.total);
        txtColumn1.setTypeface(null, Typeface.BOLD_ITALIC);
        txtColumn2.setText(R.string.total);
        txtColumn2.setTypeface(null, Typeface.BOLD_ITALIC);
        //add to listview --> move to load finished
        //getListView().addFooterView(mFooterListView);
        //set adapter
        CategoriesReportAdapter adapter = new CategoriesReportAdapter(getActivity(), null);
        setListAdapter(adapter);
        //call super method
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        Core core = new Core(getActivity());
        // pie chart
        MenuItem itemChart = menu.findItem(R.id.menu_chart);
        if (itemChart != null) {
            itemChart.setVisible(!(((CategoriesReportActivity) getActivity()).mIsDualPanel));
            itemChart.setIcon(core.resolveIdAttribute(R.attr.ic_action_pie_chart));
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        switch (loader.getId()) {
            case ID_LOADER:
                //parse cursor for calculate total
                if (data == null) return;

                CurrencyUtils currencyUtils = new CurrencyUtils(getActivity().getApplicationContext());

                double totalAmount = 0;
                while (data.moveToNext()) {
                    totalAmount += data.getDouble(data.getColumnIndex("TOTAL"));
                }
                TextView txtColumn2 = (TextView) mFooterListView.findViewById(R.id.textViewColumn2);
                txtColumn2.setText(currencyUtils.getBaseCurrencyFormatted(totalAmount));

                // soved bug chart
                if (data.getCount() > 0) {
                    getListView().removeFooterView(mFooterListView);
                    getListView().addFooterView(mFooterListView);
                }

                if (((CategoriesReportActivity) getActivity()).mIsDualPanel) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            showChart();

                        }
                    }, 1000);
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_chart) {
            showChart();
        } else if (item.getItemId() < 0) { // category
            String whereClause = getWhereClause();
            if (!TextUtils.isEmpty(whereClause))
                whereClause += " AND ";
            else
                whereClause = "";
            whereClause += " " + ViewMobileData.CategID + "=" + Integer.toString(Math.abs(item.getItemId()));
            //create arguments
            Bundle args = new Bundle();
            args.putString(KEY_WHERE_CLAUSE, whereClause);
            //starts loader
            startLoader(args);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean onQueryTextChange(String newText) {
        //recall last where clause
        String whereClause = getWhereClause();
        if (whereClause == null) whereClause = "";

        int start = whereClause.indexOf("/** */");
        if (start > 0) {
            int end = whereClause.indexOf("/** */", start + 1) + "/** */".length();
            whereClause = whereClause.substring(0, start) + whereClause.substring(end);
            // trim some space
            whereClause = whereClause.trim();
        }

        if (!TextUtils.isEmpty(whereClause)) {
            whereClause += " /** */AND ";
        } else {
            whereClause = "/** */";
        }
        // use token to replace criteria
        whereClause += "(" + ViewMobileData.Category + " Like '%" + newText + "%' OR " + ViewMobileData.Subcategory + " Like '%" + newText + "%')/** */";

        //create arguments
        Bundle args = new Bundle();
        args.putString(KEY_WHERE_CLAUSE, whereClause);
        //starts loader
        startLoader(args);
        return super.onQueryTextChange(newText);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected String prepareQuery(String whereClause) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        ViewMobileData mobileData = new ViewMobileData();
        //data to compose builder
        String[] projectionIn = new String[]{"ROWID AS _id", ViewMobileData.CategID, ViewMobileData.Category, ViewMobileData.SubcategID, ViewMobileData.Subcategory, "SUM(" + ViewMobileData.AmountBaseConvRate + ") AS TOTAL"};
        String selection = ViewMobileData.Status + "<>'V' AND " + ViewMobileData.TransactionType + " IN ('Withdrawal', 'Deposit')";
        if (!TextUtils.isEmpty(whereClause)) {
            selection += " AND " + whereClause;
        }
        String groupBy = ViewMobileData.CategID + ", " + ViewMobileData.Category + ", " + ViewMobileData.SubcategID + ", " + ViewMobileData.Subcategory;
        String having = null;
        if (!TextUtils.isEmpty(((CategoriesReportActivity) getActivity()).mFilter)) {
            String filter = ((CategoriesReportActivity) getActivity()).mFilter;
            if (TransactionTypes.valueOf(filter).equals(TransactionTypes.Withdrawal)) {
                having = "SUM(" + ViewMobileData.AmountBaseConvRate + ") < 0";
            } else {
                having = "SUM(" + ViewMobileData.AmountBaseConvRate + ") > 0";
            }
        }
        String sortOrder = ViewMobileData.Category + ", " + ViewMobileData.Subcategory;
        String limit = null;
        //compose builder
        builder.setTables(mobileData.getSource());
        //return query
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return builder.buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
        } else {
            return builder.buildQuery(projectionIn, selection, null, groupBy, having, sortOrder, limit);
        }
    }

    public void showChart() {
        CategoriesReportAdapter adapter = (CategoriesReportAdapter) getListAdapter();
        if (adapter == null) return;
        Cursor cursor = adapter.getCursor();
        if (cursor == null) return;

        ArrayList<ValuePieEntry> arrayList = new ArrayList<>();
        CurrencyUtils currencyUtils = new CurrencyUtils(getActivity().getApplicationContext());

        // process cursor
        while (cursor.moveToNext()) {
            ValuePieEntry item = new ValuePieEntry();
            String category = cursor.getString(cursor.getColumnIndex(ViewMobileData.Category));
            if (!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory)))) {
                category += " : " + cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory));
            }
            // total
            double total = Math.abs(cursor.getDouble(cursor.getColumnIndex("TOTAL")));
            // check if category is empty
            if (TextUtils.isEmpty(category)) {
                category = getString(R.string.empty_category);
            }

            item.setText(category);
            item.setValue(total);
            item.setValueFormatted(currencyUtils.getCurrencyFormatted(currencyUtils.getBaseCurrencyId(), total));
            // add element
            arrayList.add(item);
        }

        Bundle args = new Bundle();
        args.putSerializable(PieChartFragment.KEY_CATEGORIES_VALUES, arrayList);
        //get fragment manager
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (fragmentManager != null) {
            PieChartFragment fragment;
            fragment = (PieChartFragment) fragmentManager.findFragmentByTag(IncomeVsExpensesChartFragment.class.getSimpleName());
            if (fragment == null) {
                fragment = new PieChartFragment();
            }
            fragment.setChartArguments(args);
            fragment.setDisplayHomeAsUpEnabled(true);

            if (fragment.isVisible()) fragment.onResume();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (((CategoriesReportActivity) getActivity()).mIsDualPanel) {
                fragmentTransaction.replace(R.id.fragmentChart, fragment, PieChartFragment.class.getSimpleName());
            } else {
                fragmentTransaction.replace(R.id.fragmentContent, fragment, PieChartFragment.class.getSimpleName());
                fragmentTransaction.addToBackStack(null);
            }
            fragmentTransaction.commit();
        }
    }

    @Override
    public String getSubTitle() {
        return null;
    }
}
