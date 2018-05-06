package com.bgy.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.bgy.util.PubFunction;


/**
 * 提取上传的   shapefile 成geojson 文件格式字符串
 * @author Administrator
 *
 */
public class MulShpToJson extends HttpServlet{	
	private static final long serialVersionUID = 1L;	
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";
	private final String jsonPath = "D:/FIndexDB/webProject/tyrpc/json/";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		boolean result = false;
		String layerid = request.getParameter("id"); //对应  db_layer 表 ID 字段
		if (StringUtils.isNotBlank(layerid)){
			tranShapeToJson(layerid);
			File file = new File(jsonPath + layerid + ".geojson");
			result = file.exists();
		}
		response.setContentType(CONTENT_TYPE);
		PrintWriter out = response.getWriter();
		out.print(result);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {
		doGet(request,response);
    }
	
	/**
	 * 启动图层分析
	 */
	private void tranShapeToJson(String layerid){
		String shppath = DBUtils.getShpPath(layerid);
		String shapefile = DBUtils.getShapeFile(layerid);		
		String[] shps = PubFunction.getDirAllFile(shppath,".shp");		
		ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); //获取连接名	
		shapeu.mulshapeToJson(jsonPath + layerid + ".geojson",shps);
		try {
			String data = FileUtils.readFileToString(new File(jsonPath + layerid + ".geojson"),"GBK");
			FileUtils.writeStringToFile(new File(jsonPath + layerid + ".geojson"), data, "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
