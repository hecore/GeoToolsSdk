package com.bgy.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.bgy.util.PrjUtil;




public class ParseDxf extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		String dxf = request.getParameter("dxf");
		String result = "";
		if (StringUtils.isNotBlank(dxf)){
			result = PrjUtil.ParseDXFFile(dxf);
		}else{
			result = "error";
		}
		response.setContentType(CONTENT_TYPE); 
		PrintWriter out = response.getWriter();
		out.print(result);
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
		
}
