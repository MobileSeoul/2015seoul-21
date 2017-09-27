package com.sns.core;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class API {

	private final static String API_HOST = "http://openAPI.seoul.go.kr:8088";
	private final static String API_KEY = "5a58584c7062627536366f57786469";

	/**
	 * 서울시 생필품 가격 정보 반환
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @param item
	 *            품목명
	 * @param region
	 *            지역구명
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getNecessariesPrice(int startIndex, int endIndex, String item, String region) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();

		String requestUrl = getNecessariesPriceURL(startIndex, endIndex, item, region);

		// URL 생성 실패시 null 리턴
		if (StringUtils.isEmpty(requestUrl)) {
			return null;
		}

		// xml 파싱
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);

			InputStream in = new URL(requestUrl).openStream();

			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(in, "utf-8");

			List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
			int eventType = xpp.getEventType();
			String tagName = "";

			while (eventType != XmlPullParser.END_DOCUMENT) {

				if (eventType == XmlPullParser.START_TAG) {
					tagName = xpp.getName().toUpperCase();

					// 생필품 정보 리스트에 추가
					if ("ROW".equals(tagName)) {
						Map<String, Object> row = new HashMap<String, Object>();

						eventType = xpp.next();

						while (eventType != XmlPullParser.END_DOCUMENT) {

							if (eventType == XmlPullParser.START_TAG) {
								tagName = xpp.getName().toUpperCase();
							} else if (eventType == XmlPullParser.TEXT) {

								if ("\n".equals(xpp.getText()) || "\r\n".equals(xpp.getText())) {
									eventType = xpp.next();
									continue;
								}

								if ("M_NAME".equals(tagName)) {
									row.put("M_NAME", xpp.getText());
								} else if ("A_NAME".equals(tagName)) {
									row.put("A_NAME", xpp.getText());
								} else if ("A_UNIT".equals(tagName)) {
									row.put("A_UNIT", xpp.getText());
								} else if ("A_PRICE".equals(tagName)) {
									row.put("A_PRICE", xpp.getText());
								} else if ("P_YEAR_MONTH".equals(tagName)) {
									row.put("P_YEAR_MONTH", xpp.getText());
								} else if ("ADD_COL".equals(tagName)) {
									row.put("ADD_COL", xpp.getText());
								} else if ("P_DATE".equals(tagName)) {
									row.put("P_DATE", xpp.getText());
								} else if ("M_GU_NAME".equals(tagName)) {
									row.put("M_GU_NAME", xpp.getText());
								}

							} else if (eventType == XmlPullParser.END_TAG) {
								tagName = xpp.getName().toUpperCase();

								if ("ROW".equals(tagName)) {
									itemList.add(row);
									break;
								}
							}
							eventType = xpp.next();
						}
					}
				} else if (eventType == XmlPullParser.TEXT) {

					// 응답코드가 "INFO-000"이 아닐경우 정상적인 데이터가 아님
					if ("CODE".equals(tagName)) {
						if (!"INFO-000".equals(xpp.getText())) {
							return null;
						}
					} else if ("LIST_TOTAL_COUNT".equals(tagName)) {
						// 결과 값의 총 개수
						if ("\n".equals(xpp.getText()) || "\r\n".equals(xpp.getText())) {
							eventType = xpp.next();
							continue;
						}
						result.put("LIST_TOTAL_COUNT", xpp.getText());
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					tagName = "";
				}
				eventType = xpp.next();
			}

			result.put("itemList", itemList);

			return result;

		} catch (Exception e) {
			Log.d("API", "서울시 생필품 가격 정보 요청 실패");
		}

		return null;
	}

	/**
	 * 서울시 농수산물 등급별 가격
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @param item
	 *            품목명
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getGarakGradePrice(int startIndex, int endIndex, String item) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();

		String requestUrl = getGarakGradePriceURL(startIndex, endIndex, item);

		// URL 생성 실패시 null 리턴
		if (StringUtils.isEmpty(requestUrl)) {
			return null;
		}

		// xml 파싱
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);

			InputStream in = new URL(requestUrl).openStream();

			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(in, "utf-8");

			int eventType = xpp.getEventType();
			String tagName = "";

			while (eventType != XmlPullParser.END_DOCUMENT) {

				if (eventType == XmlPullParser.START_TAG) {
					tagName = xpp.getName().toUpperCase();

					// 생필품 정보 리스트에 추가
					if ("ROW".equals(tagName)) {
						Map<String, Object> row = new HashMap<String, Object>();

						eventType = xpp.next();

						while (eventType != XmlPullParser.END_DOCUMENT) {

							if (eventType == XmlPullParser.START_TAG) {
								tagName = xpp.getName().toUpperCase();
							} else if (eventType == XmlPullParser.TEXT) {

								if ("\n".equals(xpp.getText()) || "\r\n".equals(xpp.getText())) {
									eventType = xpp.next();
									continue;
								}

								if ("PUMNAME".equals(tagName)) {
									row.put("PUMNAME", xpp.getText());
								} else if ("GRADENAME".equals(tagName)) {
									row.put("GRADENAME", xpp.getText());
								} else if ("UNITQTY".equals(tagName)) {
									row.put("UNITQTY", xpp.getText());
								} else if ("UNITNAME".equals(tagName)) {
									row.put("UNITNAME", xpp.getText());
								} else if ("MAXPRICE".equals(tagName)) {
									row.put("MAXPRICE", xpp.getText());
								} else if ("MINPRICE".equals(tagName)) {
									row.put("MINPRICE", xpp.getText());
								} else if ("AVGPRICE".equals(tagName)) {
									row.put("AVGPRICE", xpp.getText());
								} else if ("INVEST_DT".equals(tagName)) {
									row.put("INVEST_DT", xpp.getText());
								}

							} else if (eventType == XmlPullParser.END_TAG) {
								tagName = xpp.getName().toUpperCase();

								if ("ROW".equals(tagName)) {
									itemList.add(row);
									break;
								}
							}
							eventType = xpp.next();
						}
					}
				} else if (eventType == XmlPullParser.TEXT) {

					// 응답코드가 "INFO-000"이 아닐경우 정상적인 데이터가 아님
					if ("CODE".equals(tagName)) {
						if (!"INFO-000".equals(xpp.getText())) {
							return null;
						}
					} else if ("LIST_TOTAL_COUNT".equals(tagName)) {
						// 결과 값의 총 개수
						if ("\n".equals(xpp.getText()) || "\r\n".equals(xpp.getText())) {
							eventType = xpp.next();
							continue;
						}
						result.put("LIST_TOTAL_COUNT", xpp.getText());
					}

				} else if (eventType == XmlPullParser.END_TAG) {
					tagName = "";
				}
				eventType = xpp.next();
			}

			result.put("itemList", itemList);

			return result;

		} catch (Exception e) {
			Log.d("API", "서울시 농수산물 등급별 가격 요청 실패");
		}

		return null;
	}

	/**
	 * 서울시 생필품 가격 요청 URL 반환
	 * 
	 * @param service
	 * @param startIndex
	 * @param endIndex
	 * @param item
	 * @param region
	 * @return
	 */
	private String getNecessariesPriceURL(int startIndex, int endIndex, String item, String region) {

		if (startIndex > endIndex) {
			return null;
		}

		if (StringUtils.isEmpty(item)) {
			item = "";
		}
		if (StringUtils.isEmpty(region)) {
			region = "";
		}

		/*
		 * ex)
		 * http://openAPI.seoul.go.kr:8088/sample/xml/ListNecessariesPricesService
		 * /1/5/ /사과/ / /강서구/
		 * http://openapi.seoul.go.kr:8088/sample/xml/ListNecessariesPricesService
		 * /1/5/%20/사과/%20/%20/%20/
		 */

		String service = "ListNecessariesPricesService";
		String requestUrl = null;

		try {
			String encService = URLEncoder.encode(service, "UTF-8");
			String encItem = URLEncoder.encode(item, "UTF-8");
			String encRegion = URLEncoder.encode(region, "UTF-8");
			String encBlank = "%20";

			requestUrl = API_HOST + "/" + API_KEY + "/xml/" + encService + "/" + startIndex + "/" + endIndex + "/" + encBlank + "/";

			if (StringUtils.isNotEmpty(item)) {
				requestUrl += encItem + "/";
			} else {
				requestUrl += encBlank + "/";
			}

			requestUrl += encBlank + "/" + encBlank + "/";

			if (StringUtils.isNotEmpty(region)) {
				requestUrl += encRegion + "/";
			} else {
				requestUrl += encBlank + "/";
			}

		} catch (Exception e) {
			return null;
		}

		return requestUrl;
	}

	/**
	 * 서울시 농수산물 등급별 가격 URL 반환
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @param item
	 *            품목명
	 * @return
	 */
	private String getGarakGradePriceURL(int startIndex, int endIndex, String item) {

		if (startIndex > endIndex) {
			return null;
		}

		if (StringUtils.isEmpty(item)) {
			item = "";
		}

		/*
		 * ex) http://openapi.seoul.go.kr:8088/sample/xml/GarakGradePrice/1/5/딸기
		 */

		String service = "GarakGradePrice";
		String requestUrl = null;

		try {
			String encService = URLEncoder.encode(service, "UTF-8");
			String encItem = URLEncoder.encode(item, "UTF-8");
			String encBlank = "%20";

			requestUrl = API_HOST + "/" + API_KEY + "/xml/" + encService + "/" + startIndex + "/" + endIndex + "/";

			if (StringUtils.isNotEmpty(item)) {
				requestUrl += encItem + "/";
			} else {
				requestUrl += encBlank + "/";
			}

		} catch (Exception e) {
			return null;
		}

		return requestUrl;
	}
}
