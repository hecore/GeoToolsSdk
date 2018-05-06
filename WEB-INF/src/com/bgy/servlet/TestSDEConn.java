package com.bgy.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bgy.arcgis.sde.SDEGeoTool;

/**
 * hegx 2016-01-28
 * 控制，是否启动解析工作，前端页面调用该功能，主要是为了有一个显示控制的地方
 * @author Administrator
 *
 */
public class TestSDEConn extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		String server = request.getParameter("server");
		String instance = request.getParameter("instance");
		String user = request.getParameter("user");
		String password = request.getParameter("password");
		String port = request.getParameter("port");
		boolean result = testConn(server, instance, user, password, port);
		response.setContentType(CONTENT_TYPE); 
		PrintWriter out = response.getWriter();
		out.print(result);		
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
		
	private boolean testConn(String server, String instance, String user, String password, String port){					
		SDEGeoTool db = new SDEGeoTool(server,instance,user,password,port);
		db.setUp();				
		boolean result = db.testConn();
		db.tearDown();
		return result;
	}
	
}
