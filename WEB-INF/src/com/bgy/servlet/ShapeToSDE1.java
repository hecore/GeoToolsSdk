package com.bgy.servlet;

import java.awt.RenderingHints.Key;
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
import org.geotools.data.DataAccess;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.joining.JoiningNestedAttributeMapping;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.GeometryFactoryFinder;
import org.geotools.geometry.iso.operation.overlay.OverlayOp;
import org.geotools.geometry.jts.FactoryFinder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.vector.UnionFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.alibaba.fastjson.parser.Feature;
import com.bgy.arcgis.sde.SDEGeoTool;
import com.bgy.arcgis.sde.ShapeUtils;
import com.bgy.arcgis.sde.ShapeUtils_1;
import com.bgy.util.DBUtils;
import com.bgy.util.PubFunction;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;



public class ShapeToSDE1 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CONTENT_TYPE = "text/html; charset=utf-8";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result = "";
		String shapepath = request.getParameter("shapepath"); // 对应shape 文件夹
		String layername = request.getParameter("layername");
		// String typeName =
		if (StringUtils.isNotBlank(shapepath)) {
			result = // addShapeTosde(shapepath);
						parseShape(shapepath,layername);
			//parseMdb(shapepath, layername);
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
			String shapefile = shapepath  + shape;
			ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); // 获取连接名
			shapeu.createPrjFile();// 必须设置ID 唯一编号，为了启动进度显示状态

			int iext = shape.lastIndexOf(".");
			String table = shape.substring(0, iext);
			String dbtable = layername.toUpperCase();
			// = table.toUpperCase();
			db.setSdeTableName(dbtable); // 设置表，判断如果存在就删除，重新生成表，并插入数据
			db.deleteTable();

			String v = db.exeImport(shapefile, dbtable);
			sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}

	private String addShapeTosde(String shapePath) {
		//OverlayOp.UNION;

		System.out.println("执行本策略");
		String[] shplist = PubFunction.getDirAllFile(shapePath, ".shp");
		StringBuilder sb = new StringBuilder();
		for (String shape : shplist) {
			String shapefile = shapePath + "/" + shape;
			int iext = shape.lastIndexOf(".");
			String table = shape.substring(0, iext);
			String dbtable = table.toUpperCase();
			Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
			SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
					params.get("password"), params.get("port"));
			try {
				ArcSDEDataStore dataStore = db.getArcSDEDS();
				SimpleFeatureStore store = (SimpleFeatureStore) dataStore.getFeatureSource(table);
				SimpleFeatureType featureType = store.getSchema();
				SimpleFeatureBuilder build = new SimpleFeatureBuilder(featureType);
				GeometryBuilder geom = new GeometryBuilder();
				List<SimpleFeature> list = new ArrayList<>();
				// List<SimpleFeature> list =new
				// ShapeUtils(shapePath).def_shape2List();
				// = new ArrayList<>();
				// 填充对象-->查找新增属性对象
				// list.add( build.buildFeature("fid1", new Object[]{
				// geom.point(1,1), "hello" } ) );
				// list.add( build.buildFeature("fid2", new Object[]{
				// geom.point(2,3), "martin" } ) );
				SimpleFeatureCollection collection = new ListFeatureCollection(featureType, list);
				Transaction transaction = new DefaultTransaction("Add Example");
				store.setTransaction(transaction);
				try {
					store.addFeatures(collection);
					transaction.commit(); // actually writes out the features in
											// one go
				} catch (Exception eek) {
					transaction.rollback();
					
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
//			 db.setSdeTableName(dbtable); //设置表，判断如果存在就删除，重新生成表，并插入数据
//			 String v = db.exeImport(shapefile,dbtable);
//			 sb.append(v);
//			 sb.append("\r\n");
			System.out.println("执行完毕");
		}
		System.out.println("....");
		return sb.toString();
	}

	private static String parseShape(String shapepath, String layername) {
		System.out.println("之后2");
		StringBuilder sb = new StringBuilder();
		Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
				params.get("password"), params.get("port"));
		db.setUp();
		
		try {
			ArcSDEDataStore dataStore = db.getArcSDEDS();
			
			SimpleFeatureStore store = (SimpleFeatureStore) dataStore.getFeatureSource("SDE."+layername);
			SimpleFeatureType featureType = store.getSchema();	
			SimpleFeatureCollection sic = store.getFeatures();
			String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");
			for (String shape : shplist) {
				String shapefile = shapepath + "/" + shape;
				ShapeUtils su= new ShapeUtils(shapefile);
				System.out.println("程序执行前");
				//SimpleFeatureCollection _collection = su.__shapeAppend(sic);	
				System.out.println("程序阻塞后");
				
				
				
				SimpleFeatureBuilder build = new SimpleFeatureBuilder(featureType);
				GeometryBuilder geom = new GeometryBuilder();
				List<SimpleFeature> list = new ArrayList<>();
				SimpleFeatureCollection collection = new ListFeatureCollection(featureType, list);
				
				Transaction transaction = new DefaultTransaction("Add Example");
				store.setTransaction(transaction);
				
				try {
					//store.addFeatures(_collection);
					transaction.commit(); // actually writes out the features in
											// one go
				} catch (Exception e) {
					transaction.rollback();
					throw new RuntimeException(e);
				}
			}
//			System.out.println("cc");	
//			Geometry defGeo =(Geometry) GD.getDefaultValue();
//			GeometryDescriptor gd= featureType.getGeometryDescriptor();
//			DataAccess<SimpleFeatureType, SimpleFeature> ds = store.getDataStore();
//			//featureType.get
//			SimpleFeatureSource sfs=store;
//			SimpleFeature addFeature = 
//			  
//			 (Geometry) geometryDescriptor;
//			ShapeUtils su= new ShapeUtils(shapepath);
//			System.out.println("执行");
//			System.out.println(defGeo+"");
//			su._shapeAppend(defGeo);
//			SimpleFeatureSource featureSource = store.getFeatureSource();
//			store.get
//			ArcSDEGeometryBuilder ab=featureType.get
//			GeometryFactoryFinder.
//			
//			List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
		
			List<SimpleFeature> list = new ArrayList<>();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		
		
		String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");
		for (String shape : shplist) {
			String shapefile = shapepath + shape;
			ShapeUtils shapeu = new ShapeUtils(shapefile); // 获取连接名
			// int iext = shape.lastIndexOf(".");
			// String table = shape.substring(0, iext);
			// String dbtable = table.toUpperCase();
			String dbtable = layername.toUpperCase();
			db.setSdeTableName(dbtable); // 设置表，判断如果存在就删除，重新生成表，并插入数据
			// if (db.) {
			//
			// }
			//new JoiningNestedAttributeMapping(;
			//List<Map<String, String>> mpf = shapeu._getShapeFields();
			//String v = db._saveShapeToSDE(shapeu, mpf) + "";
			//sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();//关闭连接
		System.out.println("连接关闭");
		return sb.toString();
	}
	
	private static String parseShape2(String shapepath, String layername) {
		System.out.println("之后");
		StringBuilder sb = new StringBuilder();
		Map<String, String> params = DBUtils.getConParams(); // 服务器配置参数
		SDEGeoTool db = new SDEGeoTool(params.get("server"), params.get("instance"), params.get("user"),
				params.get("password"), params.get("port"));
		db.setUp();
		//获取目标目录的字段-->come form db
		
		String[] shplist = PubFunction.getDirAllFile(shapepath, ".shp");
		for (String shape : shplist) {
			String shapefile = shapepath + shape;
			ShapeUtils shapeu = new ShapeUtils(shapefile); // 获取连接名
			// int iext = shape.lastIndexOf(".");
			// String table = shape.substring(0, iext);
			// String dbtable = table.toUpperCase();
			String dbtable = layername.toUpperCase();
			db.setSdeTableName(dbtable); // 设置表，判断如果存在就删除，重新生成表，并插入数据
			
			
//			ArcSDEDataStore arc= db.getArcSDEDS();
//			arc.getF
			// if (db.) {
			//
			// }
			//new JoiningNestedAttributeMapping(;
			//List<Map<String, String>> mpf = shapeu._getShapeFields();
			//String v = db._saveShapeToSDE(shapeu, mpf) + "";
			//sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}
	
	public static void main(String[] args) {
		//test2();
		test1();
//		String result = "";
//		String shapepath = "D://FIndexDB//webProject//shape//";
//				//request.getParameter("shapepath"); // 对应shape 文件夹
//		String layername =	
//				//"GH_GHJQ_PY"; 
//				//"LNK_POINT";
//		//String layername = 
//				"万柏林土地用途区";
//		//request.getParameter("layername");
//		// String typeName =
//		if (StringUtils.isNotBlank(shapepath)) {
//			// addShapeTosde(shapepath);
//			//result=parseAdd(shapepath,layername);
//			result = parseShape(shapepath,layername);
//			//parseMdb(shapepath, layername);
//		} else {
//			result = "Layer ID is not null.";
//		}
	}
	
	public static void test1(){
		String result = "";
		String shapepath = "D://tmp//shape//";
				//"D://FIndexDB//webProject//shape//";
				//request.getParameter("shapepath"); // 对应shape 文件夹
		String layername =	
				//"GH_GHJQ_PY"; 
				//"LNK_POINT";
		//String layername = 
				"万柏林土地用途区2";
		//request.getParameter("layername");
		// String typeName =
		if (StringUtils.isNotBlank(shapepath)) {
			// addShapeTosde(shapepath);
			//result=parseAdd(shapepath,layername);
			//result = parseShape(shapepath,layername);
			System.out.println("chuli start ");
			parseMdb(shapepath, layername);
			System.out.println("chuli wanbi ");
		} else {
			result = "Layer ID is not null.";
		}
	}
	
	public static void test2(){
		String result = "";
		String shapepath = "D://FIndexDB//webProject//shape//";
				//request.getParameter("shapepath"); // 对应shape 文件夹
		String layername =	
				//"GH_GHJQ_PY"; 
				//"LNK_POINT";
		//String layername = 
				"万柏林土地用途区";
		//request.getParameter("layername");
		// String typeName =
		if (StringUtils.isNotBlank(shapepath)) {
			// addShapeTosde(shapepath);
			//result=parseAdd(shapepath,layername);
			//result = parseShape(shapepath,layername);
			parseShape2(shapepath, layername);
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
					
//			ArcSDEDataStore arc= db.getArcSDEDS();
//			arc.getF

			List<Map<String, String>> mpf = shapeu._getShapeFields();
			String v = db._saveShape2ToSDE(shapeu, mpf) + "";
					//_saveShapeToSDE(shapeu, mpf) + "";
			sb.append(v);
			sb.append("\r\n");
		}
		db.tearDown();
		return sb.toString();
	}
	//添加数据



}
