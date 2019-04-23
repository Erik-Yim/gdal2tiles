/**
 * @author JerFer
 * @date 2019/3/29---13:55
 */
package com.walkgis.tiles;

import com.walkgis.tiles.util.GeopackageUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;


public class MainApp extends Application {
    public static GeopackageUtil geopackageUtil;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("mainapp.fxml"));
        primaryStage.setTitle("地图切片");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();

//        GDAL2Tiles gdal2tiles = new GDAL2Tiles(null);
//        try {
//            if (GDAL2Tiles.geopackage) {
//                geopackageUtil = new GeopackageUtil();
//                geopackageUtil.initGeopackage("E:\\date\\geopackage\\home.gpkg");
//            }
//            gdal2tiles.process();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }
}
