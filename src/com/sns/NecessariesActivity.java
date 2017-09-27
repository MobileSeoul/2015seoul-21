package com.sns;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.sns.core.API;

public class NecessariesActivity extends Activity {
	private Intent intent;
	private Context mContext;
	private RelativeLayout necessariesLayout; // root 레이아웃
	private MyScrollView scrollView; // LinearLayout을 담을 스크롤 뷰
	private LinearLayout contentLayout; // TableLayout을 담을 레이아웃
	private TableLayout tableLayout; // 마트별 정보를 담을 레이아웃
	private ImageView btnBack;
	private TextView textItem;
	private String[] optionDistrict;
	private ArrayAdapter<String> adapter; // 서울 지역구 목록
	private Spinner spinner;
	private String item;
	private String region;
	private int startIndex;
	private int endIndex;
	private List<Map<String, Object>> necessariesList;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_necessaries);

		initComponents();

		addEventListener();
	}

	private void initComponents() {
		mContext = this;
		intent = getIntent();
		item = intent.getStringExtra("item");
		region = null;
		btnBack = (ImageView) findViewById(R.id.buttonBack);
		textItem = (TextView) findViewById(R.id.textItem);
		textItem.setText("검색어 : " + item);
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				new SetTableAsyncTask().execute();
			}
		};

		optionDistrict = getResources().getStringArray(R.array.spinnerArrayDistrict);
		adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, optionDistrict);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner = (Spinner) findViewById(R.id.spinnerDistrict);
		spinner.setAdapter(adapter);

		necessariesLayout = (RelativeLayout) findViewById(R.id.necessaries_layout);
		createScrollView();
		createLinearLayout();
		scrollView.addView(contentLayout);
		necessariesLayout.addView(scrollView);

		tableLayout = new TableLayout(this);
		tableLayout.setShrinkAllColumns(true);
		tableLayout.setStretchAllColumns(true);
		necessariesList = new ArrayList<Map<String, Object>>();
	}

	// 스크롤뷰 생성
	private void createScrollView() {
		scrollView = new MyScrollView(this, handler);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.BELOW, R.id.activityTitle);
		scrollView.setLayoutParams(params);
	}

	// 테이블을 담을 LinearLayout 생성
	private void createLinearLayout() {
		contentLayout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		contentLayout.setOrientation(LinearLayout.VERTICAL);
		contentLayout.setLayoutParams(params);
	}

	private void addEventListener() {
		// btnBack 리스너
		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		// spinner 리스너
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedView, int position, long id) {

				if (spinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {

					if (spinner.getSelectedItemPosition() == 0) {
						region = null;
					} else {
						region = StringUtils.defaultString((String) spinner.getAdapter().getItem(spinner.getSelectedItemPosition()));
					}
					startIndex = 1;
					endIndex = 20;

					clearTable();

					// 로딩 다이얼로그를 UI에 적용시키고, Background에서 테이블 설정 작업을 한다
					new SetTableAsyncTask().execute();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {

			}
		});
	}

	/**
	 * API로부터 생필품정보를 받아와 리스트에 넣는다
	 */
	@SuppressWarnings("unchecked")
	private void setNecessariesList() {
		API api = new API();
		Map<String, Object> result = new HashMap<String, Object>();

		try {
			result = api.getNecessariesPrice(startIndex, endIndex, item, region);

			int listTotalCount = MapUtils.getInteger(result, "LIST_TOTAL_COUNT", -1);
			if (startIndex > listTotalCount) {
				return;
			}

			List<Map<String, Object>> listFromAPI = (List<Map<String, Object>>) MapUtils.getObject(result, "itemList", null);
			if (CollectionUtils.isEmpty(listFromAPI)) {
				return;
			}
			necessariesList.addAll(listFromAPI);

			startIndex += 20;
			endIndex += 20;

			if (CollectionUtils.isEmpty(necessariesList)) {
				Toast.makeText(getApplicationContext(), "마트/시장별 검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
				finish();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 테이블 로우 생성 및 테이블에 적재
	 */
	private void setTableRow() {
		if (CollectionUtils.isEmpty(necessariesList)) {
			return;
		}

		for (Map<String, Object> necessariesMap : necessariesList) {
			String martName = MapUtils.getString(necessariesMap, "M_NAME", "");
			String itemName = MapUtils.getString(necessariesMap, "A_NAME", "");
			String unit = MapUtils.getString(necessariesMap, "A_UNIT", "");
			String price = getformattedPrice(MapUtils.getString(necessariesMap, "A_PRICE", ""));
			String remarks = MapUtils.getString(necessariesMap, "ADD_COL", "");
			String date = MapUtils.getString(necessariesMap, "P_YEAR_MONTH", "");
			String district = MapUtils.getString(necessariesMap, "M_GU_NAME", "");

			// 판매처
			TableRow rowMartName = new TableRow(this);
			rowMartName.setGravity(Gravity.CENTER);
			TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams();
			tableRowParams.setMargins(0, 10, 0, 10);
			rowMartName.setLayoutParams(tableRowParams);
			rowMartName.setBackgroundColor(Color.parseColor("#F7F7F7"));

			TextView textMartName = new TextView(this);
			textMartName.setText(martName + " (" + district + ")");
			textMartName.setGravity(Gravity.CENTER);
			textMartName.setPadding(0, 7, 0, 7);
			textMartName.setTextColor(Color.parseColor("#333333"));
			textMartName.setPaintFlags(textMartName.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textMartName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);

			rowMartName.addView(textMartName);

			/*
			 * 생필품 상세 정보
			 */

			// 품목 이름
			TableRow rowItemName = new TableRow(this);

			TextView textItemNameTitle = new TextView(this);
			textItemNameTitle.setGravity(Gravity.CENTER);
			textItemNameTitle.setText("품목 이름");
			textItemNameTitle.setPadding(0, 0, 0, 5);
			textItemNameTitle.setTextColor(Color.parseColor("#333333"));
			textItemNameTitle.setPaintFlags(textItemNameTitle.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textItemNameTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textItemName = new TextView(this);
			textItemName.setGravity(Gravity.CENTER);
			textItemName.setText(itemName);
			textItemName.setPadding(0, 0, 0, 5);
			textItemName.setTextColor(Color.parseColor("#8F8F8F"));
			textItemName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			rowItemName.addView(textItemNameTitle);
			rowItemName.addView(textItemName);

			// 판매 규격
			TableRow rowUnit = new TableRow(this);

			TextView textUnitTitle = new TextView(this);
			textUnitTitle.setGravity(Gravity.CENTER);
			textUnitTitle.setText("판매 규격");
			textUnitTitle.setPadding(0, 0, 0, 5);
			textUnitTitle.setTextColor(Color.parseColor("#333333"));
			textUnitTitle.setPaintFlags(textUnitTitle.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textUnitTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textUnit = new TextView(this);
			textUnit.setGravity(Gravity.CENTER);
			textUnit.setText(unit);
			textUnit.setPadding(0, 0, 0, 5);
			textUnit.setTextColor(Color.parseColor("#8F8F8F"));
			textUnit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			rowUnit.addView(textUnitTitle);
			rowUnit.addView(textUnit);

			// 가격
			TableRow rowPrice = new TableRow(this);

			TextView textPriceTitle = new TextView(this);
			textPriceTitle.setGravity(Gravity.CENTER);
			textPriceTitle.setText("가격");
			textPriceTitle.setPadding(0, 0, 0, 5);
			textPriceTitle.setTextColor(Color.parseColor("#333333"));
			textPriceTitle.setPaintFlags(textPriceTitle.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textPriceTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textPrice = new TextView(this);
			textPrice.setGravity(Gravity.CENTER);
			textPrice.setText(price);
			textPrice.setPadding(0, 0, 0, 5);
			textPrice.setTextColor(Color.parseColor("#8F8F8F"));
			textPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			rowPrice.addView(textPriceTitle);
			rowPrice.addView(textPrice);

			// 점검 일자
			TableRow rowDate = new TableRow(this);

			TextView textDateTitle = new TextView(this);
			textDateTitle.setGravity(Gravity.CENTER);
			textDateTitle.setText("점검 일자");
			textDateTitle.setPadding(0, 0, 0, 5);
			textDateTitle.setTextColor(Color.parseColor("#333333"));
			textDateTitle.setPaintFlags(textDateTitle.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textDateTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textDate = new TextView(this);
			textDate.setGravity(Gravity.CENTER);
			textDate.setText(date);
			textDate.setPadding(0, 0, 0, 5);
			textDate.setTextColor(Color.parseColor("#8F8F8F"));
			textDate.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			rowDate.addView(textDateTitle);
			rowDate.addView(textDate);

			// 비고
			TableRow rowRemarks = new TableRow(this);

			TextView textRemarksTitle = new TextView(this);
			textRemarksTitle.setGravity(Gravity.CENTER);
			textRemarksTitle.setText("비고");
			textRemarksTitle.setPadding(0, 0, 0, 5);
			textRemarksTitle.setTextColor(Color.parseColor("#333333"));
			textRemarksTitle.setPaintFlags(textRemarksTitle.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textRemarksTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textRemarks = new TextView(this);
			textRemarks.setGravity(Gravity.CENTER);
			textRemarks.setText(remarks);
			textRemarks.setPadding(0, 0, 0, 5);
			textRemarks.setTextColor(Color.parseColor("#8F8F8F"));
			textRemarks.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

			rowRemarks.addView(textRemarksTitle);
			rowRemarks.addView(textRemarks);

			// tableLayout에 추가
			tableLayout.addView(rowMartName);
			tableLayout.addView(rowItemName);
			tableLayout.addView(rowUnit);
			tableLayout.addView(rowPrice);
			tableLayout.addView(rowDate);
			tableLayout.addView(rowRemarks);
		}
	}

	/**
	 * 테이블 내용물 삭제
	 */
	private void clearTable() {
		tableLayout.removeAllViews();
		contentLayout.removeAllViews();
	}

	private String getformattedPrice(String price) {
		if (StringUtils.isEmpty(price)) {
			return "-";
		}

		if ("0".equals(price)) {
			return "-";
		}

		String formattedPrice = "";

		try {
			DecimalFormat df = new DecimalFormat("##,###");
			formattedPrice = df.format(Integer.parseInt(price, 10)) + "원";
		} catch (Exception e) {
			return "-";
		}

		return formattedPrice;
	}

	/**
	 * 로딩 다이얼로그를 UI에 적용시키고, Background에서 테이블 설정 작업을 한다.
	 */
	private class SetTableAsyncTask extends AsyncTask<Void, Void, Void> {

		Dialog loadingDialog = new Dialog(mContext, R.style.LoadingDialog);

		public SetTableAsyncTask() {
			loadingDialog.setCancelable(false);
			loadingDialog.addContentView(new ProgressBar(mContext), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// show dialog
			loadingDialog.show();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// 생필품 리스트 세팅
			setNecessariesList();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			// 테이블 세팅
			setTableRow();

			// tableLayout.setGravity(Gravity.CENTER);
			contentLayout.removeAllViews();
			contentLayout.addView(tableLayout);

			// dismiss dialog
			loadingDialog.dismiss();
			necessariesList.clear();
		}
	}
}