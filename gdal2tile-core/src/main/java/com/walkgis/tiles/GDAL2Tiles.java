package com.walkgis.tiles;

import com.mortennobel.imagescaling.ResampleFilter;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import com.walkgis.tiles.util.GlobalGeodetic;
import com.walkgis.tiles.util.GlobalMercator;
import com.walkgis.tiles.util.PDFReader;
import mil.nga.geopackage.BoundingBox;
import org.apache.commons.cli.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Created by JerFer
 * Date: 2017/12/13.
 */
public class GDAL2Tiles {
    private static PDFReader reader = new PDFReader();
    private boolean stopped = false;
    private int tilesize = 256;
    private String tileext = "png";
    private boolean scaledquery = true;
    private int querysize = 4 * tilesize;
    private String resampling = "";
    private int tminz = -1, tmaxz = -1;
    private double ominx, ominy, omaxx, omaxy;
    private String profile;
    private double[] swne;
    //    private GeomEnvelop tileswne;
    private List<int[]> tminmax;
    private double[] out_gt;
    private int nativezoom = -1;
    private double[] tsize;

    private Coordinate leftTop;
    private Coordinate rightBottom;
    private double widthResolution;
    private double heightResolution;
    private CoordinateReferenceSystem referenceSystem;
    private Map<String, String> options;
    private String input;
    private String output;
    private BufferedImage in_ds, out_ds, alphaband;
    private CoordinateReferenceSystem out_srs;
    private Object out_drv, mem_drv;
    private GlobalMercator mercator;
    private GlobalGeodetic geodetic;
    private boolean dataBandsCount, kml;
    private String[] args;
    private Options parser;
    private int[] tileswne;
    private int[] in_nodata;
    private String tiledriver = "";
    private boolean overviewquery = false;
    public static boolean geopackage = false;

    public void process() throws IOException, SQLException {
        // Opening and preprocessing of the input file
        open_input();

        // Generation of main metadata files and HTML viewers
        generate_metadata();

        // Generation of the lowest tiles
        generate_base_tiles();

        // Generation of the overview tiles (higher in the pyramid)
        generate_overview_tiles();

        //generate_openlayers();
    }


    public void error(String msg, String details) {
        if (details != null) {
            System.out.println(msg + "\n\n" + details);
        } else
            System.out.println(msg);
    }


    public void progressbar(double complete) {
        //System.out.println("complete:" + complete);
    }

    public void stop() {
        this.stopped = true;
    }


    private String gettempfilename(String suffix) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        int d = 0 + (int) (Math.random() * (1000000000 - 0 + 1));
        String random_part = String.format("file%d", d);
        return tmpdir + File.separator + random_part + suffix;
    }


    //-l -p raster -z 0-5 -w none <image> <tilesdir>
    public GDAL2Tiles(String[] args) {
        this.stopped = false;
        this.input = null;
        this.output = null;

        this.tilesize = 256;
        this.tiledriver = "PNG";
        this.tileext = "png";

        this.scaledquery = true;
        this.querysize = 4 * this.tilesize;
        this.overviewquery = false;


        this.out_drv = null;
        this.mem_drv = null;
        this.in_ds = null;
        this.out_ds = null;
        this.out_srs = null;
        this.nativezoom = 0;
        this.tminmax = null;
        this.tsize = null;
        this.alphaband = null;
        this.dataBandsCount = false;
        this.out_gt = null;
//        this.tileswne = null;
        this.swne = null;
        this.ominx = 0;
        this.omaxx = 0;
        this.omaxy = 0;
        this.ominy = 0;

        this.stopped = false;
        this.input = null;
        this.output = null;

        this.tilesize = 256;
//        this.tiledriver = "PNG";
        this.tileext = "png";

        this.scaledquery = true;
        this.querysize = 4 * this.tilesize;
//        this.overviewquery = false;

        optparse_init();

        this.options = parse_options(args);
        this.args = parse_args(args);
        if (args.length <= 0) {
            error("No input file specified", null);
        }


        if (new File(args[args.length - 1]).isDirectory() ||
                (args.length > 0 && !new File(args[args.length - 1]).exists())) {
            this.output = args[args.length - 1];
            this.args = new String[]{args[args.length - 2]};
        }

        //多个输入
        if (this.args.length > 1) {
            error("", null);
        }

        this.input = this.args[0];

        if (!this.options.containsKey("title")) {
            this.options.put("title", new File(this.input).getName());
        }
        if (this.options.containsKey("url") && this.options.get("url").endsWith("/")) {
            this.options.put("url", this.options.get("url") + "/");
        }
        if (!this.options.containsKey("url")) {
            this.options.put("url", this.options.get("url") + new File(this.output).getPath() + "/");
        }

        this.resampling = "";

        this.tminz = -1;
        this.tmaxz = -1;
//
//        if (this.options.containsKey("zoom")) {
//            String[] minmax = this.options.get("zoom").split("-");
//            this.tminz = Integer.parseInt(minmax[0]);
//            if (minmax[1] != null) {
//                this.tmaxz = Integer.parseInt(minmax[1]);
//            } else this.tmaxz = this.tminz;
//        }

        this.kml = Boolean.parseBoolean(this.options.get("kml"));

        if (Boolean.parseBoolean(this.options.get("verbose"))) {
            System.out.println("Options:" + options);
            System.out.println("Input:" + input);
            System.out.println("Output:" + output);
        }
    }

    private void optparse_init() {
        Options p = new Options();

        p.addOption(new Option("p", "profile", true, ""));
        p.addOption(new Option("r", "resampling", false, ""));
        p.addOption(new Option("s", "s_srs", false, ""));
        p.addOption(new Option("z", "zoom", true, ""));
        p.addOption(new Option("e", "resume", false, ""));
        p.addOption(new Option("a", "srcnodata", false, ""));
        p.addOption(new Option("d", "tmscompatible", false, ""));
        p.addOption(new Option("v", "verbose", false, ""));
        p.addOption(new Option("q", "quiet", false, ""));

        OptionGroup g = new OptionGroup();
        g.addOption(new Option("k", "kml", false, ""));
        g.addOption(new Option("n", "kml", false, ""));
        g.addOption(new Option("u", "url", false, ""));
        p.addOptionGroup(g);

        OptionGroup g2 = new OptionGroup();
        g2.addOption(new Option("w", "webviewer", false, ""));
        g2.addOption(new Option("t", "title", false, ""));
        g2.addOption(new Option("c", "copyright", false, ""));
        g2.addOption(new Option("g", "googlekey", false, ""));
        g2.addOption(new Option("b", "bingkey", false, ""));
        p.addOptionGroup(g2);
        //设置默认值


        this.parser = p;

    }

    private void open_input() {
        this.out_drv = null;
        this.mem_drv = null;

        if (new File(this.input).exists()) {
            reader.init(this.input, this.output);
            this.in_ds = reader.getImage();
        }

        in_nodata = new int[]{};
        CoordinateReferenceSystem in_srs = reader.getReferenceSystem();
        //初始化in_nodata
        this.out_srs = in_srs;

//        if self.options.profile == 'mercator':
//            self.out_srs.ImportFromEPSG(900913)
//            elif self.options.profile == 'geodetic':
//            self.out_srs.ImportFromEPSG(4326)
//        else:
//            self.out_srs = self.in_srs

        this.out_ds = null;


        if (this.out_ds == null) {
            this.out_ds = this.in_ds;
        }

        //Read the georeference
        this.out_gt = getGeoTransform();

        this.ominx = out_gt[0];
        this.omaxx = out_gt[0] + this.out_ds.getWidth() * this.out_gt[1];
        this.omaxy = out_gt[3];
        this.ominy = out_gt[3] - this.out_ds.getHeight() * this.out_gt[1];

        if (this.options.get("profile").equals("mercator")) {
            this.mercator = new GlobalMercator(256);
//            this.tileswne = this.mercator.tileLatLonBounds();
            this.tminmax = new LinkedList<>();
            for (int tz = 0; tz < 32; tz++) {
                int[] tminxy = this.mercator.metersToTile(this.ominx, this.ominy, tz);
                int[] tmaxxy = this.mercator.metersToTile(this.omaxx, this.omaxy, tz);

                tminxy = new int[]{Math.max(0, tminxy[0]), Math.max(0, tminxy[1])};
                tmaxxy = new int[]{(int) Math.min(Math.pow(2, tz) - 1, tmaxxy[0]), (int) Math.min(Math.pow(2, tz) - 1, tmaxxy[1])};

                this.tminmax.add(tz, new int[]{tminxy[0], tminxy[1], tmaxxy[0], tmaxxy[1]});
            }

            if (this.tminz == -1) {
                this.tminz = this.mercator.zoomForPixelSize(this.out_gt[1] * Math.max(this.out_ds.getWidth(), this.out_ds.getHeight()) / (float) (this.tilesize));
            }
            if (this.tmaxz == -1) {
                this.tmaxz = this.mercator.zoomForPixelSize(this.out_gt[1]);
            }
        } else if (this.options.get("profile").equals("geodetic")) {
            this.geodetic = new GlobalGeodetic(null, 256);
//            this.tileswne = this.geodetic.tileLatLonBounds();
            this.tminmax = new LinkedList<>();
            for (int tz = 0; tz < 32; tz++) {
                int[] tminxy = this.geodetic.lonlatToTile(this.ominx, this.ominy, tz);
                int[] tmaxxy = this.geodetic.lonlatToTile(this.omaxx, this.omaxy, tz);

                tminxy = new int[]{Math.max(0, tminxy[0]), Math.max(0, tminxy[1])};
                tmaxxy = new int[]{Math.min((int) Math.pow(2, tz + 1) - 1, tmaxxy[0]),
                        (int) Math.min(Math.pow(2, tz) - 1, tmaxxy[1])};

                this.tminmax.add(tz, new int[]{tminxy[0], tminxy[1], tmaxxy[0], tmaxxy[1]});
            }

            if (this.tminz == -1) {
                this.tminz = this.geodetic.zoomForPixelSize(this.out_gt[1] *
                        Math.max(this.out_ds.getWidth(), this.out_ds.getHeight()) / (float) (this.tilesize));
            }
            if (this.tmaxz == -1) {
                this.tmaxz = this.geodetic.zoomForPixelSize(this.out_gt[1]);
            }
        } else if (this.options.get("profile").equals("raster")) {
            this.nativezoom = (int) (Math.max(Math.ceil(log2(this.out_ds.getWidth() / (float) (this.tilesize))),
                    Math.ceil(log2(this.out_ds.getHeight() / (float) (this.tilesize)))));

            if (this.tminz == -1) {
                this.tminz = 0;
            }
            if (this.tmaxz == -1) {
                this.tmaxz = this.nativezoom;
            }
            this.tminmax = new LinkedList<>();
            this.tsize = new double[this.tmaxz + 1];

            for (int tz = 0; tz < this.tmaxz + 1; tz++) {
                double tsize = Math.pow(2.0, this.nativezoom - tz) * this.tilesize;

                int[] tminxy = new int[]{0, 0};
                int[] tmaxxy = new int[]{
                        ((int) (Math.ceil(this.out_ds.getWidth() / tsize))) - 1,
                        ((int) (Math.ceil(this.out_ds.getHeight() / tsize))) - 1
                };

                this.tsize[tz] = Math.ceil(tsize);
                this.tminmax.add(tz, new int[]{tminxy[0], tminxy[1], tmaxxy[0], tmaxxy[1]});
            }
//            this.tileswne = new int[]{0, 0, 0, 0};
        }

        try {
            if (geopackage)
                MainApp.geopackageUtil.createTileTable("home", getBoundBox(), this.tminz, this.tmaxz, this.tileext);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void generate_metadata() {

        if (!new File(this.output).exists()) {
            new File(this.output).mkdirs();
        }
        double[] southWest = new double[2];
        double[] northEast = new double[2];
        if (this.options.get("profile").equals("mercator")) {
            southWest = this.mercator.metersToLatLon(this.ominx, this.ominy);
            northEast = this.mercator.metersToLatLon(this.omaxx, this.omaxy);

            southWest = new double[]{Math.max(-85.05112878, southWest[0]), Math.max(-180.0, southWest[1])};
            northEast = new double[]{Math.min(85.05112878, northEast[0]), Math.min(180.0, northEast[1])};

            this.swne = new double[]{southWest[0], southWest[1], northEast[0], northEast[1]};
            //初始化Openyers
//            if not self.options.resume or not os.path.exists(os.path.join(self.output, 'openlayers.html')):
//                f = open(os.path.join(self.output, 'openlayers.html'), 'w')
//                f.write(self.generate_openlayers())
//                f.close()

        } else if (this.options.get("profile").equals("geodetic")) {
            southWest = new double[]{this.ominy, this.ominx};
            northEast = new double[]{this.omaxy, this.omaxx};

            southWest = new double[]{Math.max(-90.0, southWest[0]), Math.max(-180.0, southWest[1])};
            northEast = new double[]{Math.min(90.0, northEast[0]), Math.min(180.0, northEast[1])};

            this.swne = new double[]{southWest[0], southWest[1], northEast[0], northEast[1]};
            //初始化Openyers
//            if not self.options.resume or not os.path.exists(os.path.join(self.output, 'openlayers.html')):
//                f = open(os.path.join(self.output, 'openlayers.html'), 'w')
//                f.write(self.generate_openlayers())
//                f.close()
        } else if (this.options.get("profile").equals("raster")) {

            southWest = new double[]{this.ominy, this.ominx};
            northEast = new double[]{this.omaxy, this.omaxx};

            this.swne = new double[]{southWest[0], southWest[1], northEast[0], northEast[1]};
            //初始化Openyers
//            if not self.options.resume or not os.path.exists(os.path.join(self.output, 'openlayers.html')):
//                f = open(os.path.join(self.output, 'openlayers.html'), 'w')
//                f.write(self.generate_openlayers())
//                f.close()
        }
        // Generate tilemapresource.xml.

        if (this.options.containsKey("resume") && !new File(this.output + File.separator + "tilemapresource.xml").exists()) {
            File file = new File(this.output + File.separator + "tilemapresource.xml");
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(generate_tilemapresource().getBytes());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Generation of the base tiles (the lowest in the pyramid) directly from the input raster
     */
    private void generate_base_tiles() throws SQLException {
        int tminx = this.tminmax.get(this.tmaxz)[0];
        int tminy = this.tminmax.get(this.tmaxz)[1];
        int tmaxx = this.tminmax.get(this.tmaxz)[2];
        int tmaxy = this.tminmax.get(this.tmaxz)[3];

        BufferedImage ds = this.out_ds;

        int querysize = this.querysize;

        int tcount = (1 + Math.abs(tmaxx - tminx)) * (1 + Math.abs(tmaxy - tminy));

        int ti = 0;

        int tz = this.tmaxz;

        for (int ty = tmaxy; ty > tminy - 1; ty--) {
            for (int tx = tminx; tx < tmaxx + 1; tx++) {
                if (this.stopped) {
                    break;
                }

                ti += 1;

                String tilefilename = this.output + File.separator + tz + File.separator + String.format("%s_%s.%s", tx, ty, this.tileext);

                if (Boolean.parseBoolean(this.options.get("resume")) && new File(tilefilename).exists()) {
                    progressbar(ti / (float) tcount);
                    continue;
                }

                if (!new File(tilefilename).getParentFile().exists()) {
                    new File(tilefilename).getParentFile().mkdirs();
                }

                double[] b = null;
                if (this.options.get("profile").equals("mercator")) {
                    b = this.mercator.tileBounds(tx, ty, tz);
                } else if (this.options.get("profile").equals("geodetic")) {
                    b = this.geodetic.tileBounds(tx, ty, tz);
                }

                int rx = 0, ry = 0, rxsize = 0, rysize = 0, wx = 0, wy = 0, wxsize = 0, wysize = 0;

                if (this.options.get("profile").equals("mercator") || this.options.get("profile").equals("geodetic")) {
                    int[][] rbwb = this.geo_query(ds, b[0], b[3], b[2], b[1], 0);

                    rbwb = this.geo_query(ds, b[0], b[3], b[2], b[1], querysize);

                    rx = rbwb[0][0];
                    ry = rbwb[0][1];
                    rxsize = rbwb[0][2];
                    rysize = rbwb[0][3];
                    wx = rbwb[1][0];
                    wy = rbwb[1][1];
                    wxsize = rbwb[1][2];
                    wysize = rbwb[1][3];
                } else if (this.options.get("profile").equals("raster")) {
                    int tsize = (int) this.tsize[tz];//tilesize in raster coordinates for actual zoom
                    int xsize = this.out_ds.getWidth();//size of the raster in pixels
                    int ysize = this.out_ds.getHeight();
                    if (tz >= this.nativezoom) {
                        querysize = this.tilesize;//int(2 * * (self.nativezoom - tz) * self.tilesize)
                    }

                    rx = (tx) * tsize;
                    rxsize = 0;
                    if (tx == tmaxx)
                        rxsize = xsize % tsize;
                    if (rxsize == 0)
                        rxsize = tsize;

                    rysize = 0;
                    if (ty == tmaxy)
                        rysize = ysize % tsize;
                    if (rysize == 0)
                        rysize = tsize;
                    ry = ysize - (ty * tsize) - rysize;

                    wx = 0;
                    wy = 0;
                    wxsize = (int) (rxsize / (float) (tsize) * this.tilesize);
                    wysize = (int) (rysize / (float) (tsize) * this.tilesize);

                    if (wysize != this.tilesize)
                        wy = this.tilesize - wysize;

                }

                ///开始处理图片了//////////////////////////////////////////////////////////////////////

                BufferedImage dstile = new BufferedImage(this.tilesize, this.tilesize, BufferedImage.TYPE_INT_ARGB);

//                data = ds.ReadRaster(rx, ry, rxsize, rysize, wxsize, wysize,
//                        band_list=list(range(1, self.dataBandsCount + 1)))
                System.out.println(String.format("rx,ry,rxsize,rysize,wxsize,wysize=%d,%d,%d,%d,%d,%d", rx, ry, rxsize, rysize, wxsize, wysize));
                BufferedImage data = ds.getSubimage(rx, ry, rxsize, rysize);

                if (data != null) {
                    if (this.tilesize == querysize) {

//                            dstile.WriteRaster(wx, wy, wxsize, wysize, data,
//                                    band_list=list(range(1, self.dataBandsCount + 1)))

                        Graphics2D graphics2D = dstile.createGraphics();
                        graphics2D.drawImage(data, wx, wy, wxsize, wysize, null);
                    } else {
                        BufferedImage dsquery = new BufferedImage(querysize, querysize, BufferedImage.TYPE_INT_ARGB);

//                            dsquery.WriteRaster(wx, wy, wxsize, wysize, data,
//                                    band_list=list(range(1, self.dataBandsCount + 1)))

                        Graphics2D graphics2D = dsquery.createGraphics();
                        graphics2D.drawImage(data, wx, wy, wxsize, wysize, null);
                        dstile = scale_query_to_tile(dsquery, dstile.getWidth());
                    }
                }
                if (!this.options.get("resampling").equals("antialias")) {
                    try {
                        ImageIO.write(dstile, this.tileext, new File(tilefilename));   //将其保存在C:/imageSort/targetPIC/下
                        if (geopackage)
                            MainApp.geopackageUtil.insertTile(new File(tilefilename), tz, tx, ty);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!Boolean.parseBoolean(this.options.get("verbose")) && !Boolean.parseBoolean(this.options.get("quiet"))) {
                    this.progressbar(ti / (double) tcount);
                }
            }
        }

        if (geopackage)
            MainApp.geopackageUtil.createMatrix(tz);
    }

    private void generate_overview_tiles() throws IOException, SQLException {
        System.out.println("Generating Overview Tiles:");

        int tcount = 0;
        for (int tz = this.tmaxz - 1; tz > this.tminz - 1; tz--) {
            int[] tminxytmaxxy = this.tminmax.get(tz);
            tcount += (1 + Math.abs(tminxytmaxxy[2] - tminxytmaxxy[0])) * (1 + Math.abs(tminxytmaxxy[3] - tminxytmaxxy[1]));
        }

        int ti = 0;

        for (int tz = this.tmaxz - 1; tz > this.tminz - 1; tz--) {
            int[] tminxytmaxxy = this.tminmax.get(tz);
            for (int ty = tminxytmaxxy[3]; ty > tminxytmaxxy[1] - 1; ty--) {
                for (int tx = tminxytmaxxy[0]; tx < tminxytmaxxy[2] + 1; tx++) {
                    if (this.stopped) {
                        break;
                    }
                    ti += 1;
                    String tilefilename = this.output + File.separator + tz + File.separator + String.format("%s_%s.%s", tx, ty, this.tileext);

//                    System.out.println(ti + "/" + tcount + tilefilename);

                    if (Boolean.parseBoolean(this.options.get("resume")) && new File(tilefilename).exists()) {
                        progressbar(ti / (float) tcount);
                        continue;
                    }

                    if (!new File(tilefilename).getParentFile().exists()) {
                        new File(tilefilename).getParentFile().mkdirs();
                    }

                    BufferedImage dsquery = new BufferedImage(2 * this.tilesize, 2 * this.tilesize, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage dstile = new BufferedImage(this.tilesize, this.tilesize, BufferedImage.TYPE_INT_ARGB);


                    for (int y = 2 * ty; y < 2 * ty + 2; y++) {
                        for (int x = 2 * tx; x < 2 * tx + 2; x++) {
                            int[] minxytmaxxy = this.tminmax.get(tz + 1);
                            if (x >= minxytmaxxy[0] && x <= minxytmaxxy[2] &&
                                    y >= minxytmaxxy[1] && y <= minxytmaxxy[3]) {
                                BufferedImage dsquerytile = ImageIO.read(new File(this.output + File.separator + ((int) (tz + 1)) + File.separator + String.format("%s_%s.%s", x, y, this.tileext)));
                                int tileposy, tileposx;
                                if ((ty == 0 && y == 1) ||
                                        (ty != 0 && (y % (2 * ty)) != 0)) {
                                    tileposy = 0;
                                } else {
                                    tileposy = this.tilesize;
                                }

                                if (tx > 0)
                                    tileposx = x % (2 * tx) * this.tilesize;
                                else if (tx == 0 && x == 1) {
                                    tileposx = this.tilesize;
                                } else {
                                    tileposx = 0;
                                }

//                                tileposx, tileposy, self.tilesize, self.tilesize,
//                                        dsquerytile.ReadRaster(0, 0, self.tilesize, self.tilesize),
//                                        band_list=list(range(1, tilebands + 1)))

                                Graphics2D graphics2D = dsquery.createGraphics();
                                graphics2D.drawImage(dsquerytile, tileposx, tileposy, this.tilesize, this.tilesize, null);
                                System.out.println(String.format("(%d,%d)===", tx, ty) + String.format("(%d,%d)===", x, y) + String.format("(%d,%d)", tileposx, tileposy));
                            }
                        }
                    }

                    dstile = scale_query_to_tile(dsquery, dstile.getWidth());

                    if (!this.options.get("resampling").equals("antialias")) {
                        try {
                            ImageIO.write(dstile, this.tileext, new File(tilefilename));   //将其保存在C:/imageSort/targetPIC/下
                            if (geopackage)
                                MainApp.geopackageUtil.insertTile(new File(tilefilename), tz, tx, ty);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (!Boolean.parseBoolean(this.options.get("verbose")) && !Boolean.parseBoolean(this.options.get("quiet"))) {
                        this.progressbar(ti / (double) tcount);
                    }
                }
            }
            if (geopackage)
                MainApp.geopackageUtil.createMatrix(tz);
        }
        if (geopackage)
            MainApp.geopackageUtil.close();
    }

    private BufferedImage scale_query_to_tile(BufferedImage dsquery, int tilesize) {
        if (this.options.get("resampling").equals("average")) {
            ResampleOp resampleOp = new ResampleOp(tilesize, tilesize);
            ResampleFilter filter = ResampleFilters.getBiCubicHighFreqResponse();
            resampleOp.setFilter(filter);
            return resampleOp.filter(dsquery, null);
        } else if (this.options.get("resampling").equals("antialias")) {
            ResampleOp resampleOp = new ResampleOp(tilesize, tilesize);
            ResampleFilter filter = ResampleFilters.getBiCubicHighFreqResponse();
            resampleOp.setFilter(filter);
            return resampleOp.filter(dsquery, null);
        } else {
            ResampleOp resampleOp = new ResampleOp(tilesize, tilesize);
            ResampleFilter filter = ResampleFilters.getBiCubicHighFreqResponse();
            resampleOp.setFilter(filter);
            return resampleOp.filter(dsquery, null);
        }
    }

    private int[][] geo_query(BufferedImage ds, double ulx, double uly, double lrx, double lry, int querysize) {
        double[] geotran = getGeoTransform();
        int rx = (int) ((ulx - geotran[0]) / geotran[1] + 0.001);
        int ry = (int) ((uly - geotran[3]) / geotran[5] + 0.001);
        int rxsize = (int) ((lrx - ulx) / geotran[1] + 0.5);
        int rysize = (int) ((lry - uly) / geotran[5] + 0.5);

        int wxsize, wysize;
        if (querysize == 0) {
            wxsize = rxsize;
            wysize = rysize;
        } else {
            wxsize = querysize;
            wysize = querysize;
        }

        int wx = 0;
        if (rx < 0) {
            int rxshift = Math.abs(rx);
            wx = (int) (wxsize * ((float) (rxshift) / rxsize));
            wxsize = wxsize - wx;
            rxsize = rxsize - (int) (rxsize * ((float) (rxshift) / rxsize));
            rx = 0;
        }
        if ((rx + rxsize) > ds.getWidth()) {
            wxsize = (int) (wxsize * ((float) (ds.getWidth() - rx) / rxsize));
            rxsize = ds.getWidth() - rx;
        }

        int wy = 0;

        if (ry < 0) {
            int ryshift = Math.abs(ry);
            wy = (int) (wysize * ((float) (ryshift) / rysize));
            wysize = wysize - wy;
            rysize = rysize - (int) (rysize * ((float) (ryshift) / rysize));
            ry = 0;
        }

        if ((ry + rysize) > ds.getHeight()) {
            wysize = (int) (wysize * ((float) (ds.getHeight() - ry) / rysize));
            rysize = ds.getHeight() - ry;
        }
        return new int[][]{new int[]{rx, ry, rxsize, rysize}, new int[]{wx, wy, wxsize, wysize}};
    }

    /**
     * 输出XML配置文件
     */
    private String generate_tilemapresource() {
        Map<String, String> args = new HashMap<>();
        args.put("title", this.options.get("title"));
        args.put("south", String.valueOf(this.swne[0]));
        args.put("west", String.valueOf(this.swne[1]));
        args.put("north", String.valueOf(this.swne[2]));
        args.put("east", String.valueOf(this.swne[3]));

        args.put("titlesize", String.valueOf(this.tilesize));
        args.put("tileformat", this.tileext);
        args.put("publishurl", this.options.get("url"));
        args.put("profile", this.options.get("profile"));

        if (this.out_srs != null) {
//            args.put("src", this.out_srs.toWKT());
            args.put("src", this.out_srs.toString());
        } else args.put("src", "");

        String s = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "    <TileMap version=\"1.0.0\" tilemapservice=\"http://tms.osgeo.org/1.0.0\">\n" +
                "      <Title>%(title)s</Title>\n" +
                "      <Abstract></Abstract>\n" +
                "      <SRS>%(srs)s</SRS>\n" +
                "      <BoundingBox minx=\"%(west).14f\" miny=\"%(south).14f\" maxx=\"%(east).14f\" maxy=\"%(north).14f\"/>\n" +
                "      <Origin x=\"%(west).14f\" y=\"%(south).14f\"/>\n" +
                "      <TileFormat width=\"%(tilesize)d\" height=\"%(tilesize)d\" mime-type=\"image/%(tileformat)s\" extension=\"%(tileformat)s\"/>\n" +
                "      <TileSets profile=\"%(profile)s\">";

//        for (int z = this.tminz; z < this.tmaxz + 1; z++) {
//            s += String.format("<TileSet href=\"%s%d\" units-per-pixel=\"%.14f\" order=\"%d\"/>\\n", args.get("publishurl"), z, (Math.pow(2, (this.nativezoom - z)) * this.out_gt[1]), z);
//        }
        s += "      </TileSets>\n" +
                "    </TileMap>";
        return s;
    }

    private void generate_openlayers() {
        String html = "" +
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta http-equiv='imagetoolbar' content='no'/>\n" +
                "    <title>Title</title>\n" +
                "    <style type=\"text/css\">\n" +
                "        html, body {\n" +
                "            overflow: hidden;\n" +
                "            padding: 0;\n" +
                "            height: 100%;\n" +
                "            width: 100%;\n" +
                "            font-family: 'Lucida Grande', Geneva, Arial, Verdana, sans-serif;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            margin: 10px;\n" +
                "            background: #fff;\n" +
                "        }\n" +
                "\n" +
                "        h1 {\n" +
                "            margin: 0;\n" +
                "            padding: 6px;\n" +
                "            border: 0;\n" +
                "            font-size: 20pt;\n" +
                "        }\n" +
                "\n" +
                "        #header {\n" +
                "            height: 43px;\n" +
                "            padding: 0;\n" +
                "            background-color: #eee;\n" +
                "            border: 1px solid #888;\n" +
                "        }\n" +
                "\n" +
                "        #subheader {\n" +
                "            height: 12px;\n" +
                "            text-align: right;\n" +
                "            font-size: 10px;\n" +
                "            color: #555;\n" +
                "        }\n" +
                "\n" +
                "        #map {\n" +
                "            height: 95%;\n" +
                "            border: 1px solid #888;\n" +
                "        }\n" +
                "\n" +
                "        .olImageLoadError {\n" +
                "            display: none;\n" +
                "        }\n" +
                "\n" +
                "        .olControlLayerSwitcher .layersDiv {\n" +
                "            border-radius: 10px 0 0 10px;\n" +
                "        }\n" +
                "    </style>\n" +
                "    <script src=\"http://www.openlayers.org/api/2.12/OpenLayers.js\"></script>" +
                "";
        html += "<script>\n" +
                "        var map;\n" +
                "        var mapBounds = new OpenLayers.Bounds(" + this.swne[0] + ", " + this.swne[1] + ", " + this.swne[2] + ", " + this.swne[3] + ");\n" +
                "        var mapMinZoom = " + this.tminz + ";\n" +
                "        var mapMaxZoom = " + this.tmaxz + ";\n" +
                "        var emptyTileURL = \"http://www.maptiler.org/img/none.png\";\n" +
                "        OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;\n" +
                "\n" +
                "        function init() {\n" +
                "            var options = {\n" +
                "                div: \"map\",\n" +
                "                controls: [],\n" +
                "                maxExtent: new OpenLayers.Bounds(" + this.swne[0] + ", " + this.swne[1] + ", " + this.swne[2] + ", " + this.swne[3] + "),\n" +
                "                maxResolution: " + (Math.pow(2, this.nativezoom) * this.out_gt[1]) + ",\n" +
                "                numZoomLevels: " + (this.tmaxz + 1) + "\n" +
                "            };\n" +
                "            map = new OpenLayers.Map(options);\n" +
                "\n" +
                "            var layer = new OpenLayers.Layer.TMS(\"TMS Layer\", \"\",\n" +
                "                {\n" +
                "                    serviceVersion: '.',\n" +
                "                    layername: '.',\n" +
                "                    alpha: true,\n" +
                "                    type: \"png\",\n" +
                "                    getURL: getURL\n" +
                "                });\n" +
                "\n" +
                "            map.addLayer(layer);\n" +
                "            map.zoomToExtent(mapBounds);\n" +
                "            map.addControls([new OpenLayers.Control.PanZoomBar(),\n" +
                "                new OpenLayers.Control.Navigation(),\n" +
                "                new OpenLayers.Control.MousePosition(),\n" +
                "                new OpenLayers.Control.ArgParser(),\n" +
                "                new OpenLayers.Control.Attribution()]);\n" +
                "        }\n" +
                "\n" +
                "        function getURL(bounds) {\n" +
                "            bounds = this.adjustBounds(bounds);\n" +
                "            var res = this.getServerResolution();\n" +
                "            var x = Math.round((bounds.left - this.tileOrigin.lon) / (res * this.tileSize.w));\n" +
                "            var y = Math.round((bounds.bottom - this.tileOrigin.lat) / (res * this.tileSize.h));\n" +
                "            var z = this.getServerZoom();\n" +
                "            var path = '.' + \"/\" + '.' + \"/\" + z + \"/\" + x + \"_\" + y + \".\" + this.type;\n" +
                "            var url = this.url;\n" +
                "            if (OpenLayers.Util.isArray(url)) {\n" +
                "                url = this.selectUrl(path, url);\n" +
                "            }\n" +
                "            if (mapBounds.intersectsBounds(bounds) && (z >= mapMinZoom) && (z <= mapMaxZoom)) {\n" +
                "                return url + path;\n" +
                "            } else {\n" +
                "                return emptyTileURL;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function getWindowHeight() {\n" +
                "            if (self.innerHeight) return self.innerHeight;\n" +
                "            if (document.documentElement && document.documentElement.clientHeight)\n" +
                "                return document.documentElement.clientHeight;\n" +
                "            if (document.body) return document.body.clientHeight;\n" +
                "            return 0;\n" +
                "        }\n" +
                "\n" +
                "        function getWindowWidth() {\n" +
                "            if (self.innerWidth) return self.innerWidth;\n" +
                "            if (document.documentElement && document.documentElement.clientWidth)\n" +
                "                return document.documentElement.clientWidth;\n" +
                "            if (document.body) return document.body.clientWidth;\n" +
                "            return 0;\n" +
                "        }\n" +
                "\n" +
                "        function resize() {\n" +
                "            var map = document.getElementById(\"map\");\n" +
                "            var header = document.getElementById(\"header\");\n" +
                "            var subheader = document.getElementById(\"subheader\");\n" +
                "            map.style.height = (getWindowHeight() - 80) + \"px\";\n" +
                "            map.style.width = (getWindowWidth() - 20) + \"px\";\n" +
                "            header.style.width = (getWindowWidth() - 20) + \"px\";\n" +
                "            subheader.style.width = (getWindowWidth() - 20) + \"px\";\n" +
                "            if (map.updateSize) {\n" +
                "                map.updateSize();\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        onresize = function () {\n" +
                "            resize();\n" +
                "        };\n" +
                "\n" +
                "    </script>";
        html += "</head>\n" +
                "<body onload=\"init()\">\n" +
                "<div id=\"header\"><h1>title</h1></div>\n" +
                "<div id=\"subheader\">Generated by\n" +
                "    <a href=\"http://www.klokan.cz/projects/gdal2tiles/\">GDAL2Tiles</a>,\n" +
                "    Copyright &copy; 2008\n" +
                "    <a href=\"http://www.klokan.cz/\">Klokan Petr Pridal</a>,\n" +
                "    <a href=\"http://www.gdal.org/\">GDAL</a> &amp;\n" +
                "    <a href=\"http://www.osgeo.org/\">OSGeo</a>\n" +
                "    <a href=\"http://code.google.com/soc/\">GSoC</a>\n" +
                "</div>\n" +
                "<div id=\"map\"></div>\n" +
                "<script type=\"text/javascript\">resize()</script>\n" +
                "</body>\n" +
                "</html>";
        File file = new File(this.output + File.separator + "openlayers.html");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(html.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private double log2(double x) {
        return Math.log10(x) / Math.log10(2);
    }

    private double[] getGeoTransform() {
        Envelope envelope = null;
        if (this.options.get("profile").equals("mercator")) {
            envelope = reader.getEnvelope();
            double[] min = lonLat2Mercator(envelope.getMinX(), envelope.getMinY());
            double[] max = lonLat2Mercator(envelope.getMaxX(), envelope.getMaxY());
            envelope = new Envelope();
            envelope.init(min[0], max[0], min[1], max[1]);
        } else {
            envelope = reader.getEnvelope();
        }

        widthResolution = (envelope.getMaxX() - envelope.getMinX()) / this.in_ds.getWidth();
        heightResolution = (envelope.getMinY() - envelope.getMaxY()) / this.in_ds.getHeight();
        //mercator(11440253.586413905, 0.2985821410518959, 0.0, 2877395.4927671393, 0.0, -0.2985821410519452)
        //84(102.76954650878781, 2.5533771416895517e-06, 0.0, 25.013439812256067, 0.0, -2.553377141690922e-06)

        return new double[]{envelope.getMinX(), widthResolution, 0.0, envelope.getMinY(), 0.0, heightResolution};
    }

    private BoundingBox getBoundBox() {
        Envelope envelope = reader.getEnvelope();
        if (this.options.get("profile").equals("mercator")) {
            envelope = reader.getEnvelope();
            double[] min = lonLat2Mercator(envelope.getMinX(), envelope.getMinY());
            double[] max = lonLat2Mercator(envelope.getMaxX(), envelope.getMaxY());
            envelope = new Envelope();
            envelope.init(min[0], max[0], min[1], max[1]);
        }

        return new BoundingBox(envelope.getMinX(), envelope.getMaxX(), envelope.getMaxX(), envelope.getMaxY());
    }

    public double[] lonLat2Mercator(double lon, double lat) {
        double x = lon * 20037508.342789 / 180;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180);
        y = y * 20037508.34789 / 180;
        return new double[]{x, y};
    }

    private String[] parse_args(String[] args) {
        return new String[]{args[args.length - 1], args[args.length - 2]};
    }

    private Map<String, String> parse_options(String[] args) {
        Map<String, String> options = new HashMap<>();
        BasicParser parser = new BasicParser();
        CommandLine cl;
        try {
            cl = parser.parse(this.parser, args);
            if (cl.getOptions().length > 0) {
                for (Option option : cl.getOptions()) {
                    options.put(option.getLongOpt(), cl.getOptionValue(option.getOpt()));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //初始化默认值
//        verbose=False, profile="mercator", kml=False, url='',
//                webviewer='all', copyright='', resampling='average', resume=False,
//                googlekey='INSERT_YOUR_KEY_HERE', bingkey='INSERT_YOUR_KEY_HERE'

        if (!options.containsKey("verbose"))
            options.put("verbose", "false");//是否输出日志
        if (!options.containsKey("profile"))
            options.put("profile", "mercator");//切片的模式
        if (!options.containsKey("kml"))
            options.put("kml", "false");//是否输出KML
        if (!options.containsKey("url"))
            options.put("url", "");//地址
        if (!options.containsKey("webviewer"))
            options.put("webviewer", "all");
        if (!options.containsKey("copyright"))
            options.put("copyright", "");
        if (!options.containsKey("resampling"))
            options.put("resampling", "average");//重采样模式
        if (!options.containsKey("resume"))
            options.put("resume", "false");
        if (!options.containsKey("googlekey"))
            options.put("googlekey", "INSERT_YOUR_KEY_HERE");
        if (!options.containsKey("bingkey"))
            options.put("bingkey", "INSERT_YOUR_KEY_HERE");
        return options;
    }
}
