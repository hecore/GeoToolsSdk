package com.bgy.servlet;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.geotools.arcsde.data.ArcSDEDataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import com.bgy.arcgis.sde.SDEGeoTool;
import com.bgy.arcgis.sde.ShapeUtils;
import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.bgy.util.PubFunction;

public class ShapeToSDE extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result = "";
		String shapepath = request.getParameter("shapepath"); // 对应shape 文件夹
		String layername = request.getParameter("layername");
		String type = request.getParameter("type");
		// String typeName =
		if (StringUtils.isNotBlank(shapepath)) {
			if (type.equals("A")) {
				result = parseShape(shapepath, layername);
			} else if (type.equals("T")) {
				result = parseMdb(shapepath, layername);
			}
			;
		} else {
			result = "Layer ID is not null.";
		}
		response.setContentType(CONTENT_TYPE);
		PrintWriter out = response.getWriter();
		out.print(result);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private static String parseMdb(String shapepath, String layername) {
		StringBuilder sb = new StringBuilder();
		Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
				params.get("password"), params.get("port"));
		db.setUp();
		db.setCharset(Charset.forName("GBK"));
		String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");
		for (String shape : shplist) {
			String shapefile = shapepath + shape;
			ShapeUtils shapeu = new ShapeUtils(shapefile); // 获取连接名
			shapeu.createPrjFile();// 必须设置ID 唯一编号，为了启动进度显示状态
			int iext = shape.lastIndexOf(".");
			String dbtable = layername;
			db.setSdeTableName(dbtable); // 设置表，判断如果存在就删除，重新生成表，并插入数据
			db.deleteTable();

			String v = db.exeImport(shapefile, dbtable);
			sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}

	private static String parseShape(String shapepath, String layername) {

		Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
				params.get("password"), params.get("port"));
		db.setUp();
		SimpleFeatureStore store = null;
		SimpleFeatureCollection twoList = null;
		SimpleFeatureType twoshapeu = null;
		try {
			ArcSDEDataStore dataStore = db.getArcSDEDS();

			store = (SimpleFeatureStore) dataStore.getFeatureSource("SDE." + layername);

			twoshapeu = store.getSchema();
			twoList = store.getFeatures();

		} catch (Exception e) {
			e.printStackTrace();
			// throw new RuntimeException(e);
		}

		// 1.Shp simple
		// String oneShp = "D:/FIndexDB/mdb/2018/04/25/212/shape/尖草坪土地用途区.shp";
		// "F:/tmp/北京POI数据/北京市界.shp"; //原shape
		String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");
		File f1 = new File("D:/tmp/shape");
		if (!f1.exists()) {
			f1.mkdirs();
		}
		for (String shape : shplist) {
			String oneShp = shapepath + shape;
			// "F:/tmp/北京POI数据/区县界_QXJ_PG.shp"; //目标shape
			String dstShp = "D:/tmp//shape/three.shp";
			File f = new File(dstShp);
			if (!f.exists()) {
				try {
					f.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ShapeUtils oneshapeu = new ShapeUtils(oneShp); // 获取连接名
			ShapeUtils twopeu = new ShapeUtils(oneShp); // 获取连接名
			SimpleFeatureCollection oneList = oneshapeu.getShapeFeatures();
			// SimpleFeatureCollection twoList = twopeu.getShapeFeatures();
			// 3.Message get
			SimpleFeatureType schema = twoList.getSchema();

			List<AttributeType> types = schema.getTypes();

			// 4.combine map
			List<Object> shapeLi = oneshapeu.getShapeLi(twoshapeu);

			// 3.shape append

			// 5.union
			// SimpleFeatureType st = oneshapeu.tyFeatureType("three",
			// fldList);//TDYTQLXMC:TDYTQLXMC,TDYTQLXMC:TDYTQLXMC,Shape:Shape

			oneshapeu.unionShapeWithSde(oneList, twoList, dstShp, "three", twoList.getSchema(), shapeLi);
			// store.getSchema());
			System.out.println("合并完毕");
			db.tearDown();

		}
		System.out.println("连接关闭");
		parseMdb("D:/tmp//shape//", layername);
		return "";
	}

	public static void test1() {
		String result = "";
		String shapepath = "D://tmp//shape//";
		// "D://FIndexDB//webProject//shape//";
		// request.getParameter("shapepath"); // 对应shape 文件夹
		String layername =
		// "GH_GHJQ_PY";
		// "LNK_POINT";
		// String layername =
		"万柏林土地用途区2";
		// request.getParameter("layername");
		// String typeName =
		if (StringUtils.isNotBlank(shapepath)) {
			// addShapeTosde(shapepath);
			// result=parseAdd(shapepath,layername);
			// result = parseShape(shapepath,layername);
			System.out.println("chuli start ");
			parseMdb(shapepath, layername);
			System.out.println("chuli wanbi ");
		} else {
			result = "Layer ID is not null.";
		}
	}

	private static String parseAdd(String shapepath, String layername) {
		StringBuilder sb = new StringBuilder();
		Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
				params.get("password"), params.get("port"));
		db.setUp();
		String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");

		for (String shape : shplist) {
			String shapefile = shapepath + shape;
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); // 获取连接名
			// int iext = shape.lastIndexOf(".");
			// String table = shape.substring(0, iext);
			// String dbtable = table.toUpperCase();
			String dbtable = layername.toUpperCase();
			db.setSdeTableName(dbtable); // 设置表，判断如果存在就删除，重新生成表，并插入数据

			// ArcSDEDataStore arc= db.getArcSDEDS();
			// arc.getF

			List<Map<String, String>> mpf = shapeu._getShapeFields();
			String v = db._saveShape2ToSDE(shapeu, mpf) + "";
			// _saveShapeToSDE(shapeu, mpf) + "";
			sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}
	// 添加数据

}
