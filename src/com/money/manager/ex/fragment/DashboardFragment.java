package com.money.manager.ex.fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.money.manager.ex.R;
import com.money.manager.ex.core.Core;
import com.money.manager.ex.core.CurrencyUtils;
import com.money.manager.ex.database.QueryBillDeposits;
import com.money.manager.ex.database.QueryReportIncomeVsExpenses;
import com.money.manager.ex.database.SQLDataSet;
import com.money.manager.ex.database.ViewMobileData;
import com.money.manager.ex.view.RobotoTextView;

public class DashboardFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	@SuppressWarnings("unused")
	private static final String LOGCAT = DashboardFragment.class.getSimpleName();
	// ID LOADER
	private static final int ID_LOADER_SCREEN1 = 0x000;
	private static final int ID_LOADER_SCREEN2 = 0x001;
	private static final int ID_LOADER_SCREEN3 = 0x002;
	private static final int ID_LOADER_SCREEN4 = 0x003;
	// Padding
	final int padding_in_dp = 6; // 6 dps
	float scale;
	int padding_in_px;

	// array of part screen
	LinearLayout[] linearScreens;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//set has option menu to close dashboard item
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
			return null;
		// parse layout
		ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.dashboard_fragment, container, false);
		if (layout == null)
			return null;
		linearScreens = new LinearLayout[ID_LOADER_SCREEN4 + 1];
		// get sub linearlayout
		linearScreens[ID_LOADER_SCREEN1] = (LinearLayout) layout.findViewById(R.id.linearLayoutScreen1);
		linearScreens[ID_LOADER_SCREEN2] = (LinearLayout) layout.findViewById(R.id.linearLayoutScreen2);
		linearScreens[ID_LOADER_SCREEN3] = (LinearLayout) layout.findViewById(R.id.linearLayoutScreen3);
		linearScreens[ID_LOADER_SCREEN4] = (LinearLayout) layout.findViewById(R.id.linearLayoutScreen4);
		// calculate padding
		scale = getResources().getDisplayMetrics().density;
		padding_in_px = (int) (padding_in_dp * scale + 0.5f);
		
		return layout;
	}

	@Override
	public void onResume() {
		super.onResume();
		loadData();
	}
	
	@Override
	public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		super.onPrepareOptionsMenu(menu);
		//find menu dashboard
		MenuItem itemDashboard = menu.findItem(R.id.menu_dashboard);
		if (itemDashboard != null)
			itemDashboard.setVisible(false);
	}
	
	public void loadData() {
		// restart loader
		if (linearScreens[ID_LOADER_SCREEN1].getVisibility() == View.VISIBLE)
			getLoaderManager().restartLoader(ID_LOADER_SCREEN1, null, this);
		
		if (linearScreens[ID_LOADER_SCREEN2].getVisibility() == View.VISIBLE)
			getLoaderManager().restartLoader(ID_LOADER_SCREEN2, null, this);
		
		if (linearScreens[ID_LOADER_SCREEN3].getVisibility() == View.VISIBLE)
			getLoaderManager().restartLoader(ID_LOADER_SCREEN3, null, this);
		
		if (linearScreens[ID_LOADER_SCREEN4].getVisibility() == View.VISIBLE)
			getLoaderManager().restartLoader(ID_LOADER_SCREEN4, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// add progress bar
		ProgressBar progressBar = new ProgressBar(getSherlockActivity());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParams.gravity = Gravity.CENTER;
		progressBar.setLayoutParams(layoutParams);
		progressBar.setIndeterminate(true);
		
		if (id <= ID_LOADER_SCREEN4) {
			linearScreens[id].removeAllViews();
			// add view
			linearScreens[id].addView(progressBar);
		}
		// start loader
		switch (id) {
		case ID_LOADER_SCREEN1:
			QueryReportIncomeVsExpenses report = new QueryReportIncomeVsExpenses(getSherlockActivity());
			return new CursorLoader(getActivity(), report.getUri(), report.getAllColumns(), QueryReportIncomeVsExpenses.Month + "="
					+ Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1) + " AND " + QueryReportIncomeVsExpenses.Year + "="
					+ Integer.toString(Calendar.getInstance().get(Calendar.YEAR)), null, null);
		case ID_LOADER_SCREEN2:
			return new CursorLoader(getActivity(), new SQLDataSet().getUri(), null, prepareQueryTopWithdrawals(), null, null);
		case ID_LOADER_SCREEN3:
			return new CursorLoader(getActivity(), new SQLDataSet().getUri(), null, prepareQueryTopPayees(), null, null);
		case ID_LOADER_SCREEN4:
			QueryBillDeposits billDeposits = new QueryBillDeposits(getSherlockActivity());
			return new CursorLoader(getActivity(), billDeposits.getUri(), billDeposits.getAllColumns(), QueryBillDeposits.DAYSLEFT + "<=10", null, QueryBillDeposits.DAYSLEFT);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (loader.getId() <= ID_LOADER_SCREEN4)
			linearScreens[loader.getId()].removeAllViews();

		switch (loader.getId()) {
		case ID_LOADER_SCREEN1:
			showChartIncomeVsExpensesCurrentMonth(data);
			break;
		case ID_LOADER_SCREEN2:
			linearScreens[loader.getId()].addView(showTableLayoutTopWithdrawals(data));
			break;
		case ID_LOADER_SCREEN3:
			linearScreens[loader.getId()].addView(showTableLayoutTopPayees(data));
			break;
		case ID_LOADER_SCREEN4:
			linearScreens[loader.getId()].addView(showTableLayoutUpComingTransactions(data));
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	@SuppressWarnings("deprecation")
	private String prepareQueryTopWithdrawals() {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		ViewMobileData mobileData = new ViewMobileData();
		// data to compose builder
		String[] projectionIn = new String[] { "ROWID AS _id", ViewMobileData.CategID, ViewMobileData.Category, ViewMobileData.SubcategID,
				ViewMobileData.Subcategory, "SUM(" + ViewMobileData.AmountBaseConvRate + ") AS TOTAL", "COUNT(*) AS NUM" };

		String selection = ViewMobileData.Status + "<>'V' AND " + ViewMobileData.TransactionType + " IN ('Withdrawal')"
				+ " AND (julianday(date('now')) - julianday(" + ViewMobileData.Date + ") <= 30)";

		String groupBy = ViewMobileData.CategID + ", " + ViewMobileData.Category + ", " + ViewMobileData.SubcategID + ", " + ViewMobileData.Subcategory;
		String having = "SUM(" + ViewMobileData.AmountBaseConvRate + ") < 0";
		String sortOrder = "ABS(SUM(" + ViewMobileData.AmountBaseConvRate + ")) DESC";
		String limit = "10";
		// compose builder
		builder.setTables(mobileData.getSource());
		// return query
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			return builder.buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
		} else {
			return builder.buildQuery(projectionIn, selection, null, groupBy, having, sortOrder, limit);
		}
	}

	@SuppressWarnings("deprecation")
	private String prepareQueryTopPayees() {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		ViewMobileData mobileData = new ViewMobileData();
		// data to compose builder
		String[] projectionIn = new String[] { "ROWID AS _id", ViewMobileData.PayeeID, ViewMobileData.Payee,
				"ABS(SUM(" + ViewMobileData.AmountBaseConvRate + ")) AS TOTAL", "COUNT(*) AS NUM" };

		String selection = ViewMobileData.Status + "<>'V' AND " + ViewMobileData.TransactionType
				+ " IN ('Withdrawal', 'Deposit') AND (julianday(date('now')) - julianday(" + ViewMobileData.Date + ") <= 30)";

		String groupBy = ViewMobileData.PayeeID + ", " + ViewMobileData.Payee;
		String having = null;
		String sortOrder = "ABS(SUM(" + ViewMobileData.AmountBaseConvRate + ")) DESC";
		String limit = "10";
		// compose builder
		builder.setTables(mobileData.getSource());
		// return query
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			return builder.buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
		} else {
			return builder.buildQuery(projectionIn, selection, null, groupBy, having, sortOrder, limit);
		}
	}

	/*
	 * Show Chart of Income Vs. Expenses Cur
	 */
	private void showChartIncomeVsExpensesCurrentMonth(Cursor cursor) {
		// move to first
		if (!cursor.moveToFirst())
			return;
		// arrays
		double[] incomes = new double[3];
		double[] expenses = new double[3];
		String[] titles = new String[3];

		// incomes and expenses
		incomes[1] = cursor.getFloat(cursor.getColumnIndex(QueryReportIncomeVsExpenses.Income));
		expenses[1] = Math.abs(cursor.getFloat(cursor.getColumnIndex(QueryReportIncomeVsExpenses.Expenses)));
		// titles
		int year = cursor.getInt(cursor.getColumnIndex(QueryReportIncomeVsExpenses.Year));
		int month = cursor.getInt(cursor.getColumnIndex(QueryReportIncomeVsExpenses.Month));
		// format month
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month - 1, 1);
		// titles
		titles[1] = Integer.toString(year) + "-" + new SimpleDateFormat("MMM").format(calendar.getTime());

		// compose bundle for arguments
		Bundle args = new Bundle();
		args.putDoubleArray(IncomeVsExpensesChartFragment.KEY_EXPENSES_VALUES, expenses);
		args.putDoubleArray(IncomeVsExpensesChartFragment.KEY_INCOME_VALUES, incomes);
		args.putStringArray(IncomeVsExpensesChartFragment.KEY_XTITLES, titles);
		args.putString(IncomeVsExpensesChartFragment.KEY_TITLE, getString(R.string.income_vs_expenses_current_month));
		args.putBoolean(IncomeVsExpensesChartFragment.KEY_DISPLAY_AS_UP_ENABLED, false);

		// get fragment manager
		FragmentManager fragmentManager = getChildFragmentManager();
		if (fragmentManager != null) {
			IncomeVsExpensesChartFragment fragment;
			
			fragment = new IncomeVsExpensesChartFragment();
			fragment.setChartArguments(args);

			if (fragment.isVisible())
				fragment.onResume();

			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			fragmentTransaction.replace(R.id.linearLayoutScreen1, fragment, IncomeVsExpensesChartFragment.class.getSimpleName());

			fragmentTransaction.commit();
		}
	}

	private View showTableLayoutTopWithdrawals(Cursor cursor) {
		LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dashboard_summary_layout, null);
		CurrencyUtils currencyUtils = new CurrencyUtils(getSherlockActivity());		

		// Textview Title
		TextView title = (TextView) layout.findViewById(R.id.textViewTitle);
		title.setText(R.string.top_withdrawals_last_30_days);
		// Table
		TableLayout tableLayout = (TableLayout) layout.findViewById(R.id.tableLayoutSummary);
		// Create Title
		tableLayout.addView(createTableRow(new String[] { "<small><b>" + getString(R.string.category) + "</b></small>",
				"<small><b>" + getString(R.string.quantity) + "</b></small>", "<small><b>" + getString(R.string.summary) + "</b></small>" }, new Float[] { 1f,
				null, null }, new Integer[] { null, Gravity.RIGHT, Gravity.RIGHT }, new Integer[][] { null, { 0, 0, padding_in_px, 0 }, null }));
		// add rows
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				// load values
				String category = "<b>" + cursor.getString(cursor.getColumnIndex(ViewMobileData.Category)) + "</b>";
				if (!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory)))) {
					category += " : " + cursor.getString(cursor.getColumnIndex(ViewMobileData.Subcategory));
				}
				float total = cursor.getFloat(cursor.getColumnIndex("TOTAL"));
				int num = cursor.getInt(cursor.getColumnIndex("NUM"));
				// Add Row
				tableLayout.addView(createTableRow(new String[] { "<small>" + category + "</small>", "<small><i>" + Integer.toString(num) + "</i></small>",
						"<small>" + currencyUtils.getCurrencyFormatted(currencyUtils.getBaseCurrencyId(), total) + "</small>" }, new Float[] { 1f, null, null },
						new Integer[] { null, Gravity.RIGHT, Gravity.RIGHT }, new Integer[][] { null, { 0, 0, padding_in_px, 0 }, null }));
				// move to nextrow
				cursor.moveToNext();
			}
		}
		// return Layout
		return layout;
	}

	private View showTableLayoutTopPayees(Cursor cursor) {
		LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dashboard_summary_layout, null);
		CurrencyUtils currencyUtils = new CurrencyUtils(getSherlockActivity());

		// Textview Title
		TextView title = (TextView) layout.findViewById(R.id.textViewTitle);
		title.setText(R.string.top_payees_last_30_days);
		// Table
		TableLayout tableLayout = (TableLayout) layout.findViewById(R.id.tableLayoutSummary);
		// Create Title
		tableLayout.addView(createTableRow(new String[] { "<small><b>" + getString(R.string.payee) + "</b></small>",
				"<small><b>" + getString(R.string.quantity) + "</b></small>", "<small><b>" + getString(R.string.summary) + "</b></small>" }, new Float[] { 1f,
				null, null }, new Integer[] { null, Gravity.RIGHT, Gravity.RIGHT }, new Integer[][] { null, { 0, 0, padding_in_px, 0 }, null }));
		// add rows
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				// load values
				String payee = cursor.getString(cursor.getColumnIndex(ViewMobileData.Payee));
				float total = cursor.getFloat(cursor.getColumnIndex("TOTAL"));
				int num = cursor.getInt(cursor.getColumnIndex("NUM"));
				// Add Row
				tableLayout.addView(createTableRow(new String[] { "<small>" + payee + "</small>", "<small><i>" + Integer.toString(num) + "</i></small>",
						"<small>" + currencyUtils.getCurrencyFormatted(currencyUtils.getBaseCurrencyId(), total) + "</small>" }, new Float[] { 1f, null, null },
						new Integer[] { null, Gravity.RIGHT, Gravity.RIGHT }, new Integer[][] { null, { 0, 0, padding_in_px, 0 }, null }));
				// move to nextrow
				cursor.moveToNext();
			}
		}
		// return Layout
		return layout;
	}
	
	private View showTableLayoutUpComingTransactions(Cursor cursor) {
		LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dashboard_summary_layout, null);
		CurrencyUtils currencyUtils = new CurrencyUtils(getSherlockActivity());
		Core core = new Core(getSherlockActivity());
		
		// Textview Title
		TextView title = (TextView) layout.findViewById(R.id.textViewTitle);
		title.setText(R.string.upcoming_transactions);
		// Table
		TableLayout tableLayout = (TableLayout) layout.findViewById(R.id.tableLayoutSummary);
		// add rows
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				// load values
				String payee = "<i>" + cursor.getString(cursor.getColumnIndex(QueryBillDeposits.PAYEENAME)) + "</i>";
				float total = cursor.getFloat(cursor.getColumnIndex(QueryBillDeposits.AMOUNT));
				int daysLeft = cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.DAYSLEFT));
				int currencyId = cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.CURRENCYID));
				String daysLeftText = "";
				daysLeftText = Integer.toString(Math.abs(daysLeft)) + " " + getString(daysLeft >=0 ? R.string.days_remaining : R.string.days_overdue);
				TableRow row = createTableRow(new String[] { "<small>" + payee + "</small>",
						  "<small>" + currencyUtils.getCurrencyFormatted(currencyId, total) + "</small>",
						  "<small>" + daysLeftText + "</small>"}, new Float[] { 1f, null, 1f },
						  new Integer[] { null, Gravity.RIGHT, Gravity.RIGHT }, new Integer[][] { null, { 0, 0, padding_in_px, 0 }, null });
				TextView txt = (TextView)row.getChildAt(2);
				txt.setTextColor(getResources().getColor(daysLeft >= 0 ? core.resolveIdAttribute(R.attr.holo_green_color_theme) : core.resolveIdAttribute(R.attr.holo_red_color_theme)));
				// Add Row
				tableLayout.addView(row);
				// move to nextrow
				cursor.moveToNext();
			}
		}
		// return Layout
		return layout;
	}

	private TableRow createTableRow(String[] fields, Float[] weight, Integer[] gravity, Integer[][] margin) {
		// create row
		TableRow row = new TableRow(getSherlockActivity());
		row.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		for (int i = 0; i < fields.length; i++) {
			RobotoTextView txtField = new RobotoTextView(getSherlockActivity(), null);
			TableRow.LayoutParams layoutParams;
			if (weight[i] != null) {
				layoutParams = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, weight[i]);
			} else {
				layoutParams = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			}
			// set margin
			if (margin[i] != null)
				layoutParams.setMargins(margin[i][0], margin[i][1], margin[i][2], margin[i][3]);
			txtField.setLayoutParams(layoutParams);
			if (gravity[i] != null)
				txtField.setGravity(gravity[i]);
			// set text
			txtField.setText(Html.fromHtml(fields[i]));
			// set singleline
			txtField.setSingleLine(true);
			// add field
			row.addView(txtField);
		}
		// return row
		return row;
	}
}
