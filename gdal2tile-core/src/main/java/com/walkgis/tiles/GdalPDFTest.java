package com.walkgis.tiles;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;

public class GdalPDFTest {
    public static void main(String[] args) {
        // 注册所有的驱动
        ogr.RegisterAll();
        // 为了支持中文路径，请添加下面这句代码
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        // 为了使属性表字段支持中文，请添加下面这句
        gdal.SetConfigOption("SHAPE_ENCODING", "");

        Driver driver = gdal.GetDriverByName("PDF");
        if (driver == null) {
            System.out.println("FAILURE: Output driver 'PDF' not recognized.");
        }

        Dataset dataset = gdal.Open("E:\\Data\\pdf\\84.pdf", gdalconstConstants.GA_ReadOnly);
        if (dataset == null) {
            System.out.println("GDAL read error: " + gdal.GetLastErrorMsg());
        }

        driver = dataset.GetDriver();
        System.out.println("driver short name: " + driver.getShortName());
        System.out.println("driver long name: " + driver.getLongName());
        System.out.println("metadata list: " + driver.GetMetadata_List());

        int xsize = dataset.getRasterXSize();
        int ysize = dataset.getRasterYSize();
        int count = dataset.getRasterCount();
        String proj = dataset.GetProjection();
        SpatialReference sp = new SpatialReference(proj);
        Band band = dataset.GetRasterBand(1);

        // 左上角点坐标 lon lat: transform[0]、transform[3]
        // 像素分辨率 x、y方向 : transform[1]、transform[5]
        // 旋转角度: transform[2]、transform[4])
        double[] transform = dataset.GetGeoTransform();
        for (int i = 0; i < transform.length; i++) {
            System.out.println("transform: " + transform[i]);
        }
    }
}
