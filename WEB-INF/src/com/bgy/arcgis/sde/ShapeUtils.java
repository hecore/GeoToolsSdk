package com.bgy.arcgis.sde;

import java.io.File;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.css.selector.TypeName;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.geotools.arcsde.data.ArcSDEDataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.ResourceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.bgy.util.DBUtils;
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

import ucar.ma2.ArrayDouble.D3.IF;

public class ShapeUtils {
	private String shapeFile;
	private BoundingBox boundingBox = null;
	private CoordinateReferenceSystem coordRefCRS = null;
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

	public ShapeUtils(String shapefile) {
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

	public void defshapeToJson(String jsonfile, Map<String, String> map, SimpleFeatureBuilder sfbuilder) {
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
					// SimpleFeatureBuilder fbuilder = new
					// SimpleFeatureBuilder(feature.getFeatureType());
					// //重新构建要素类别
					// SimpleFeature newfeature =
					// fbuilder.buildFeature(parseRowID(feature.getID()));
					// //都重新构建新的要素，ID
					// newfeature.setAttributes(feature.getAttributes());
					// newfeature.setDefaultGeometry(tarGeo);
					// fcs.add(newfeature);
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

	private SimpleFeatureType tyFeatureType(String typename, List<Map<String, Object>> list) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName(typename);
		builder.setCRS(getCRS()); // <- Coordinate reference system
		for (Map<String, Object> mp : list) {
			String name = mp.get("name") + "";
			char type = (mp.get("type") + "").toCharArray()[0];
			// (char)( mp.get("type"));
			int len = Integer.parseInt(mp.get("len") + "");
			// Integer.valueOf((String) mp.get("len"));
			int pre = Integer.parseInt(mp.get("pre") + "");
			// Integer.valueOf((String) mp.get("pre"));
			switch (type) {
			case 'S':
				builder.length(len).add(name, String.class);
				break;
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
	public SimpleFeatureCollection getShapeFeatures() {
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		try {
			ShapefileDataStore sds = (ShapefileDataStore) dataStoreFactory
					.createDataStore(new File(shapeFile).toURI().toURL());
			sds.setCharset(Charset.forName("UTF-8"));
			//sds.setCharset(Charset.forName("GBK"));
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
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriterAppend(typename,
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
	 * 把两个shape合并成一个shape
	 * 
	 * @param oneList
	 * @param twoList
	 * @param dstshp
	 *            合并后的shape文件路径 D:/tmp/filename.shp
	 */
	public void unionShapeFile(SimpleFeatureCollection oneList, SimpleFeatureCollection twoList, String newshape,
			String typename, SimpleFeatureType type) {
		SimpleFeatureIterator oneiter = oneList.features();
		// features.hasNext();
		// FeatureIterator<SimpleFeature> oneiter = features;
		SimpleFeatureIterator twoiter = twoList.features();
		// FeatureIterator<SimpleFeature> twoiter = twoList.features();
		try {
			ShapefileDataStore ds = getNewShapeDS(newshape, type); // 创建新的shape文件对象
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriterAppend(typename,
					Transaction.AUTO_COMMIT);
			while (oneiter.hasNext()) {
				SimpleFeature feature = oneiter.next();
				SimpleFeature newfeature = writer.next();
				newfeature.setAttributes(feature.getAttributes());
				writer.write();
			}
			while (twoiter.hasNext()) {
				SimpleFeature feature = twoiter.next();
				SimpleFeature newfeature = writer.next();		
				newfeature.setAttributes(feature.getAttributes());
				writer.write();
			}
			writer.close();
			ds.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oneiter.close();
			twoiter.close();
		}
	}
	
	public void unionShapeWithSde(SimpleFeatureCollection shapeList, SimpleFeatureCollection sdeList, String newshape,
			String typename, SimpleFeatureType type,List<Object> li){
		SimpleFeatureIterator oneiter = shapeList.features();
		SimpleFeatureIterator twoiter = sdeList.features();
		try {
			ShapefileDataStore ds = getNewShapeDS(newshape, type); // 创建新的shape文件对象
			FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriterAppend(typename,
					Transaction.AUTO_COMMIT);
		
			while (oneiter.hasNext()) {
				SimpleFeature feature = oneiter.next();	
				SimpleFeature newfeature = writer.next();
				for (Object str : li) {
					String temp=str+"";
					if (null!=str&&null!=feature.getAttribute(temp)&&!temp.equals("SHAPE")) {
						
						newfeature.setAttribute(temp,feature.getAttribute(temp));
					}			
				}
				newfeature.setAttributes(feature.getAttributes());
				writer.write();
			}
			while (twoiter.hasNext()) {
				SimpleFeature feature = twoiter.next();
				SimpleFeature newfeature = writer.next();	
				//newfeature.setAttribute("SHAPE",feature.getAttribute("SHAPE"));
				for (Object str : li) {
					String temp=str+"";
					if (null!=str&&null!=feature.getAttribute(temp)&&!temp.equals("SHAPE")) {
						System.out.println(temp);
						newfeature.setAttribute(temp,feature.getAttribute(temp));
					}			
				}
				writer.write();
			}
			writer.close();
			ds.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oneiter.close();
			twoiter.close();
		}
	}

	private List<Object> charsetFeature(List<Object> attributes,String charOrgin,String charDest) {
		List<Object> reo = new ArrayList<>();
		for (Object o : attributes) {
			if (o instanceof String) {
				try {
					// String iso = new String(((String) o).getBytes("UTF-8"),
					// "ISO-8859-1");
					// String utf8 = new String(iso.getBytes("ISO-8859-1"),
					// "UTF-8");
					String utf8 = gbk2Utf8((String) o,charOrgin,charDest);
							//new String(((String) o).getBytes("gbk"), "utf-8");
					o = utf8;

					// String str=new String(((String)
					// o).getBytes("GBK"),"ISO-8859-1");
					// o=new String(((String)
					// str).getBytes("ISO-8859-1"),"UTF-8");
					//
					// o=new String();
					// o=new String(((String) o).getBytes("GBK"),"UTF-8");
					// GBK-U8
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			reo.add(o);
		}
		return reo;
	}

	private String gbk2Utf8(String gbk,String charOrgin,String charDest) throws UnsupportedEncodingException {
		//String iso = new String(gbk.getBytes("UTF-8"), "ISO-8859-1");
		String utf8 = new String(gbk.getBytes(charOrgin), charDest);
		return utf8;
	}
//	public static String gbk2utf8(String gbk){
//		 	char[] c = gbk.toCharArray();  
//		   byte[] fullByte = new byte[3*c.length];  
//		    for (int i=0; i<c.length; i++) {  
//		        String binary = Integer.toBinaryString(c[i]);  
//		        StringBuffer sb = new StringBuffer();  
//		        int len = 16 - binary.length();  
//		        //前面补零  
//		        for(int j=0; j<len; j++){  
//		                sb.append("0");  
//		            }  
//		        sb.append(binary);  
//		        //增加位，达到到24位3个字节  
//		        sb.insert(0, "1110");  
//		            sb.insert(8, "10");  
//		            sb.insert(16, "10");  
//		            fullByte[i*3] = Integer.valueOf(sb.substring(0, 8), 2).byteValue();//二进制字符串创建整型  
//		            fullByte[i*3+1] = Integer.valueOf(sb.substring(8, 16), 2).byteValue();  
//		            fullByte[i*3+2] = Integer.valueOf(sb.substring(16, 24), 2).byteValue();  
//		    }  
//		   try {
//			return  new String(fullByte,"UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}



	public static List<Object> getShapeLi(SimpleFeatureType shapeu) {
		// TODO Auto-generated method stub
		List li=new ArrayList<>();
		List<AttributeDescriptor> ad = shapeu.getAttributeDescriptors();
		for (AttributeDescriptor o : ad) {
			System.out.println(o.getName());
			li.add(o.getName());
		}
		return li;
	//	System.out.println(typeName+"显示名称 "+attributeDescriptors+" 显示类型 "+binding);
	}
	
	public static void main(String[] args) {
		String layername = "万柏林土地用途区";
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
			//throw new RuntimeException(e);
		}

		// 1.Shp simple
		String oneShp = "D:/FIndexDB/mdb/2018/04/25/212/shape/尖草坪土地用途区.shp";
		// "F:/tmp/北京POI数据/北京市界.shp"; //原shape
		String twoShp = "D:/FIndexDB/mdb/2018/04/25/212/shape/万柏林土地用途区.shp";
		// "F:/tmp/北京POI数据/区县界_QXJ_PG.shp"; //目标shape
		String dstShp = "D:/tmp//shape/three.shp";
		File f1 = new File("D:/tmp/shape");
		if (!f1.exists()) {
			f1.mkdirs();
		}
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
		//SimpleFeatureCollection twoList = twopeu.getShapeFeatures();
		// 3.Message get
		SimpleFeatureType schema = twoList.getSchema();

		List<AttributeType> types = schema.getTypes();
		
		// 4.combine map
		List<Object> shapeLi = getShapeLi(twoshapeu);
		
		// 3.shape append

		// 5.union
		// SimpleFeatureType st = oneshapeu.tyFeatureType("three",
		// fldList);//TDYTQLXMC:TDYTQLXMC,TDYTQLXMC:TDYTQLXMC,Shape:Shape

		oneshapeu.unionShapeWithSde(oneList, twoList, dstShp, "three", twoList.getSchema(),shapeLi );
				//store.getSchema());
		
		System.out.println("合并完毕");
		db.tearDown();
	}

}
