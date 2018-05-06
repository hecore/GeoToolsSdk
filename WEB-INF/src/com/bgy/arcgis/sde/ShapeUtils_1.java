package com.bgy.arcgis.sde;

import java.awt.print.PrinterException;
import java.io.File;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.vector.UnionFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.print.PrintException;

import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.db.util.DBOper;
import com.esri.sde.sdk.client.SeColumnDefinition;
import com.esri.sde.sdk.client.SeLayer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.overlay.OverlayOp;

public class ShapeUtils_1 {
	private String shapeFile;
	private BoundingBox boundingBox = null;
	private CoordinateReferenceSystem coordRefCRS = null;
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

	public ShapeUtils_1(String shapefile) {
		this.shapeFile = shapefile;
	}

	public CoordinateReferenceSystem getCoordRefCRS() {
		return this.coordRefCRS;
	}

	public boolean isExistCrs() {
		String prjfile = shapeFile.substring(0, shapeFile.length() - 4) + ".prj";
		File file = new File(prjfile);
		try {
			if (file.exists()) {
				String wkt_str = new String(Files.readAllBytes(Paths.get(prjfile)));
				if (StringUtils.isNotBlank(wkt_str)) {
					return true;
				} else {
					return false;
				}
			} else {
				ShapefileDataStore store = new ShapefileDataStore(new File(shapeFile).toURI().toURL());
				CoordinateReferenceSystem srcCRS = store.getSchema().getCoordinateReferenceSystem();
				store.dispose();
				if (srcCRS == null) {
					return false;
				} else {
					return true;
				}
			}
		} catch (Exception E) {
			E.printStackTrace();
		}
		return false;
	}

	private CoordinateReferenceSystem getCRS() {
		// String prjfile = shapeFile.substring(0, shapeFile.length() - 4) +
		// ".prj";
		// File file = new File(prjfile);
		CoordinateReferenceSystem srcCRS = null;
		String wkt = "PROJCS[\"Beijing_1954_3_Degree_GK_CM_111E\","
				+ "  GEOGCS[\"GCS_Beijing_1954\",DATUM[\"D_Beijing_1954\",SPHEROID[\"Krasovsky_1940\",6378245.0,298.3]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199]],"
				+ "  PROJECTION[\"Gauss_Kruger\"],PARAMETER[\"False_Easting\",13900.0],PARAMETER[\"False_Northing\",-4129200.0],PARAMETER[\"Central_Meridian\",112.5],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0],AUTHORITY[\"EPSG\",2434]]";

		// wkt =
		// "PROJCS[\"Beijing_1954_3_Degree_GK_CM_111E\",GEOGCS[\"GCS_Beijing_1954\",DATUM[\"D_Beijing_1954\",SPHEROID[\"Krasovsky_1940\",6378245.0,298.3]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Gauss_Kruger\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",111.0],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0],AUTHORITY[\"EPSG\",2434]]";
		try {
			/*
			 * if (file.exists()){ String wkt_str = new
			 * String(Files.readAllBytes(Paths.get(prjfile))); if
			 * (StringUtils.isNotBlank(wkt_str)){ srcCRS =
			 * CRS.parseWKT(wkt_str); }else{ srcCRS = CRS.parseWKT(wkt); }
			 * }else{
			 */
			// ShapefileDataStore store = new ShapefileDataStore(new
			// File(shapeFile).toURI().toURL());
			// srcCRS = store.getSchema().getCoordinateReferenceSystem();
			// store.dispose();
			// if (srcCRS == null){
			srcCRS = CRS.parseWKT(wkt);
			// }
			// }
			return srcCRS;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DefaultGeographicCRS.WGS84;
	}

	public void createPrjFile() {
		String prjfile = shapeFile.substring(0, shapeFile.length() - 4) + ".prj";
		File file = new File(prjfile);
		String wkt = "PROJCS[\"Beijing_1954_3_Degree_GK_CM_111E\","
				+ "  GEOGCS[\"GCS_Beijing_1954\",DATUM[\"D_Beijing_1954\",SPHEROID[\"Krasovsky_1940\",6378245.0,298.3]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199]],"
				+ "  PROJECTION[\"Gauss_Kruger\"],PARAMETER[\"False_Easting\",13900.0],PARAMETER[\"False_Northing\",-4129200.0],PARAMETER[\"Central_Meridian\",112.5],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0],AUTHORITY[\"EPSG\",2434]]";
		if (!file.exists()) {
			try {
				ShapefileDataStore store = new ShapefileDataStore(new File(shapeFile).toURI().toURL());
				CoordinateReferenceSystem srcCRS = CRS.parseWKT(wkt);
				store.forceSchemaCRS(srcCRS);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * 图层类型
	 * 
	 * @return
	 */
	public int getLayerType() {
		int selayerType = 0;
		Class<? extends Geometry> geomType = getGeometryClass();
		if (geomType == Point.class) {
			selayerType = SeLayer.SE_POINT_TYPE_MASK;
		} else if (geomType == MultiPoint.class) {
			selayerType = SeLayer.SE_POINT_TYPE_MASK;
		} else if (geomType == LineString.class) {
			selayerType = SeLayer.SE_LINE_TYPE_MASK;
		} else if (geomType == MultiLineString.class) {
			selayerType = SeLayer.SE_LINE_TYPE_MASK;
		} else if (geomType == Polygon.class) {
			selayerType = SeLayer.SE_AREA_TYPE_MASK;
		} else if (geomType == MultiPolygon.class) {
			selayerType = SeLayer.SE_MULTIPART_TYPE_MASK;
		} else {
			throw new UnsupportedOperationException("finish implementing this!");
		}
		return selayerType;
	}

	/**
	 * 获取图层的几何类型
	 * 
	 * @return
	 */
	public String getLayerGeomType() {
		String selayerType = "";
		Class<? extends Geometry> geomType = getGeometryClass();
		if (geomType == Point.class) {
			selayerType = "POINT";
		} else if (geomType == MultiPoint.class) {
			selayerType = "POINT";
		} else if (geomType == LineString.class) {
			selayerType = "LINE";
		} else if (geomType == MultiLineString.class) {
			selayerType = "LINE";
		} else if (geomType == Polygon.class) {
			selayerType = "POLYGON";
		} else if (geomType == MultiPolygon.class) {
			selayerType = "POLYGON";
		} else {
			throw new UnsupportedOperationException("finish implementing this!");
		}
		return selayerType;
	}

	/**
	 * 初始化坐标系，以及边界信息
	 */
	public void initCoordSys() {
		try {
			CoordinateReferenceSystem crs = getCRS();
			// ShapefileDataStoreFactory dataStoreFactory = new
			// ShapefileDataStoreFactory();
			// ShapefileDataStore store =
			// (ShapefileDataStore)dataStoreFactory.createDataStore(new
			// File(shapeFile).toURI().toURL());
			// SimpleFeatureSource featureSource = store.getFeatureSource();
			this.coordRefCRS = crs;
			// store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 新建一个要素类别
	 * 
	 * @param typename
	 * @param mpf：标准字段和源字段的映射关系
	 * @return
	 */
	public SimpleFeatureType newFeatureType(String typename, Map<String, String> mpf) {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		ShpFiles shapeDBF = null;
		DbaseFileReader dbfReader = null;
		try {
			shapeDBF = new ShpFiles(dbfile);
			dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		DbaseFileHeader header = dbfReader.getHeader();
		int numFields = header.getNumFields();
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(typename);
		builder.setCRS(getCRS()); // <- Coordinate reference system
		builder.add("OBJECTID", Integer.class);
		for (int i = 0; i < numFields; i++) {
			String srcfield = header.getFieldName(i).toUpperCase(); // 不用大写
			String fieldName = mpf.get(srcfield);
			if (StringUtils.isBlank(fieldName)) {
				continue;
			}
			String lowerc = fieldName.toLowerCase();
			if (lowerc.indexOf("shape") == 0 || "objectid".equals(lowerc) || lowerc.indexOf("fid") == 0
					|| "id".equals(lowerc)) {
				continue;
			}
			char fieldType = header.getFieldType(i);
			switch (fieldType) {
			case 'C':
				builder.length(512).add(fieldName, String.class);
				break;
			case 'L':
				builder.add(fieldName, Integer.class);
				break;
			case 'D':
				builder.add(fieldName, Date.class);
				break;
			case 'I':
				builder.add(fieldName, Integer.class);
				break;
			case 'F':
				builder.add(fieldName, Double.class);
				break;
			case 'N':
				builder.add(fieldName, Integer.class);
				break;
			default:
				System.out.println("无效类型:" + fieldType);
			}
		}
		builder.add("SHAPE", getGeometryClass());
		final SimpleFeatureType LOCATION = builder.buildFeatureType();
		try {
			dbfReader.close();
			shapeDBF.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return LOCATION;
	}

	public SimpleFeatureType createFeatureType(String typename, List<String> fieldls) {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		ShpFiles shapeDBF = null;
		DbaseFileReader dbfReader = null;
		try {
			shapeDBF = new ShpFiles(dbfile);
			dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("GBK"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		DbaseFileHeader header = dbfReader.getHeader();
		int numFields = header.getNumFields();
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(typename);
		builder.setCRS(getCRS()); // <- Coordinate reference system
		builder.nillable(true);
		for (int i = 0; i < numFields; i++) {
			String fieldName = header.getFieldName(i).toUpperCase();
			if (fieldName.toLowerCase().indexOf("shape") == 0 || "OBJECTID".equals(fieldName.toUpperCase())
					|| fieldName.toLowerCase().indexOf("fid") == 0 || "ID".equals(fieldName.toUpperCase())) {
				continue;
			}
			fieldls.add(fieldName);
			char fieldType = header.getFieldType(i);
			System.out.println("字段类型:" + fieldType + "," + fieldName);
			switch (fieldType) {
			case 'C':
				builder.add(fieldName, String.class);
				break;
			case 'L':
				builder.add(fieldName, Integer.class);
				break;
			case 'D':
				builder.add(fieldName, Date.class);
				break;
			case 'I':
				builder.add(fieldName, Integer.class);
				break;
			case 'F':
				builder.add(fieldName, Double.class);
				break;
			case 'N':
				builder.add(fieldName, Integer.class);
				break;
			default:
				System.out.println("无效类型:" + fieldType);
			}
		}
		fieldls.add("SHAPE");
		builder.add("SHAPE", getGeometryClass());
		final SimpleFeatureType LOCATION = builder.buildFeatureType();
		try {
			dbfReader.close();
			shapeDBF.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return LOCATION;
	}

	private Class<? extends Geometry> getGeometryClass() {
		Class<? extends Geometry> geoStr = null;
		try {
			ShapefileReader r = new ShapefileReader(new ShpFiles(shapeFile), false, false, new GeometryFactory());
			if (r.hasNext()) {
				Geometry shape = (Geometry) r.nextRecord().shape(); // com.vividsolutions.jts.geom.Geometry;
				geoStr = shape.getClass();
			}
			r.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return geoStr;
	}

	/**
	 * 获取所有列信息
	 * 
	 * @return
	 */
	public Map<String, Map<String, Integer>> getFields() {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		try {
			ShpFiles shapeDBF = new ShpFiles(dbfile);
			DbaseFileReader dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("GBK"));
			DbaseFileHeader header = dbfReader.getHeader();
			int numFields = header.getNumFields();
			Map<String, Map<String, Integer>> mmp = new LinkedHashMap<String, Map<String, Integer>>();
			for (int i = 0; i < numFields; i++) {
				String fieldName = header.getFieldName(i);
				if (fieldName.toLowerCase().indexOf("shape_") > -1 || "OBJECTID".equals(fieldName.toUpperCase())) {
					continue;
				}
				int ftype = SeColumnDefinition.TYPE_NSTRING;
				char fieldType = header.getFieldType(i);
				int fldSize = header.getFieldLength(i);
				int fldPer = header.getFieldDecimalCount(i);
				switch (fieldType) {
				case 'C':
					fldPer = 0;
					break;
				case 'L':
					ftype = SeColumnDefinition.TYPE_INT32;
					fldPer = 0;
					break;
				case 'D':
					ftype = SeColumnDefinition.TYPE_DATE;
					fldSize = 1;
					fldPer = 0;
					break;
				case 'I':
					ftype = SeColumnDefinition.TYPE_INT32;
					fldPer = 0;
					break;
				case 'F':
					ftype = SeColumnDefinition.TYPE_FLOAT64;
					break;
				case 'N':
					ftype = SeColumnDefinition.TYPE_INT32;
					fldPer = 0;
					break;
				default:
					System.out.println("无效类型:" + fieldType);
				}
				Map<String, Integer> hm = new LinkedHashMap<String, Integer>();
				hm.put("ftype", ftype);
				hm.put("fsize", fldSize);
				hm.put("fdeci", fldPer);
				mmp.put(fieldName.toUpperCase(), hm);
			}
			dbfReader.close();
			shapeDBF.dispose();
			return mmp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取字段属性信息
	 * 
	 * @return
	 */
	public List<Map<String, String>> getShapeFields() {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		ShpFiles shapeDBF = null;
		DbaseFileReader dbfReader = null;
		try {
			shapeDBF = new ShpFiles(dbfile);
			dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		List<Map<String, String>> lmp = new ArrayList<Map<String, String>>();
		DbaseFileHeader header = dbfReader.getHeader();
		int numFields = header.getNumFields();
		for (int i = 0; i < numFields; i++) {
			String fieldName = header.getFieldName(i);
			if (fieldName.toLowerCase().indexOf("shape") == 0 || "OBJECTID".equals(fieldName.toUpperCase())
					|| fieldName.toLowerCase().indexOf("fid") == 0 || "ID".equals(fieldName.toUpperCase())) {
				continue;
			}
			String ftype = "";
			char fieldType = header.getFieldType(i);
			String fldSize = String.valueOf(header.getFieldLength(i));
			String fldPer = String.valueOf(header.getFieldDecimalCount(i));
			switch (fieldType) {
			case 'C':
				ftype = "文本";
				break;
			case 'L':
				ftype = "长整型";
				break;
			case 'D':
				ftype = "日期";
				break;
			case 'I':
				ftype = "长整型";
				break;
			case 'F':
				ftype = "双精度";
				break;
			case 'N':
				ftype = "长整型";
				break;
			default:
				System.out.println("无效类型:" + fieldType);
			}
			Map<String, String> hm = new LinkedHashMap<String, String>();
			hm.put("fname", fieldName);
			hm.put("ftype", ftype);
			hm.put("fsize", fldSize);
			hm.put("fdeci", fldPer);
			lmp.add(hm);
		}
		try {
			dbfReader.close();
			shapeDBF.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lmp;
	}

	/**
	 * 获取字段属性信息
	 * 
	 * @return
	 */
	public List<Map<String, String>> _getShapeFields() {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		ShpFiles shapeDBF = null;
		DbaseFileReader dbfReader = null;
		try {
			shapeDBF = new ShpFiles(dbfile);
			dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		List<Map<String, String>> lmp = new ArrayList<Map<String, String>>();
		DbaseFileHeader header = dbfReader.getHeader();
		int numFields = header.getNumFields();
		for (int i = 0; i < numFields; i++) {
			String fieldName = header.getFieldName(i);
			// if (fieldName.toLowerCase().indexOf("shape") == 0 ||
			// "OBJECTID".equals(fieldName.toUpperCase())
			// || fieldName.toLowerCase().indexOf("fid") == 0 ||
			// "ID".equals(fieldName.toUpperCase())) {
			// continue;
			// }
			String ftype = "";
			char fieldType = header.getFieldType(i);
			String fldSize = String.valueOf(header.getFieldLength(i));
			String fldPer = String.valueOf(header.getFieldDecimalCount(i));
			switch (fieldType) {
			case 'C':
				ftype = "文本";
				break;
			case 'L':
				ftype = "长整型";
				break;
			case 'D':
				ftype = "日期";
				break;
			case 'I':
				ftype = "长整型";
				break;
			case 'F':
				ftype = "双精度";
				break;
			case 'N':
				ftype = "长整型";
				break;
			default:
				System.out.println("无效类型:" + fieldType);
			}
			Map<String, String> hm = new LinkedHashMap<String, String>();
			hm.put("fname", fieldName);
			hm.put("ftype", ftype);
			hm.put("fsize", fldSize);
			hm.put("fdeci", fldPer);
			lmp.add(hm);
		}
		try {
			dbfReader.close();
			shapeDBF.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lmp;
	}

	/**
	 * 获取记录个数
	 * 
	 * @return
	 */
	public int getDBFRecordCount() {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		ShpFiles shapeDBF = null;
		DbaseFileReader dbfReader = null;
		int rc = 0;
		try {
			shapeDBF = new ShpFiles(dbfile);
			dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("GBK"));
			DbaseFileHeader header = dbfReader.getHeader();
			rc = header.getNumRecords();
			dbfReader.close();
			shapeDBF.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rc;
	}

	/**
	 * 获取所有列信息
	 * 
	 * @return
	 */
	public SeColumnDefinition[] getColInfo() {
		Map<String, Map<String, Integer>> mmp = getFields();
		int isize = mmp.size();
		int row = 0;
		try {
			SeColumnDefinition[] colDefs = new SeColumnDefinition[isize];
			for (Map.Entry<String, Map<String, Integer>> entry : mmp.entrySet()) {
				String key = entry.getKey();
				Map<String, Integer> hm = entry.getValue();
				colDefs[row] = new SeColumnDefinition(key, hm.get("ftype"), hm.get("fsize"), hm.get("fdeci"), true);
				row += 1;
			}
			return colDefs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Set<FeatureId> getFeatureIds() {
		Set<FeatureId> fids = new LinkedHashSet<FeatureId>();
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				fids.add(feature.getIdentifier());
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fids;
	}

	public BoundingBox getBounding() {
		return boundingBox;
	}

	public List<SimpleFeature> getShapePropertis() {
		List<SimpleFeature> lhp = new ArrayList<SimpleFeature>();
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				lhp.add(feature);
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lhp;
	}

	public void getShapePropertis(FeatureWriter<SimpleFeatureType, SimpleFeature> writer, List<String> fields) {
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			this.boundingBox = featureSource.getBounds();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature addFeature = itertor.next();
				Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
				if (!defGeo.isValid()) {
					defGeo.normalize();
				}
				SimpleFeature newFeature = writer.next();
				newFeature.setAttribute("SHAPE", defGeo);
				for (String key : fields) {
					if ("SHAPE".equals(key) || "OBJECTID".equals(key)) {
						continue;
					}
					Object rv = addFeature.getAttribute(key);
					newFeature.setAttribute(key, rv);
				}
				writer.write();
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getShapePropertis(FeatureWriter<SimpleFeatureType, SimpleFeature> writer, Map<String, String> mpkv) {
		int row = 0;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			this.boundingBox = featureSource.getBounds();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature addFeature = itertor.next();
				Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
				defGeo.normalize();
				SimpleFeature newFeature = writer.next();
				newFeature.setAttribute("SHAPE", defGeo);
				for (Map.Entry<String, String> entry : mpkv.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					String lowerc = key.toLowerCase();
					if (lowerc.indexOf("shape") == 0 || "objectid".equals(lowerc) || lowerc.indexOf("fid") == 0
							|| "id".equals(lowerc)) {
						continue;
					}
					Object rv = addFeature.getAttribute(key);
					newFeature.setAttribute(value, rv);
				}
				try {
					writer.write();
					row += 1;
				} catch (Exception E) {
					System.out.println(E.getMessage());
				}

			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return row;
	}

	public int _getShapePropertis(FeatureWriter<SimpleFeatureType, SimpleFeature> writer,
			List<Map<String, String>> mpkv) {
		int row = 0;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());// shape中读取数据
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			this.boundingBox = featureSource.getBounds();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature addFeature = itertor.next();
				Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
				defGeo.normalize();
				SimpleFeature newFeature = writer.next();
				newFeature.setAttribute("SHAPE", defGeo);// 多边范围.
				for (int i = 0; i < mpkv.size(); i++) {
					Map<String, String> mpv = mpkv.get(i);
					for (Map.Entry<String, String> entry : mpv.entrySet()) {
						String key = entry.getKey();// JQBH
						String value = entry.getValue();// JQBH
						String lowerc = key.toLowerCase();
						// if (lowerc.indexOf("shape") == 0 ||
						// "objectid".equals(lowerc) || lowerc.indexOf("fid") ==
						// 0
						// || "id".equals(lowerc)) {
						// continue;
						// }
						Object rv = addFeature.getAttribute(value);
						if (null != value && (null != value && null != rv) && !"OBJECTID".equals(value)
						// (!"OBJECTID".equals(value)&&null!=rv)
								&& (!"Shape_Area".equals(value) && null != rv)
								&& (!"Shape_Leng".equals(value) && null != rv)
								&& (!("Shape_Le_1").equals(value) && null != rv)
								&& (!"Shape_Le_2".equals(value) && null != rv)
								&& (!("Shape_Ar_1").equals(value) && null != rv)) {
							newFeature.setAttribute(value, rv);
						}
					}
					// newFeature.setAttribute(mpkv.get(i), );
					try {
						writer.write();
						row += 1;
					} catch (Exception E) {
						System.out.println(E.getMessage());
					}
				}
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return row;
	}

	/**
	 * 获取要素构造器
	 * 
	 * @return
	 */
	public SimpleFeatureBuilder getFBuilder(Map<String, String> map) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("featureType");
		builder.setCRS(getCRS());
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String fieldName = entry.getKey();
			String ftype = entry.getValue();
			if (ftype.equals("文本")) {
				builder.add(fieldName, String.class);
			} else if (ftype.equals("长整型")) {
				builder.add(fieldName, Integer.class);
			} else if (ftype.equals("日期")) {
				builder.add(fieldName, Date.class);
			} else if (ftype.equals("双精度")) {
				builder.add(fieldName, Double.class);
			}
		}
		builder.add("SHAPE", getGeometryClass());
		final SimpleFeatureType LOCATION = builder.buildFeatureType();
		SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(LOCATION);
		return fbuilder;
	}

	private String parseRowID(String rowid) {
		int ipos = rowid.lastIndexOf(".");
		String row = rowid.substring(ipos + 1);
		return row;
	}

	private FeatureJSON newFJSON() {
		FeatureJSON fjson = new FeatureJSON(new GeometryJSON(19));
		fjson.setEncodeNullValues(true);
		fjson.setEncodeFeatureCollectionCRS(true);
		fjson.setEncodeFeatureCollectionBounds(true);
		return fjson;
	}

	/**
	 * 缺省的直接处理json 文件，不需要任何压缩
	 * 
	 * @param jsonfile
	 */
	public void _defshapeToJson(String jsonfile) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			FeatureJSON fjson = newFJSON();
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Geometry tarGeo = JTS.transform(srcGeo, tranf);
				feature.setDefaultGeometry(tarGeo);
				fcs.add(feature);
			}
			fjson.writeFeatureCollection(fcs, jsonfile);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void def_shape2List() {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			List<SimpleFeature> list = new ArrayList<>();
			// FeatureJSON fjson = newFJSON();
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Geometry tarGeo = JTS.transform(srcGeo, tranf);
				feature.setDefaultGeometry(tarGeo);
				list.add(feature);
				// fcs.add(feature);
			}
			// fjson.writeFeatureCollection(fcs, jsonfile);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public void defshapeToJson(String jsonfile, Map<String, String> map, SimpleFeatureBuilder sfbuilder) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("utf-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();

			FeatureJSON fjson = newFJSON();
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Geometry tarGeo = JTS.transform(srcGeo, tranf);
				if (map != null) {
					SimpleFeature newfeature = sfbuilder.buildFeature(parseRowID(feature.getID()));
					for (Map.Entry<String, String> entry : map.entrySet()) {
						String key = entry.getValue();
						String value = entry.getKey();
						Object objattr = feature.getAttribute(value);
						newfeature.setAttribute(key, objattr);
					}
					newfeature.setDefaultGeometry(tarGeo);
					fcs.add(newfeature);
				} else {
					SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(feature.getFeatureType()); // 重新构建要素类别
					SimpleFeature newfeature = fbuilder.buildFeature(parseRowID(feature.getID())); // 都重新构建新的要素，ID
					newfeature.setAttributes(feature.getAttributes());
					newfeature.setDefaultGeometry(tarGeo);
					fcs.add(newfeature);
				}
			}
			fjson.writeFeatureCollection(fcs, jsonfile);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 多shapefie文件同时处理成一个json文件
	 * 
	 * @param jsonfile
	 * @param map
	 * @param sfbuilder
	 */
	public void mulshapeToJson(String jsonfile, String[] shps) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);
			FeatureJSON fjson = newFJSON();
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			int rownumber = 0;
			for (String shape : shps) {
				ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
						.createDataStore(new File(shape).toURI().toURL());
				store.setCharset(Charset.forName("utf-8"));
				SimpleFeatureSource featureSource = store.getFeatureSource();
				SimpleFeatureIterator itertor = featureSource.getFeatures().features();
				while (itertor.hasNext()) {
					SimpleFeature feature = itertor.next();
					Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
					Geometry tarGeo = JTS.transform(srcGeo, tranf);
					rownumber++;
					SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(feature.getFeatureType()); // 重新构建要素类别
					SimpleFeature newfeature = fbuilder.buildFeature(String.valueOf(rownumber)); // 都重新构建新的要素，ID
					newfeature.setAttributes(feature.getAttributes());
					newfeature.setDefaultGeometry(tarGeo);
					fcs.add(newfeature);
				}
				itertor.close();
				store.dispose();
			}
			fjson.writeFeatureCollection(fcs, jsonfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 面图形转换
	 * 
	 * @param jsonfile
	 * @param area
	 *            面积大小
	 */
	public void polygonShapeToJson(String jsonfile, Map<String, String> map, SimpleFeatureBuilder sfbuilder) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);

			ShapefileDataStore store = new ShapefileDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureCollection sc = featureSource.getFeatures();
			SimpleFeatureIterator itertor = sc.features();
			int rowcount = sc.size();
			int deCount = 0;
			if (rowcount > 3000 && rowcount < 10000) {
				deCount = 100;
			} else if (rowcount > 10000 && rowcount < 100000) {
				deCount = 300;
			} else if (rowcount > 100000) {
				deCount = 600;
			}

			FeatureJSON fjson = new FeatureJSON(new GeometryJSON(4));
			fjson.setEncodeNullValues(false);
			fjson.setEncodeFeatureCollectionCRS(true);
			fjson.setEncodeFeatureCollectionBounds(true);
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Coordinate[] cords = srcGeo.getCoordinates();
				if (cords.length < deCount) {
					continue;
				}
				// System.out.println("坐标点个数:" + cords.length + "," +
				// srcGeo.getGeometryType());
				List<Polygon> polist = new ArrayList<Polygon>();
				List<Coordinate> plist = new ArrayList<Coordinate>();
				plist.add(cords[0]);
				double _x = cords[0].x;
				double _y = cords[0].y;
				boolean bs = false;
				for (int I = 1; I < cords.length; I++) {
					if (bs) {
						plist.add(cords[I]);
						_x = cords[I].x;
						_y = cords[I].y;
						bs = false;
						continue;
					}
					double x = cords[I].x;
					double y = cords[I].y;
					if (x == _x && y == _y) {
						plist.add(cords[I]);
						if (plist.size() > 3) {
							Coordinate[] coords = (Coordinate[]) plist.toArray(new Coordinate[plist.size()]);
							Polygon polygon = geometryFactory.createPolygon(coords);
							polist.add(polygon);
						}
						plist.clear();
						bs = true;
						continue;
					}
					if (I % 2 == 0) {
						plist.add(cords[I]);
					}
				}

				Polygon[] polygons = polist.toArray(new Polygon[polist.size()]);
				MultiPolygon polygon = geometryFactory.createMultiPolygon(polygons);
				Geometry tarGeo = JTS.transform(polygon, tranf);

				if (map != null) {
					SimpleFeature newfeature = sfbuilder.buildFeature(parseRowID(feature.getID()));
					for (Map.Entry<String, String> entry : map.entrySet()) {
						String key = entry.getValue();
						String value = entry.getKey();
						Object objattr = feature.getAttribute(value);
						newfeature.setAttribute(key, objattr);
					}
					newfeature.setDefaultGeometry(tarGeo);
					fcs.add(newfeature);
				} else {
					feature.setDefaultGeometry(tarGeo);
					fcs.add(feature);
				}
			}
			fjson.writeFeatureCollection(fcs, jsonfile);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 面图形转换
	 * 
	 * @param jsonfile
	 * @param area
	 *            面积大小
	 */
	public void LineShapeToJson(String jsonfile, Map<String, String> map, SimpleFeatureBuilder sfbuilder) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);

			ShapefileDataStore store = new ShapefileDataStore(new File(shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureCollection sc = featureSource.getFeatures();
			SimpleFeatureIterator itertor = sc.features();
			int rowcount = sc.size();
			int deCount = 0;
			if (rowcount > 2000 && rowcount < 10000) {
				deCount = 100;
			} else if (rowcount > 10000) {
				deCount = 500;
			}

			FeatureJSON fjson = new FeatureJSON(new GeometryJSON(4));
			fjson.setEncodeNullValues(false);
			fjson.setEncodeFeatureCollectionCRS(true);
			fjson.setEncodeFeatureCollectionBounds(true);
			DefaultFeatureCollection fcs = new DefaultFeatureCollection();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Coordinate[] cords = srcGeo.getCoordinates();
				if (cords.length < deCount) {
					continue;
				}
				// System.out.println("行:" + row + ",坐标点个数:" + cords.length +
				// "," + tarGeo.getGeometryType());
				List<Coordinate> plist = new ArrayList<Coordinate>();
				plist.add(cords[0]);
				for (int I = 1; I < cords.length - 1; I++) {
					if (I % 2 == 0) {
						plist.add(cords[I]);
					}
				}
				plist.add(cords[cords.length - 1]);
				Coordinate[] coords = (Coordinate[]) plist.toArray(new Coordinate[plist.size()]);
				LineString line = geometryFactory.createLineString(coords);
				Geometry tarGeo = JTS.transform(line, tranf);
				if (map != null) {
					SimpleFeature newfeature = sfbuilder.buildFeature(parseRowID(feature.getID()));
					for (Map.Entry<String, String> entry : map.entrySet()) {
						String key = entry.getValue();
						String value = entry.getKey();
						Object objattr = feature.getAttribute(value);
						newfeature.setAttribute(key, objattr);
					}
					newfeature.setDefaultGeometry(tarGeo);
					fcs.add(newfeature);
				} else {
					feature.setDefaultGeometry(tarGeo);
					fcs.add(feature);
				}
			}
			fjson.writeFeatureCollection(fcs, jsonfile);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 缺省的处理json
	 * 
	 * @param jsonfile
	 */
	public void shapeToJson(String jsonfile, Map<String, String> map, SimpleFeatureBuilder sfbuilde) {
		defshapeToJson(jsonfile, map, sfbuilde);
		/*
		 * int lt = getLayerType(); switch (lt) { case
		 * SeLayer.SE_POINT_TYPE_MASK: defshapeToJson(jsonfile,map,sfbuilde);
		 * break; case SeLayer.SE_LINE_TYPE_MASK:
		 * LineShapeToJson(jsonfile,map,sfbuilde); break; case
		 * SeLayer.SE_AREA_TYPE_MASK: polygonShapeToJson(jsonfile,map,sfbuilde);
		 * break; case SeLayer.SE_MULTIPART_TYPE_MASK:
		 * polygonShapeToJson(jsonfile,map,sfbuilde); break; default:
		 * defshapeToJson(jsonfile,map,sfbuilde); }
		 */
	}

	/**
	 * 单个shape文件，生成多个json，文件，主要是文件太大了，拆分来使用
	 * 
	 * @param shape
	 * @param jpath
	 */
	public void shapeToMulJson(String jpath) {
		try {
			CoordinateReferenceSystem srcCRS = getCRS();
			MathTransform tranf = CRS.findMathTransform(srcCRS, DefaultGeographicCRS.WGS84, true);
			FeatureJSON fjson = newFJSON();
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(this.shapeFile).toURI().toURL());
			store.setCharset(Charset.forName("utf-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			DefaultFeatureCollection fcs = new DefaultFeatureCollection(); // 生成记录缓存区
			int rownumber = 0;
			int filenum = 1;// 文件号，后缀
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				Geometry tarGeo = JTS.transform(srcGeo, tranf);
				rownumber++;
				SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(feature.getFeatureType()); // 重新构建要素类别
				SimpleFeature newfeature = fbuilder.buildFeature(String.valueOf(rownumber)); // 都重新构建新的要素，ID
				newfeature.setAttributes(feature.getAttributes());
				newfeature.setDefaultGeometry(tarGeo);
				// Point p = tarGeo.getCentroid();
				// System.out.println(p.getX() + "," + p.getY() + "," +
				// parseRowID(feature.getID()));
				fcs.add(newfeature);
				if (rownumber >= 2000) {
					rownumber = 0;
					String geojson = jpath + "/" + filenum + ".geojson";
					fjson.writeFeatureCollection(fcs, geojson);
					filenum++;
					fcs.clear();
					break;
				}
			}
			String geojson = jpath + "/" + filenum + ".geojson";
			fjson.writeFeatureCollection(fcs, geojson);
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取原始shape文件，dbf字段列表
	 * 
	 * @return
	 */
	private List<String> getDBFiledList() {
		String dbfile = shapeFile.substring(0, shapeFile.length() - 4) + ".dbf";
		try {
			ShpFiles shapeDBF = new ShpFiles(dbfile);
			DbaseFileReader dbfReader = new DbaseFileReader(shapeDBF, false, Charset.forName("GBK"));
			DbaseFileHeader header = dbfReader.getHeader();
			int numFields = header.getNumFields();
			List<String> list = new ArrayList<String>();
			for (int i = 0; i < numFields; i++) {
				String fieldName = header.getFieldName(i);
				list.add(fieldName);
			}
			dbfReader.close();
			shapeDBF.dispose();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public SimpleFeatureType newFeatureType(String typename, List<Map<String, Object>> list) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(typename);
		builder.setCRS(getCRS()); // <- Coordinate reference system
		for (Map<String, Object> mp : list) {
			String name = (String) mp.get("name");
			char type = (char) mp.get("type");
			int len = Integer.valueOf((String) mp.get("len"));
			int pre = Integer.valueOf((String) mp.get("pre"));
			switch (type) {
			case 'C':
				builder.length(len).add(name, String.class);
				break;
			case 'L':
				builder.length(len).add(name, Integer.class);
				break;
			case 'D':
				builder.add(name, Date.class);
				break;
			case 'I':
				builder.length(len).add(name, Integer.class);
				break;
			case 'F':
				builder.length(len).userData("decimalCount", pre).add(name, Double.class);
				break;
			case 'N':
				builder.length(len).add(name, Integer.class);
				break;
			default:
				System.out.println("无效类型:" + type);
			}
		}
		builder.add("Shape", getGeometryClass());
		final SimpleFeatureType LOCATION = builder.buildFeatureType();

		return LOCATION;
	}

	private ShapefileDataStore getNewShapeDS(String newshape, SimpleFeatureType type) throws Exception {
		File file = new File(newshape);
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
		ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);
		ds.createSchema(type);// 创建表结构
		ds.setCharset(Charset.forName("UTF-8"));
		return ds;
	}

	/**
	 * 获取原始要素集合合
	 * 
	 * @return
	 */
	private SimpleFeatureCollection getShapeFeatures() {
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		try {
			ShapefileDataStore sds = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			sds.setCharset(Charset.forName("GBK"));
			SimpleFeatureSource featureSource = sds.getFeatureSource();
			SimpleFeatureCollection itertor = featureSource.getFeatures();
			sds.dispose();
			return itertor;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 列表是否包含某个词
	 * 
	 * @param list
	 * @param fld
	 * @return
	 */
	private int listContains(List<String> list, String fld) {
		int v = -1;
		for (int i = 0; i < list.size(); i++) {
			String tmp = list.get(i);
			if (tmp.equalsIgnoreCase(fld)) {
				v = i;
				break;
			}
		}
		return v;
	}

	/**
	 * 创建shapefile，数据来源与ArcSDE数据库
	 * 
	 * @param filepath
	 *            = D:/shapefile/ 不带文件名，只是路径
	 */
	public boolean newShapeFile(String newshape, String typename, SimpleFeatureType type,
			List<Map<String, Object>> dataList) {
		boolean result = false;
		int index = 0;
		List<String> flist = getDBFiledList();
		FeatureCollection<SimpleFeatureType, SimpleFeature> FSS = getShapeFeatures(); // 原shape文件的所有要素集合
		FeatureIterator<SimpleFeature> iterator = FSS.features();
		try {
			ShapefileDataStore ds = getNewShapeDS(newshape, type); // 创建新的shape文件对象
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(typename,
					Transaction.AUTO_COMMIT);
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				SimpleFeature newfeature = writer.next();
				Map<String, Object> map = dataList.get(index);
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					String fld = entry.getKey();
					if ("id".equals(fld)) {
						continue;
					}
					Object objatrr = entry.getValue();
					int findex = listContains(flist, fld); // 包含
					if (findex > -1) {
						objatrr = feature.getAttribute(findex + 1);
					}
					newfeature.setAttribute(fld, objatrr);
				}
				Geometry defGeo = (Geometry) feature.getDefaultGeometry();
				defGeo.normalize();
				newfeature.setAttribute("the_geom", defGeo);
				writer.write();
				index++;
			}
			writer.close();
			ds.dispose();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			iterator.close();
		}
		return result;
	}

	/**
	 * 插入到空间表中
	 */
	public void shapeToDB(String layerid) {
		try {
			// CoordinateReferenceSystem srcCRS = getCRS();
			// MathTransform tranf = CRS.findMathTransform(srcCRS,
			// DefaultGeographicCRS.WGS84, true);
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(this.shapeFile).toURI().toURL());
			// store.setCharset(Charset.forName("utf-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature feature = itertor.next();
				Geometry srcGeo = (Geometry) feature.getDefaultGeometry();
				// Geometry tarGeo = JTS.transform(srcGeo, tranf);
				// Point p = tarGeo.getCentroid();
				Point p = srcGeo.getCentroid();
				if (!p.isEmpty()) {
					System.out.println(p.getX() + "," + p.getY() + "," + parseRowID(feature.getID()));
					insertToYS(layerid, feature, p);
				}
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void insertToYS(String layerid, SimpleFeature feature, Point p) {
		String sql = "insert into db_yaosu(LAYERID,OBJECTID,PX,PY)VALUES(?,?,?,?)";
		String objectid = parseRowID(feature.getID());
		Map<Integer, Object> pm = new HashMap<Integer, Object>(); // 参数
		pm.put(1, layerid);
		pm.put(2, objectid);
		pm.put(3, p.getX());
		pm.put(4, p.getY());
		DBOper.insertDB(sql, pm);
	}

	public static void main(String[] args) {
		/*
		 * String wkt = "PROJCS[\"Beijing_1954_3_Degree_GK_CM_111E\"," +
		 * "  GEOGCS[\"GCS_Beijing_1954\",DATUM[\"D_Beijing_1954\",SPHEROID[\"Krasovsky_1940\",6378245.0,298.3]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199]],"
		 * +
		 * "  PROJECTION[\"Gauss_Kruger\"],PARAMETER[\"False_Easting\",13900.0],PARAMETER[\"False_Northing\",-4129200.0],PARAMETER[\"Central_Meridian\",112.5],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]"
		 * ;
		 */
		String shapefile = "D:/tmp/171/171.shp";
		ShapeUtils_1 shapeu = new ShapeUtils_1(shapefile); // 获取连接名
		// shapeu.shapeToMulJson("D:/log");
		shapeu.shapeToJson("D:/tmp/171/171.geojson", null, null);
		// String[] shps = {"D:/07/P07.shp","D:/07/P09.shp","D:/07/P10.shp"};
		// shapeu.mulshapeToJson("D:/1.geojson",shps);
		// String vv = shapeu.getLayerGeomType();
		// System.out.println(vv);
		// String fileEncode = EncodingDetect.getJavaEncode("D:/lc.geojson");

		/*
		 * try { //String data = FileUtils.readFileToString(new
		 * File("D:/522.geojson"),"GBK"); //FileUtils.writeStringToFile(new
		 * File("D:/522.prj"), wkt, "utf-8"); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		/*
		 * String path = "文件路径"; File file = new File(path); String fileCode =
		 * JudgeFileCode.getFileCode(file); if(fileCode.equals("UTF-8")){
		 * 
		 * }
		 */

	}

	public int __getShapePropertis(FeatureWriter<SimpleFeatureType, SimpleFeature> writer,
			List<Map<String, String>> mpkv) {
		int row = 0;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());// shape中读取数据
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			this.boundingBox = featureSource.getBounds();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature addFeature = itertor.next();
				Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
				defGeo.normalize();
				SimpleFeature newFeature = writer.next();
				newFeature.setAttribute("SHAPE", defGeo);// 多边范围.
				Map<String, Object> ma = new HashMap<>();
				StringBuilder sba = new StringBuilder();
				StringBuilder sbb = new StringBuilder();
				for (int i = 0; i < mpkv.size(); i++) {
					Map<String, String> mpv = mpkv.get(i);
					for (Map.Entry<String, String> entry : mpv.entrySet()) {
						String key = entry.getKey();// JQBH
						String value = entry.getValue();// JQBH
						String lowerc = key.toLowerCase();
						// if (lowerc.indexOf("shape") == 0 ||
						// "objectid".equals(lowerc) || lowerc.indexOf("fid") ==
						// 0
						// || "id".equals(lowerc)) {
						// continue;
						// }
						Object rv = addFeature.getAttribute(value);
						if (null != value && (null != value && null != rv) && !"OBJECTID".equals(value)
						// (!"OBJECTID".equals(value)&&null!=rv)
								&& (!"Shape_Area".equals(value) && null != rv)
								&& (!"Shape_Leng".equals(value) && null != rv)
								&& (!("Shape_Le_1").equals(value) && null != rv)
								&& (!"Shape_Le_2".equals(value) && null != rv)
								&& (!("Shape_Ar_1").equals(value) && null != rv)) {
							// ma.put(value, rv);
							sba.append(value + ",");
							sba.append(rv + ",");
							// newFeature.setAttribute(value, rv);
						}
					}
					// newFeature.setAttribute(mpkv.get(i), );
				}
				try {
					// writer.write();
					// 写入一条数据
					String key = sba.toString().substring(0, sba.toString().length() - 1);
					String value = sbb.toString().substring(0, sbb.toString().length() - 1);
					DBOper.insertRecord("insert into SDE");
					row += 1;
				} catch (Exception E) {
					System.out.println(E.getMessage());
				}
			}
			itertor.close();
			store.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	// shape叠加
	public Geometry _Overlay_Union(Geometry geometry1, Geometry geometry2) {
		// Polygon geometry1 = (Polygon) reader.read("POLYGON((0 0, 2 0 ,2 2, 0
		// 2,0 0))");
		// Polygon geometry2 = (Polygon) reader.read("POLYGON((0 0, 4 0 , 4 1, 0
		// 1, 0 0))");
		OverlayOp op = new OverlayOp(geometry1, geometry2);
		Geometry g2 = op.getResultGeometry(OverlayOp.UNION);
		return g2;
		// OverlayOp.UNION;
	}

	public void _shapeAppend(Geometry baseGe) {
		// 去Db中找到对应的ge

		int row = 0;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());// shape中读取数据
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();
			this.boundingBox = featureSource.getBounds();
			SimpleFeatureIterator itertor = featureSource.getFeatures().features();
			while (itertor.hasNext()) {
				SimpleFeature addFeature = itertor.next();
				// addFeature.get
				Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
				Geometry un_ge = _Overlay_Union(baseGe, defGeo);

			}
			// SimpleFeatureIterator itertor =
			// featureSource.getFeatures().features();
			// while (itertor.hasNext()) {
			// SimpleFeature addFeature = itertor.next();

		} catch (Exception e) {
			try {
				throw new Exception(e);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	public SimpleFeatureCollection __shapeAppend(SimpleFeatureCollection firFeatures) {
		SimpleFeatureCollection exe = null;
		int row = 0;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());// shape中读取数据
			store.setCharset(Charset.forName("UTF-8"));
			SimpleFeatureSource featureSource = store.getFeatureSource();

			SimpleFeatureCollection secondFeatures = featureSource.getFeatures();
			UnionFeatureCollection uf = new UnionFeatureCollection();
			exe = uf.execute(firFeatures, secondFeatures);
			File f=new File("D://FIndexDB//webProject//w_shape//a.shp");
			if (!f.exists()) {
				f.createNewFile();
			}
			writeShape("D://FIndexDB//webProject//w_shape//a.shp", exe);
		} catch (Exception e) {
			e.printStackTrace();
			//return exe;
			//throw new RuntimeException(e);
		}
		return exe;
	}

	public void writeShape(String destfilepath, SimpleFeatureCollection sfc) {
		try {
			// 1.创建目标shape文件
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
			params.put(ShapefileDataStoreFactory.URLP.key, new File(destfilepath).toURI().toURL());
			ShapefileDataStore ds = (ShapefileDataStore) factory.createNewDataStore(params);
			//设置geo
			
			// 2.设置 属性
			ds.createSchema(
					SimpleFeatureTypeBuilder.retype(sfc.getSchema(), sfc.getBounds().getCoordinateReferenceSystem()));
			//ds.setCharset(Charset.forName("UTF-8"));//GBK-->
			// 设置writer
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(ds.getTypeNames()[0],
					Transaction.AUTO_COMMIT);
			// 写记录
			SimpleFeatureIterator it = sfc.features();
			try {
				while (it.hasNext()) {
					SimpleFeature f = it.next();					
					Geometry defGeo = (Geometry) f.getDefaultGeometry();	

					//SimpleFeature fNew = writer.next();
					//Geometry defGeo = (Geometry) addFeature.getDefaultGeometry();
					SimpleFeature newFeature = writer.next();
					if (null!=defGeo&&!defGeo.isValid()) {
						defGeo.normalize();
						newFeature.setDefaultGeometry(defGeo);
					}			
					//newFeature.setAttribute("SHAPE", defGeo);
					
//					for (String key : fields) {
//						if ("SHAPE".equals(key) || "OBJECTID".equals(key)) {
//							continue;
//						}
//						Object rv = addFeature.getAttribute(key);
//						newFeature.setAttribute(key, rv);
//					}
					//fNew.setAttribute("SHAPE", defGeo);// 多边范围.
					
					//判断非空--->
					
					List<Object> attributes = f.getAttributes();
					List<Object> fullAttr = new ArrayList<>();
					for (int i = 0; i < attributes.size(); i++) {
						if(null==attributes.get(i)) {
							fullAttr.add(null);
						}else{
							if (attributes.get(i) instanceof Geometry) {
								System.out.println("我拿到了需要的属性对象"+attributes.get(i));//MULTIPOLYGON (((940.1341400000019 62718.827255, 937.8802090000027 62718.865095, 934.3607460000003 62719.891224, 931.3029950000018 62720.782709, 930.1708480000034 62719.72168, 929.5087609999991 62717.450831, 928.0042589999994 62714.890243, 927.928995000002 62710.407172, 928.4469659999995 62706.994378, 930.710079000004 62703.822569, 933.6634819999999 62700.372688, 940.4439870000024 62698.395665, 946.6229740000017 62696.59413, 953.7080099999985 62697.669613, 959.700420000001 62695.922408, 965.0070090000045 62695.833319, 969.6399280000041 62695.755539, 972.6858590000029 62698.132759, 972.7385140000024 62701.263189, 972.7825590000048 62703.892693, 971.2925840000025 62704.669163, 970.8190840000025 62706.305415, 970.3153310000016 62709.623976, 969.6320790000009 62710.208112, 965.983554000004 62711.271806, 966.1555050000043 62712.019975, 968.7314770000012 62714.225194, 968.9775350000018 62715.98065900001, 967.6379850000048 62717.545356, 965.4973160000027 62717.581295, 960.2383100000006 62717.669586, 957.2534990000022 62718.539847, 955.0347560000046 62718.577097, 949.775650000005 62718.665389, 944.5166430000008 62718.75358, 940.1341400000019 62718.827255)))								
								// ge=(Geometry) attributes.get(i);
								//fullAttr.add(ge);								
							}else{
								fullAttr.add(attributes.get(i));
							}					
						}
						//fullAttr.add(null);
					}
					//fullAttr.add(9, "tt");
					System.out.println("ss");
					for (int i = 0; i < fullAttr.size(); i++) {
						if (fullAttr.get(i) instanceof com.vividsolutions.jts.geom.Geometry) {
							com.vividsolutions.jts.geom.Geometry go=(Geometry)fullAttr.get(i);
							newFeature.setAttribute(i,go);
						}else
							newFeature.setAttribute(i, fullAttr.get(i));
					}	
					//newFeature.setAttributes(fullAttr);
					writer.write();
				}
			} finally {
				it.close();
			}
			writer.close();
			ds.dispose();
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException(e);
		}

	}

	// public void write(String filepath) {
	// try {
	// //创建shape文件对象
	// File file = new File(filepath);
	// Map<String, Serializable> params = new HashMap<String, Serializable>();
	// params.put( ShapefileDataStoreFactory.URLP.key, file.toURI().toURL() );
	// ShapefileDataStore ds = (ShapefileDataStore) new
	// ShapefileDataStoreFactory().createNewDataStore(params);
	// //定义图形信息和属性信息
	// SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
	// tb.setCRS(DefaultGeographicCRS.WGS84);
	// tb.setName("shapefile");
	// tb.add("the_geom", Point.class);
	// tb.add("POIID", Long.class);
	// tb.add("NAMEC", String.class);
	// ds.createSchema(tb.buildFeatureType());
	// ds.setCharset(Charset.forName("GBK"));
	// //设置Writer
	// FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
	// ds.getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);
	// //写下一条
	// SimpleFeature feature = writer.next();
	// feature.setAttribute("the_geom", new GeometryFactory().createPoint(new
	// Coordinate(116.123, 39.345)));
	// feature.setAttribute("POIID", 1234567890l);
	// feature.setAttribute("NAMEC", "某兴趣点1");
	// feature = writer.next();
	// feature.setAttribute("the_geom", new GeometryFactory().createPoint(new
	// Coordinate(116.456, 39.678)));
	// feature.setAttribute("POIID", 1234567891l);
	// feature.setAttribute("NAMEC", "某兴趣点2");
	// writer.write();
	// writer.close();
	// ds.dispose();
	//
	// //读取刚写完shape文件的图形信息
	// ShpFiles shpFiles = new ShpFiles(filepath);
	// ShapefileReader reader = new ShapefileReader(shpFiles, false, true, new
	// GeometryFactory(), false);
	// try {
	// while (reader.hasNext()) {
	// System.out.println(reader.nextRecord().shape());
	// }
	// } finally {
	// reader.close();
	// }
	// } catch (Exception e) { }
	// }

	private DataStore createShapeFile() {
		// TODO Auto-generated method stub
		ShapefileDataStoreFactory sfs = new ShapefileDataStoreFactory();
		try {
			Map<String, Serializable> params = new HashMap<String, Serializable>();

			return sfs.createDataStore(new HashMap<>());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void writeToShapefile(DataStore data, FeatureCollection collection) {
		DefaultTransaction transaction = null;
		FeatureStore store = null;
		try {
			String[] featureNames = data.getTypeNames();
			String featureName = featureNames[0];
			// 创建默认的事务对象
			transaction = new DefaultTransaction();
			// 同时标明数据源使用的要素名称，通常Shapefile文件名称和Shapefile类型名称通常是一样的。
			store = (FeatureStore) data.getFeatureSource(featureName);
			// 关联默认事务和数据源
			store.setTransaction(transaction);
			// 增加要素信息到数据源
			store.addFeatures(collection);
			// 提交
			transaction.commit();
			// 关闭
			transaction.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				// 回滚
				transaction.rollback();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

//	public void write2() {
//		try {
//			// 定义属性
//			final SimpleFeatureType TYPE = DataUtilities.createType("Location",
//					"location:Point," + // <- the geometry attribute: Point type
//							"POIID:String," + // <- a String attribute
//							"MESHID:String," + // a number attribute
//							"OWNER:String");
//			SimpleFeatureCollection collection = FeatureCollections.newCollection();
//			GeometryFactory geometryFactory = new GeometryFactory();
//			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
//
//			double latitude = Double.parseDouble("116.123456789");
//			double longitude = Double.parseDouble("39.120001");
//			String POIID = "2050003092";
//			String MESHID = "0";
//			String OWNER = "340881";
//			Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
//			Object[] obj = { point, POIID, MESHID, OWNER };
//			SimpleFeature feature = featureBuilder.buildFeature(null, obj);
//			collection.add(feature);
//			feature = featureBuilder.buildFeature(null, obj);
//			collection.add(feature);
//			File newFile = new File("D:/newPoi.shp");
//			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
//			Map<String, Serializable> params = new HashMap<String, Serializable>();
//			params.put("url", newFile.toURI().toURL());
//			params.put("create spatial index", Boolean.TRUE);
//			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//			newDataStore.createSchema(TYPE);
//			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
//
//			Transaction transaction = new DefaultTransaction("create");
//			String typeName = newDataStore.getTypeNames()[0];
//			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
//
//			if (featureSource instanceof SimpleFeatureStore) {
//				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
//				featureStore.setTransaction(transaction);
//				try {
//					featureStore.addFeatures(collection);
//					transaction.commit();
//				} catch (Exception problem) {
//					problem.printStackTrace();
//					transaction.rollback();
//				} finally {
//					transaction.close();
//				}
//			} else {
//				System.out.println(typeName + " does not support read/write access");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
