package com.bgy.servlet;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.bgy.util.WorkThread;

public class StartupConfig extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	
	public void init() throws ServletException{
		Thread t = new Thread(new WorkThread());  
        t.start(); 
	}		
}
