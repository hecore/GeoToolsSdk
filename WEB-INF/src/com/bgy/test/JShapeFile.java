package com.bgy.test;

import java.io.File;  
import java.nio.charset.Charset;  
  
import org.geotools.data.FileDataStore;  
import org.geotools.data.FileDataStoreFinder;  
import org.geotools.data.shapefile.ShapefileDataStore;  
import org.geotools.data.simple.SimpleFeatureSource;  
import org.geotools.map.FeatureLayer;  
import org.geotools.map.Layer;  
import org.geotools.map.MapContent;  
import org.geotools.styling.SLD;  
import org.geotools.styling.Style;  
import org.geotools.swing.JMapFrame;  
import org.geotools.swing.data.JFileDataStoreChooser;  
  
  
public class JShapeFile {  
    public static void main(String[] args) throws Exception{  
//      GeotoolsTest geotoolsTest = new GeotoolsTest();  
//      geotoolsTest.showMap();  
        File file = JFileDataStoreChooser.showOpenFile("shp", null);  
        if (file == null) {  
            return;  
        }  
  
        FileDataStore store = FileDataStoreFinder.getDataStore(file);  
        //中文转码，避免乱码  
        ((ShapefileDataStore) store).setCharset(Charset.forName("GBK"));  
          
        SimpleFeatureSource featureSource = store.getFeatureSource();  
  
        // Create a map content and add our shapefile to it  
        MapContent map = new MapContent();  
        map.setTitle("Quickstart");  
          
        Style style = SLD.createSimpleStyle(featureSource.getSchema());  
        Layer layer = new FeatureLayer(featureSource, style);  
        map.addLayer(layer);  
  
        // Now display the map  
        JMapFrame.showMap(map);  
    }  
}  

