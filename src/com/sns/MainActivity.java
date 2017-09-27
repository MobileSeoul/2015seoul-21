package com.sns;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.sns.core.API;
import com.sns.core.BackPressCloseHandler;

public class MainActivity extends Activity {
	private BackPressCloseHandler backPressCloseHandler;
	private RelativeLayout topLayout; // 검색기능이 위치하는 레이아웃
	private RelativeLayout necessaryLayoutTitle;
	private RelativeLayout necessaryLayout;
	private RelativeLayout contentLayoutTitle;
	private LinearLayout contentLayout;
	private TableLayout tableLayout; // 테이블 레이아웃
	private EditText editText;
	private TextView textContentLayoutTitle;
	private TextView necessarySampleText;
	private Button btnSearch;
	private Button btnMoreNecessaries;
	private Animation animation;
	private boolean isAlreadyAnimated;
	private Context mContext; // MainActivity의 context
	private String query;
	private List<Map<String, Object>> necessaryList;
	private List<Map<String, Object>> itemList;
	private List<TableRow> tableRowList;
	private Map<String, Object> result;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 3초간 스플래시 액티비티 노출
		startActivity(new Intent(this, Splash.class));

		// 메인 액티비티에서 사용하는 컴포넌트 초기화
		initComponents();

		// 이벤트 리스너 등록
		addEventListener();
	}

	private void addEventListener() {
		// 검색버튼 리스너
		btnSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSearch();
			}
		});

		// 더보기 버튼 리스너
		btnMoreNecessaries.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 새로운 액티비티에서 생필품 정보 출력
				Intent intent = new Intent(mContext, NecessariesActivity.class);
				intent.putExtra("item", query);
				startActivity(intent);
			}
		});

		// 엔터키 리스너
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				switch (actionId) {
				case EditorInfo.IME_ACTION_SEARCH:
					doSearch();
					break;
				default:
					return false;
				}
				return true;
			}
		});

		// 애니메이션 리스너
		animation.setAnimationListener(new Animation.AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				isAlreadyAnimated = true;
				editText.setEnabled(false);
				btnSearch.setEnabled(false);

				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				topLayout.setLayoutParams(layoutParams);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {

				new SetLayoutAsyncTask().execute();

				necessaryLayoutTitle.setVisibility(View.VISIBLE);
				necessaryLayout.setVisibility(View.VISIBLE);
				contentLayoutTitle.setVisibility(View.VISIBLE);
				contentLayout.setVisibility(View.VISIBLE);
				editText.setEnabled(true);
				btnSearch.setEnabled(true);
			}
		});
	}

	private void initComponents() {
		backPressCloseHandler = new BackPressCloseHandler(this);
		mContext = this;

		topLayout = (RelativeLayout) findViewById(R.id.topLayout);
		necessaryLayoutTitle = (RelativeLayout) findViewById(R.id.necessaryLayoutTitle);
		necessaryLayout = (RelativeLayout) findViewById(R.id.necessaryLayout);
		contentLayoutTitle = (RelativeLayout) findViewById(R.id.contentLayoutTitle);
		contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
		editText = (EditText) findViewById(R.id.editText);
		btnSearch = (Button) findViewById(R.id.btnSearch);
		btnMoreNecessaries = (Button) findViewById(R.id.btnMoreNecessaries);
		animation = AnimationUtils.loadAnimation(this, R.anim.move_up);
		necessarySampleText = (TextView) findViewById(R.id.necessaryText);
		textContentLayoutTitle = (TextView) findViewById(R.id.textContentLayoutTitle);

		necessaryLayoutTitle.setVisibility(View.INVISIBLE);
		necessaryLayout.setVisibility(View.INVISIBLE);
		contentLayoutTitle.setVisibility(View.INVISIBLE);
		contentLayout.setVisibility(View.INVISIBLE);
		btnMoreNecessaries.setSelected(true);

		isAlreadyAnimated = false;

		tableLayout = new TableLayout(this);
		tableLayout.setShrinkAllColumns(false);
		tableLayout.setStretchAllColumns(true);

		necessaryList = new ArrayList<Map<String, Object>>();
		itemList = new ArrayList<Map<String, Object>>();
		tableRowList = new ArrayList<TableRow>();
		result = new HashMap<String, Object>();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * 뒤로가기 버튼을 두번 눌렀을 때 앱 종료
	 */
	@Override
	public void onBackPressed() {
		backPressCloseHandler.onBackPressed();
	}

	/**
	 * 앱에서 사용하는 API는 일요일에는 제공하지 않는다. 앱을 사용하는 날이 일요일인지 체크한다.
	 */
	private boolean isItSundayToday() {
		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		switch (day) {
		case Calendar.SUNDAY:
			return true;
		default:
			return false;
		}
	}

	/**
	 * 검색
	 */
	public void doSearch() {
		if (isItSundayToday()) {
			Toast.makeText(getApplicationContext(), "'일요일'에는 데이터가 제공되지않아 앱을 사용할 수 없습니다", Toast.LENGTH_SHORT).show();
		} else {
			query = editText.getText().toString();
			if (StringUtils.isEmpty(query)) {
				Toast.makeText(getApplicationContext(), "품목을 입력해주세요", Toast.LENGTH_SHORT).show();
				return;
			}

			// 키보드(키패드) 내리기
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

			// topLayout 애니메이션은 한 번만 사용된다
			if (isAlreadyAnimated == false) {
				topLayout.startAnimation(animation);
			} else {

				new SetLayoutAsyncTask().execute();
			}
		}
	}

	/**
	 * 생필품 정보 예시 받아오기
	 */
	@SuppressWarnings("unchecked")
	private void getNecessaryPrice() {
		try {
			result = new API().getNecessariesPrice(1, 1, query, null);

			if (MapUtils.isNotEmpty(result)) {
				necessaryList = (List<Map<String, Object>>) MapUtils.getObject(result, "itemList", null);
				if (CollectionUtils.isEmpty(necessaryList)) {
					return;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 생필품 정보 예시 노출
	 */
	private void setNecessaryLayout() {
		try {
			if (CollectionUtils.isNotEmpty(necessaryList)) {

				Map<String, Object> map = necessaryList.get(0);
				if (MapUtils.isNotEmpty(map)) {
					String str = MapUtils.getString(map, "A_NAME", "");
					str = str + "  /  " + getformattedPrice(MapUtils.getString(map, "A_PRICE", ""));
					str = str + "  /  " + MapUtils.getString(map, "A_UNIT", "");
					str = str + "  /  " + MapUtils.getString(map, "M_NAME", "");
					str = str + "  /  " + MapUtils.getString(map, "M_GU_NAME", "");
					str = str + "  /  조사일자 : " + MapUtils.getString(map, "P_YEAR_MONTH", "");

					necessarySampleText.setText(str);
					necessarySampleText.setTextColor(Color.parseColor("#8F8F8F"));
					necessarySampleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
					necessarySampleText.setSelected(true);

					necessaryLayoutTitle.setVisibility(View.VISIBLE);
					necessaryLayout.setVisibility(View.VISIBLE);
					btnMoreNecessaries.setVisibility(View.VISIBLE);
				}
			} else {
				necessaryLayoutTitle.setVisibility(View.GONE);
				btnMoreNecessaries.setVisibility(View.GONE);
				necessaryLayout.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 서울시 농수산물 등급별 가격 조회 후 itemList에 추가
	 */
	@SuppressWarnings("unchecked")
	private void setItemList() {
		API api = new API();

		int startIndex = 1;
		int endIndex = 10;

		try {
			// API 조회
			Map<String, Object> itemMap = api.getGarakGradePrice(startIndex, endIndex, query);

			// itemList에 품목 추가
			List<Map<String, Object>> listFromAPI = (List<Map<String, Object>>) MapUtils.getObject(itemMap, "itemList");
			if (CollectionUtils.isEmpty(listFromAPI)) {
				return;
			}

			itemList.addAll(listFromAPI);

			// LIST_TOTAL_COUNT가 itemList보다 많으면 개수가 같아질 때까지 호출
			int itemListSize = itemList.size();
			int listTotalCount = MapUtils.getIntValue(itemMap, "LIST_TOTAL_COUNT", -1);

			if (itemListSize >= listTotalCount) {
				return;
			}

			int diff = listTotalCount - itemListSize;

			while (diff > 0) {
				if (diff > 1000) {
					diff = 1000;
				}
				startIndex = endIndex + 1;
				endIndex += diff;

				itemMap = api.getGarakGradePrice(startIndex, endIndex, query);

				listFromAPI = (List<Map<String, Object>>) MapUtils.getObject(itemMap, "itemList");
				if (CollectionUtils.isEmpty(listFromAPI)) {
					return;
				}

				itemList.addAll(listFromAPI);

				itemListSize = itemList.size();
				diff = listTotalCount - itemListSize;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 테이블 리스트 추가
	 */
	@SuppressWarnings("unchecked")
	private void setTableRowList() {
		if (CollectionUtils.isEmpty(itemList)) {
			return;
		}

		Map<String, List<Map<String, Object>>> classifiedByPumName = new HashMap<String, List<Map<String, Object>>>();

		/*
		 * 품목명이 key로된 Map 생성
		 * 
		 * ex) "배추" -> [{특,8,kg상자,12021}, {상,...}, ...]
		 */

		for (Map<String, Object> item : itemList) {
			String pumName = MapUtils.getString(item, "PUMNAME", "");
			String gradeName = MapUtils.getString(item, "GRADENAME", "");
			String unitQty = MapUtils.getString(item, "UNITQTY", "");
			String unitName = MapUtils.getString(item, "UNITNAME", "");
			String avgPrice = MapUtils.getString(item, "AVGPRICE", "");

			List<Map<String, Object>> list = (List<Map<String, Object>>) MapUtils.getObject(classifiedByPumName, pumName, null);

			// 해당 품목명이 Map에 없으면 새로운 List 추가
			if (CollectionUtils.isEmpty(list)) {
				List<Map<String, Object>> newList = new ArrayList<Map<String, Object>>();
				Map<String, Object> subMap = new HashMap<String, Object>();

				subMap.put("GRADENAME", gradeName);
				subMap.put("UNITQTY", unitQty);
				subMap.put("UNITNAME", unitName);
				subMap.put("AVGPRICE", avgPrice);

				newList.add(subMap);

				classifiedByPumName.put(pumName, newList);

			} else {
				// 해당 품목명이 Map에 이미 있으면 기존 List에 추가
				Map<String, Object> subMap = new HashMap<String, Object>();

				subMap.put("GRADENAME", gradeName);
				subMap.put("UNITQTY", unitQty);
				subMap.put("UNITNAME", unitName);
				subMap.put("AVGPRICE", avgPrice);

				list.add(subMap);

				classifiedByPumName.put(pumName, list);
			}

		}

		/*
		 * 품목별 Map으로 TableRowList 생성
		 */
		for (String pumName : classifiedByPumName.keySet()) {

			List<Map<String, Object>> itemInfo = (List<Map<String, Object>>) MapUtils.getObject(classifiedByPumName, pumName);
			if (CollectionUtils.isEmpty(itemInfo)) {
				continue;
			}

			// 품목 row
			TextView textPumName = new TextView(this);
			textPumName.setText(pumName);
			textPumName.setPaintFlags(textPumName.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textPumName.setGravity(Gravity.CENTER);
			textPumName.setPadding(0, 10, 0, 10);
			textPumName.setTextColor(Color.parseColor("#333333"));
			textPumName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);

			TableRow rowPumName = new TableRow(this);
			TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams();
			tableRowParams.setMargins(0, 10, 0, 0);
			rowPumName.setLayoutParams(tableRowParams);
			rowPumName.setBackgroundColor(Color.parseColor("#F7F7F7"));
			rowPumName.setGravity(Gravity.CENTER);
			rowPumName.addView(textPumName);

			tableRowList.add(rowPumName);

			// 등급별 row
			TextView textColumnGrade = new TextView(this);
			textColumnGrade.setText("등급");
			textColumnGrade.setGravity(Gravity.CENTER);
			textColumnGrade.setPadding(0, 7, 0, 7);
			textColumnGrade.setTextColor(Color.parseColor("#333333"));
			textColumnGrade.setPaintFlags(textColumnGrade.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textColumnGrade.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textColumnUnit = new TextView(this);
			textColumnUnit.setText("단위");
			textColumnUnit.setGravity(Gravity.CENTER);
			textColumnUnit.setPadding(0, 7, 0, 7);
			textColumnUnit.setTextColor(Color.parseColor("#333333"));
			textColumnUnit.setPaintFlags(textColumnUnit.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textColumnUnit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TextView textColumnPrice = new TextView(this);
			textColumnPrice.setText("평균가");
			textColumnPrice.setGravity(Gravity.CENTER);
			textColumnPrice.setPadding(0, 7, 0, 7);
			textColumnPrice.setTextColor(Color.parseColor("#333333"));
			textColumnPrice.setPaintFlags(textColumnPrice.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
			textColumnPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

			TableRow rowColumnName = new TableRow(this);
			rowColumnName.addView(textColumnGrade);
			rowColumnName.addView(textColumnUnit);
			rowColumnName.addView(textColumnPrice);

			tableRowList.add(rowColumnName);

			for (Map<String, Object> item : itemInfo) {
				TableRow rowDetail = new TableRow(this);

				String gradeName = MapUtils.getString(item, "GRADENAME", "");
				String unitQty = MapUtils.getString(item, "UNITQTY", "");
				String unitName = MapUtils.getString(item, "UNITNAME", "");
				String avgPrice = MapUtils.getString(item, "AVGPRICE", "");

				avgPrice = getformattedPrice(avgPrice);

				// 등급
				TextView textGrade = new TextView(this);
				textGrade.setText(gradeName);
				textGrade.setGravity(Gravity.CENTER);
				textGrade.setPadding(0, 0, 0, 5);
				textGrade.setTextColor(Color.parseColor("#8F8F8F"));
				textGrade.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
				rowDetail.addView(textGrade);

				// 단위
				TextView textUnit = new TextView(this);
				textUnit.setText(unitQty + " " + unitName);
				textUnit.setGravity(Gravity.CENTER);
				textUnit.setPadding(0, 0, 0, 5);
				textUnit.setTextColor(Color.parseColor("#8F8F8F"));
				textUnit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
				rowDetail.addView(textUnit);

				// 평균가
				TextView textPrice = new TextView(this);
				textPrice.setText(avgPrice);
				textPrice.setGravity(Gravity.CENTER);
				textPrice.setPadding(0, 0, 0, 5);
				textPrice.setTextColor(Color.parseColor("#8F8F8F"));
				textPrice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
				rowDetail.addView(textPrice);

				tableRowList.add(rowDetail);
			}
		}
	}

	/**
	 * 테이블 레이아웃에 테이블 로우 추가
	 */
	private void setTable() {
		if (CollectionUtils.isEmpty(itemList)) {
			contentLayoutTitle.setVisibility(View.GONE);
			Toast.makeText(getApplicationContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
		} else {

			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
			String date = sf.format(new Date());

			textContentLayoutTitle.setText(date + "기준 농수산물 가격");

			textContentLayoutTitle.setVisibility(View.VISIBLE);
			contentLayoutTitle.setVisibility(View.VISIBLE);
			tableLayout.setGravity(Gravity.CENTER);

			setTableRowList();
			if (CollectionUtils.isEmpty(tableRowList)) {
				return;
			}
			for (TableRow tableRow : tableRowList) {
				tableLayout.addView(tableRow);
			}
			contentLayout.addView(tableLayout);
		}
	}

	/**
	 * Map과 List 클리어
	 */
	private void clearList() {
		necessaryList.clear();
		itemList.clear();
		tableRowList.clear();
	}

	/**
	 * table 클리어
	 */
	private void clearLayout() {
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
	private class SetLayoutAsyncTask extends AsyncTask<Void, Void, Void> {

		Dialog loadingDialog = new Dialog(mContext, R.style.LoadingDialog);

		public SetLayoutAsyncTask() {
			loadingDialog.setCancelable(false);
			loadingDialog.addContentView(new ProgressBar(mContext), new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// show dialog
			loadingDialog.show();
			clearLayout();
		}

		@Override
		protected Void doInBackground(Void... arg0) {

			clearList();

			getNecessaryPrice();

			setItemList();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			setNecessaryLayout();

			setTable();

			// dismiss dialog
			loadingDialog.dismiss();
		}
	}
}