package com.sns;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

	private Handler mHandler = null; // 스크롤의 맨 끝에 도달할 때 처리할 이벤트 전달용 핸들
	private Rect mRect; // 스크롤뷰 영역을 체크할 변수

	public MyScrollView(Context context, Handler handler) {
		super(context);
		mHandler = handler;
	}

	/**
	 * 그리기가 끝나면 체크
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		checkIsLocatedAtFooter();
	}

	/**
	 * 스크롤의 맨 아래에 도달했는지 체크
	 */
	private void checkIsLocatedAtFooter() {
		if (mRect == null) {
			mRect = new Rect();
			getLocalVisibleRect(mRect); // 스크롤 영역 get
			return;
		}
		int oldBottom = mRect.bottom; // 이전 bottom

		getLocalVisibleRect(mRect);

		int heightView = getMeasuredHeight(); // 스크롤 뷰의 높이
		View v = getChildAt(0); // 스크롤 뷰 안의 내용물의 높이
		int heightInView = v.getMeasuredHeight();

		if (oldBottom > 0 && heightView > 0) {
			if (oldBottom != mRect.bottom && mRect.bottom == (heightInView + getPaddingTop() + getPaddingBottom())) {
				// 맨 끝에 도달했을 때의 처리

				if (mHandler != null) {
					mHandler.sendEmptyMessage(1);
				}
			}
		}
	}

	public boolean isScrollable() {
		if (mRect == null) {
			mRect = new Rect();
			getLocalVisibleRect(mRect); // 스크롤 영역 get
		}

		int heightView = getMeasuredHeight(); // 스크롤 뷰의 높이

		return mRect.bottom == heightView ? false : true;
	}
}