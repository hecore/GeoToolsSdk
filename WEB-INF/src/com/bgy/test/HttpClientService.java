package com.bgy.test;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpClientService {
	
	private static CloseableHttpClient httpClient=HttpClients.createDefault();
	private static CloseableHttpResponse response = null;
	private static HttpGet httpget=null;
	private static HttpClientService instance=null;
	private static final int TIMEOUT_SECONDS = 20;
	private static final int POOL_SIZE = 20;
	private static final long serialVersionUID = -8483811141908827663L;
	private HttpClientService(){
		
	}
	public static HttpClientService getInstance(){
		if (instance==null) {
			instance=new HttpClientService();
		}
		return instance;
	}

	 public String doGet(String url,Map<String, String> params,String encode) throws Exception{
		 String str=null;
		 if (null !=params) {
			URIBuilder builder = new URIBuilder(url);
			for(Map.Entry<String, String>entry:params.entrySet()){
			   builder.setParameter(entry.getKey(), entry.getValue());
			}
			url=builder.build().toString();
		 }
    	 httpget = new HttpGet(url);
    	 try {
    		if(null==httpClient){
        		httpClient=HttpClientBuilder.create().setMaxConnPerRoute(20).build() ;
    		}
			response=httpClient.execute(httpget);
		    str = EntityUtils.toString(response.getEntity(),encode);			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (response!=null) {
				response.close();			
			}
		}      
    	 return str;
    }
}


