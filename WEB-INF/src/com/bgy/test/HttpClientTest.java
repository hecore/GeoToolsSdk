package com.bgy.test;

import java.util.HashMap;

public class HttpClientTest {

	public static void main(String[] args) {
		String url="http://docs.geotools.org/latest/userguide/tutorial/quickstart/index.html";
		try {
			String str= HttpClientService.getInstance().doGet(url,new HashMap<>(),"utf-8");
			System.out.println(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
