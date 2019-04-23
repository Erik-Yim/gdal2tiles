package com.walkgis.tiles.common;


/**
 * Created by JerFer
 * Date: 2017/12/13.
 */
public class ContentValue {
    public static int MAXZOOMLEVEL = 32;
    public static String[] resamplingList = new String[]{"average", "near", "bilinear", "cubic", "cubicspline", "lanczos", "antialias"};
    public static String[] profileList = new String[]{"mercator", "geodetic", "raster"};
    private static String[] webviewList = new String[]{"all", "google", "openlayers", "leaflet", "none"};
}
