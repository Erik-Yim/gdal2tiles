package org.walkgis.tiles.utfgrid.web;

import cn.com.enersun.dgpmicro.problem.Application;
import cn.com.enersun.dgpmicro.problem.entity.Style;
import cn.com.enersun.dgpmicro.problem.translation.BaseEntity;
import cn.com.enersun.dgpmicro.problem.translation.LocationsWithShape;
import cn.com.enersun.dgpmicro.problem.utfgrid.*;
import cn.com.enersun.dgpmicro.problem.util.DrawUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.walkgis.utils.common.GlobalGeodetic;
import com.walkgis.utils.common.GlobalMercator;
import com.walkgis.utils.service.GeoJsonServicesImpl;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.SQLReady;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.walkgis.tiles.utfgrid.MainApplication;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author JerFer
 * @date 2019/4/4---14:57
 */
@RestController
@RequestMapping(value = "image")
public class ImageWMSController extends GeoJsonServicesImpl<LocationsWithShape, String> {

    @Value("${cache.tile-path}")
    public String cachePath;

    public static Map<String, LinkedHashMap<String, String>> tbInfoMap = new ConcurrentHashMap();

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            tbInfoMap = objectMapper.readValue(MainApplication.class.getClassLoader().getResourceAsStream("tbInfo.json"), ConcurrentHashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final ExecutorService servicePool = Executors.newCachedThreadPool();//线程池
    private final Semaphore servicePoolLimit = new Semaphore(200);//信号量，控制线程池的最大线程数，防止用户过多时冲垮内存
    private GlobalGeodetic globalGeodetic;

    @Autowired
    private SQLManager sqlManager;
    private RTree<Integer, com.github.davidmoten.rtree.geometry.Geometry> rTree;
    private Map<String, Object> tileJson;

    /**
     * 返回一个WMS的图片
     *
     * @param crs
     * @param width
     * @param height
     * @param bbox
     * @param style
     * @return
     */
    @RequestMapping(value = "wms")
    public void imageWMS(
            @RequestParam(value = "CRS", defaultValue = "EPSG:4326") String crs,
            @RequestParam(value = "WIDTH") Integer width,
            @RequestParam(value = "HEIGHT") Integer height,
            @RequestParam(value = "BBOX") String bbox,
            @RequestParam(value = "style") String style,
            HttpServletResponse response
    ) {
        Style style1 = new Style();
        if (!StringUtils.isEmpty(style)) {
            try {
                style1 = new ObjectMapper().readValue(style, Style.class);
            } catch (IOException e) {
                style1 = new Style();
            }
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        globalGeodetic = new GlobalGeodetic("", 256);
        String[] xxx = bbox.split(",");
        Envelope envelope = new Envelope(Double.parseDouble(xxx[0]), Double.parseDouble(xxx[2]), Double.parseDouble(xxx[1]), Double.parseDouble(xxx[3]));

        String sql = "select t.* from river t ";
        if (!StringUtils.isEmpty(bbox))
            sql += " where ST_Intersects (t.shape,ST_MakeEnvelope(" + bbox + ",4326))";
        List<BaseEntity> countyAreas = sqlManager.execute(new SQLReady(sql), BaseEntity.class);
        rTree = RTree.create();
        countyAreas.forEach(baseEntity -> {
            try {
                org.locationtech.jts.geom.MultiLineString multiPolygon = (org.locationtech.jts.geom.MultiLineString) new WKBReader().read(WKBReader.hexToBytes(((PGobject) baseEntity.getShape()).getValue()));
                Envelope envelope1 = multiPolygon.getEnvelopeInternal();
                rTree = rTree.add(baseEntity.getId(), Geometries.rectangleGeographic(envelope1.getMinX(), envelope1.getMinY(), envelope1.getMaxX(), envelope1.getMaxY()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        com.github.davidmoten.rtree.geometry.Rectangle rectangle = rTree.mbr().get();
        tileJson = new HashMap<>();
        tileJson.put("attribution", "");
        tileJson.put("bounds", Arrays.asList(rectangle.x1(), rectangle.y1(), rectangle.x2(), rectangle.y2()));
        tileJson.put("center", Arrays.asList(0, 0, 4));
        tileJson.put("created", countyAreas.size());
        tileJson.put("description", "One");
        tileJson.put("filesize", countyAreas.size());
        tileJson.put("grids", Arrays.asList(
                "http://localhost:8084/ywgh-problem/image/utfgrid/{z}/{x}/{-y}.grid.json"
        ));
        tileJson.put("id", "utfgrid");
        tileJson.put("legend", "");
        tileJson.put("mapbox_logo", false);
        tileJson.put("maxzoom", 17);
        tileJson.put("minzoom", 1);
        tileJson.put("name", "Geography Class");
        tileJson.put("private", false);
        tileJson.put("scheme", "xyz");
        tileJson.put("template", "");
        tileJson.put("tilejson", "2.2.0");
        tileJson.put("tiles", Arrays.asList(
                "http://localhost:8084/ywgh-problem/image/utfgrid/{z}/{x}/{-y}.grid.json"
        ));
        tileJson.put("version", "1.0.0");
        tileJson.put("webpage", "");
        //#region 设置样式
        Graphics2D g = bufferedImage.createGraphics();
        // 设置出图规则
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);// 开启抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);// Alpha插值算法，选取偏重速度的算法
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);// 禁用抖动
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);// 呈现规则，选取偏重速度的算法
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);// 笔画规范，设置几何形状应保持不变
        //endregion

        //#region 设置变换矩阵
        AffineTransform translate = AffineTransform.getTranslateInstance(-envelope.getMinX(), -envelope.getMaxY());
        AffineTransform scale = AffineTransform.getScaleInstance(width / (envelope.getMaxX() - envelope.getMinX()), height / (envelope.getMaxY() - envelope.getMinY()));
        AffineTransform mirror_y = new AffineTransform(1, 0, 0, -1, 0, envelope.getMaxY());
        AffineTransform world2pixel = new AffineTransform(mirror_y);
        world2pixel.concatenate(scale);
        world2pixel.concatenate(translate);

        DrawUtils utils = new DrawUtils(world2pixel);

        ShapeWriter writer = new ShapeWriter();
        writer.setRemoveDuplicatePoints(true);
        //#endregion

        //#region 绘图
        g.setColor(Color.BLUE);
        for (BaseEntity locationsWithShape : countyAreas) {
            if (locationsWithShape.getShape() instanceof PGobject) {
                Object shapeObject = locationsWithShape.getShape();
                Geometry geometry = null;
                if (shapeObject != null) {
                    PGobject pGeobject = (PGobject) shapeObject;
                    try {
                        geometry = new WKBReader().read(WKBReader.hexToBytes(pGeobject.getValue()));
                        Shape shape = writer.toShape(geometry);
                        Shape screenShape = world2pixel.createTransformedShape(shape);
                        g.draw(screenShape);
//                        if (geometry.getGeometryType().equalsIgnoreCase("Point") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiPoint")) {
//                            Integer radius = 2;
//                            Point2D coords = utils.lonlatTranslatescreen(geometry.getCoordinate());
//                            if (null != style1.getImage()) {
//                                Circle circle = style1.getImage();
//
//                                radius = circle.getRadius() == null ? 2 : circle.getRadius();
//                                if (circle.getStroke() != null) {
//                                    cn.com.enersun.dgpmicro.problem.entity.sub.Stroke stroke = style1.getStroke();
//                                    BasicStroke basicStroke = new BasicStroke(
//                                            stroke.getWidth(),
//                                            stroke.getLineCap().getValue(),
//                                            stroke.getLineJoin().getValue(),
//                                            stroke.getMiterLimit(),
//                                            stroke.getLineDash(),
//                                            0.0f
//                                    );
//                                    g.setStroke(basicStroke);
//                                    g.setColor(Color.decode(circle.getStroke().getColor()));
//                                    g.drawOval((int) coords.getX(), (int) coords.getY(), radius, radius);
//                                }
//                                g.setColor(Color.decode(circle.getFill().getColor()));
//                                g.fillOval((int) coords.getX(), (int) coords.getY(), radius, radius);
//                            }
//                            utils.drawGeometry(g, geometry);
//                        } else if (geometry.getGeometryType().equalsIgnoreCase("LineString") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiLineString")) {
//                            if (null != style1.getStroke()) {
//                                cn.com.enersun.dgpmicro.problem.entity.sub.Stroke stroke = style1.getStroke();
//                                BasicStroke basicStroke = new BasicStroke(
//                                        stroke.getWidth(),
//                                        stroke.getLineCap().getValue(),
//                                        stroke.getLineJoin().getValue(),
//                                        stroke.getMiterLimit(),
//                                        stroke.getLineDash(),
//                                        0.0f
//                                );
//                                g.setStroke(basicStroke);
//                                g.setColor(Color.decode(stroke.getColor()));
//                            }
//                            utils.drawGeometry(g, geometry);
//                        } else if (geometry.getGeometryType().equalsIgnoreCase("Polygon") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiPolygon")) {
//                            if (null != style1.getFill()) {
//                                g.setColor(Color.decode(style1.getFill().getColor()));
//                            }
//                            if (null != style1.getStroke()) {
//                                Stroke stroke = style1.getStroke();
//                                BasicStroke basicStroke = new BasicStroke(
//                                        stroke.getWidth(),
//                                        stroke.getLineCap().getValue(),
//                                        stroke.getLineJoin().getValue(),
//                                        stroke.getMiterLimit(),
//                                        stroke.getLineDash(),
//                                        0.0f
//                                );
//                                g.setStroke(basicStroke);
//                                g.setColor(Color.decode(stroke.getColor()));
//                            }
//                            utils.drawGeometry(g, geometry);
//                        }
                    } catch (Exception var7) {
                        var7.printStackTrace();
                    }
                }
            }
        }
        //endregion

        response.setContentType("image/png");
        try (ServletOutputStream os = response.getOutputStream()) {
            ImageIO.write(bufferedImage, "png", os);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 进来的是XYZ scheme
     *
     * @param layerName
     * @param x
     * @param y
     * @param z
     * @return
     */
    @RequestMapping(value = "tile/{z}/{x}/{y}.png", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void generateVectorTiles(
            HttpServletResponse response,
            @RequestParam(value = "layerName", defaultValue = "vtdemo") String layerName,
            @RequestParam(value = "CRS") String crs,
            @PathVariable("x") Integer x,
            @PathVariable("y") Integer y,
            @PathVariable("z") Integer z
    ) throws Exception {
        ReadWriteLock lock = new ReentrantReadWriteLock();

        final Lock readLock = lock.readLock();
        final Lock writeLock = lock.writeLock();

        File file = new File(cachePath + File.separator + layerName + File.separator + z + File.separator + x + File.separator + String.format("%d.%s", y, "mvt"));
        if (!file.exists()) {
            //#region 下载内容
            double[] bboxs = new double[]{0, 0, 0, 0};
            if (crs.equalsIgnoreCase("EPSG:4326"))
                bboxs = new GlobalGeodetic("", 256).tileLatLonBounds(x, y, z);
            else if (crs.equalsIgnoreCase("EPSG:3857"))
                bboxs = new GlobalMercator(256).tileLatLonBounds(x, y, z);
            else throw new Exception("不支持的地理坐标系");

            Map<String, List> entityMap = new ConcurrentHashMap<>();

            String sql = "SELECT t.* FROM river t  WHERE ST_Intersects (st_setsrid(t.shape,4326),ST_MakeEnvelope(" + bboxs[1] + "," + bboxs[0] + "," + bboxs[3] + "," + bboxs[2] + ",4326))";
            List<BaseEntity> tRiverList = sqlManager.execute(new SQLReady(sql), BaseEntity.class);

            //#region 设置样式
            BufferedImage bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bufferedImage.createGraphics();
            // 设置出图规则
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);// 开启抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);// Alpha插值算法，选取偏重速度的算法
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);// 禁用抖动
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);// 呈现规则，选取偏重速度的算法
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);// 笔画规范，设置几何形状应保持不变
            //endregion

            //#region 设置变换矩阵
            Envelope envelope = new Envelope(bboxs[1], bboxs[3], bboxs[0], bboxs[2]);
            AffineTransform translate = AffineTransform.getTranslateInstance(-envelope.getMinX(), -envelope.getMaxY());
            AffineTransform scale = AffineTransform.getScaleInstance(256 / (envelope.getMaxX() - envelope.getMinX()), 256 / (envelope.getMaxY() - envelope.getMinY()));
            AffineTransform mirror_y = new AffineTransform(1, 0, 0, -1, 0, envelope.getMaxY());
            AffineTransform world2pixel = new AffineTransform(mirror_y);
            world2pixel.concatenate(scale);
            world2pixel.concatenate(translate);

            DrawUtils utils = new DrawUtils(world2pixel);

            ShapeWriter writer = new ShapeWriter();
            writer.setRemoveDuplicatePoints(true);
            //#endregion

            //#region 绘图
            g.setColor(Color.BLUE);
            for (BaseEntity locationsWithShape : tRiverList) {
                if (locationsWithShape.getShape() instanceof PGobject) {
                    Object shapeObject = locationsWithShape.getShape();
                    Geometry geometry = null;
                    if (shapeObject != null) {
                        PGobject pGeobject = (PGobject) shapeObject;
                        try {
                            geometry = new WKBReader().read(WKBReader.hexToBytes(pGeobject.getValue()));
                            Shape shape = writer.toShape(geometry);
                            Shape screenShape = world2pixel.createTransformedShape(shape);
                            g.draw(screenShape);
//                        if (geometry.getGeometryType().equalsIgnoreCase("Point") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiPoint")) {
//                            Integer radius = 2;
//                            Point2D coords = utils.lonlatTranslatescreen(geometry.getCoordinate());
//                            if (null != style1.getImage()) {
//                                Circle circle = style1.getImage();
//
//                                radius = circle.getRadius() == null ? 2 : circle.getRadius();
//                                if (circle.getStroke() != null) {
//                                    cn.com.enersun.dgpmicro.problem.entity.sub.Stroke stroke = style1.getStroke();
//                                    BasicStroke basicStroke = new BasicStroke(
//                                            stroke.getWidth(),
//                                            stroke.getLineCap().getValue(),
//                                            stroke.getLineJoin().getValue(),
//                                            stroke.getMiterLimit(),
//                                            stroke.getLineDash(),
//                                            0.0f
//                                    );
//                                    g.setStroke(basicStroke);
//                                    g.setColor(Color.decode(circle.getStroke().getColor()));
//                                    g.drawOval((int) coords.getX(), (int) coords.getY(), radius, radius);
//                                }
//                                g.setColor(Color.decode(circle.getFill().getColor()));
//                                g.fillOval((int) coords.getX(), (int) coords.getY(), radius, radius);
//                            }
//                            utils.drawGeometry(g, geometry);
//                        } else if (geometry.getGeometryType().equalsIgnoreCase("LineString") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiLineString")) {
//                            if (null != style1.getStroke()) {
//                                cn.com.enersun.dgpmicro.problem.entity.sub.Stroke stroke = style1.getStroke();
//                                BasicStroke basicStroke = new BasicStroke(
//                                        stroke.getWidth(),
//                                        stroke.getLineCap().getValue(),
//                                        stroke.getLineJoin().getValue(),
//                                        stroke.getMiterLimit(),
//                                        stroke.getLineDash(),
//                                        0.0f
//                                );
//                                g.setStroke(basicStroke);
//                                g.setColor(Color.decode(stroke.getColor()));
//                            }
//                            utils.drawGeometry(g, geometry);
//                        } else if (geometry.getGeometryType().equalsIgnoreCase("Polygon") ||
//                                geometry.getGeometryType().equalsIgnoreCase("MultiPolygon")) {
//                            if (null != style1.getFill()) {
//                                g.setColor(Color.decode(style1.getFill().getColor()));
//                            }
//                            if (null != style1.getStroke()) {
//                                Stroke stroke = style1.getStroke();
//                                BasicStroke basicStroke = new BasicStroke(
//                                        stroke.getWidth(),
//                                        stroke.getLineCap().getValue(),
//                                        stroke.getLineJoin().getValue(),
//                                        stroke.getMiterLimit(),
//                                        stroke.getLineDash(),
//                                        0.0f
//                                );
//                                g.setStroke(basicStroke);
//                                g.setColor(Color.decode(stroke.getColor()));
//                            }
//                            utils.drawGeometry(g, geometry);
//                        }
                        } catch (Exception var7) {
                            var7.printStackTrace();
                        }
                    }
                }
            }
            //endregion

            response.setContentType("image/png");
            try (ServletOutputStream os = response.getOutputStream()) {
                ImageIO.write(bufferedImage, "png", os);
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //endregion
        } else {
        }
    }

    /**
     * 返回一个WMS的图片
     *
     * @return
     */
    @RequestMapping(value = "utfgrid/{z}/{x}/{y}.grid.json")
    public Map<String, Object> utfgrid(
            @PathVariable Integer z,
            @PathVariable Integer x,
            @PathVariable Integer y
    ) {
        double[] bbox = new GlobalGeodetic("", 256).tileLatLonBounds(x, y, z);
        Extent box = new Extent(bbox[0], bbox[1], bbox[2], bbox[3]);
        Request tile = new Request(256, 256, box);
        CoordTransform ctrans = new CoordTransform(tile);
        Grid grid = new Grid();
        Renderer renderer = new Renderer(grid, ctrans);
//        if (resFeas.size() > 0) {
//            renderer.apply(resFeas);
//        }
        return grid.encode();
    }


    @RequestMapping(value = "utfgrid/layer.json")
    public Map<String, Object> utfgridJson() {
        return tileJson;
    }

    /**
     * 返回一个WMS的图片
     *
     * @param srsname
     * @param bbox
     * @param deviceInfos
     * @return
     */
    @RequestMapping(value = "geojson")
    public String geoJson(
            @RequestParam(value = "srsname", defaultValue = "EPSG:4326") String srsname,
            @RequestParam(value = "bbox") String bbox,
            @RequestParam(value = "deviceInfos") String deviceInfos
    ) {
        //region 获取数据
        Map<String, List<String>> classDevIds = new HashMap<>();
        String[] strs = deviceInfos.split(",");

        Map<String, List<String>> feedersMap = new HashMap<>();

        for (int i = 0; i < strs.length; i++) {
            String[] devClass = strs[i].split("-");
            if (devClass[1].equalsIgnoreCase("200") || devClass[1].equalsIgnoreCase("300")) {
                if (!feedersMap.containsKey(devClass[1]))
                    feedersMap.put(devClass[1], new ArrayList<>());
                feedersMap.get(devClass[1]).add(devClass[0]);
            } else {
                if (!classDevIds.containsKey(devClass[1]))
                    classDevIds.put(devClass[1], new ArrayList<>());
                classDevIds.get(devClass[1]).add(devClass[0]);
            }
        }
        //查找非馈线的
        List<LocationsWithShape> resFeas = new ArrayList<>();
        resFeas.addAll(subQuery(classDevIds));
        //查找馈线的
        List<String> feederStrs = new ArrayList<>();
        feedersMap.entrySet().stream().forEach(a -> {
            feederStrs.addAll(a.getValue().stream().map(b -> "'" + b + "'").collect(Collectors.toList()));
        });

        if (feederStrs.size() > 0) {
            String sql = "select t.dev_id||'-'||t.class_id from locations t where t.feeder_id in (" + String.join(",", feederStrs) + ") and t.class_id <> '200' and t.class_id<>'300' and t.dev_id<>t.feeder_id";
            List<String> devIds = sqlManager.execute(new SQLReady(sql), String.class);

            classDevIds = new HashMap<>();
            for (String a : devIds) {
                String[] devClass = a.split("-");
                if (!classDevIds.containsKey(devClass[1]))
                    classDevIds.put(devClass[1], new ArrayList<>());
                classDevIds.get(devClass[1]).add(devClass[0]);
            }

            resFeas.addAll(subQuery(classDevIds));
        }

        //endregion
        try {
            return new ObjectMapper().writeValueAsString(toFeatures(resFeas));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private List<LocationsWithShape> buildShape() throws SQLException {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        List<LocationsWithShape> resFeas = new ArrayList<>();
        PGobject pGobject = new PGobject();
        pGobject.setType("Geometry");
        pGobject.setValue(WKBWriter.toHex(new WKBWriter().write(factory.createLineString(
                new Coordinate[]{new Coordinate(102.55926318855657, 24.132950387659747), new Coordinate(102.62415118904485, 24.12539728707381)}))));
        resFeas.add(new LocationsWithShape("2323", pGobject));

        pGobject = new PGobject();
        pGobject.setType("Geometry");
        pGobject.setValue(WKBWriter.toHex(new WKBWriter().write(factory.createLineString(
                new Coordinate[]{new Coordinate(102.7144450733222, 24.097588144007403), new Coordinate(102.71925159187688, 24.19303186959334)}))));
        resFeas.add(new LocationsWithShape("23434", pGobject));

        pGobject = new PGobject();
        pGobject.setType("Geometry");
        pGobject.setValue(WKBWriter.toHex(new WKBWriter().write(factory.createPoint(new Coordinate(102.7144450733222, 24.097588144007403)))));
        resFeas.add(new LocationsWithShape("234334", pGobject));
        return resFeas;
    }

    private static List<LocationsWithShape> buildData() {
        List<LocationsWithShape> list = new ArrayList<>();

        PGobject pGobject = new PGobject();
        pGobject.setType("Geometry");
        Geometry geometry = new GeometryFactory().createLineString(new Coordinate[]{
                new Coordinate(99.778029, 26.016677),
                new Coordinate(99.171349, 25.280917)});
        try {
            pGobject.setValue(WKBWriter.toHex(new WKBWriter().write(geometry)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LocationsWithShape locationsWithShape = new LocationsWithShape("12323", pGobject);
        list.add(locationsWithShape);

        geometry = new GeometryFactory().createLineString(new Coordinate[]{
                new Coordinate(100.952664, 22.273334),
                new Coordinate(104.153868, 24.067557)});
        try {
            pGobject.setValue(WKBWriter.toHex(new WKBWriter().write(geometry)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        locationsWithShape = new LocationsWithShape("12323", pGobject);
        list.add(locationsWithShape);
        return list;
    }

    private List<LocationsWithShape> subQuery(Map<String, List<String>> classDevIds) {
        List<Future<List<LocationsWithShape>>> resultFeatures = new ArrayList<>();
        tbInfoMap.forEach((k, v) -> {
            for (Map.Entry<String, String> entity : v.entrySet()) {
                String key = entity.getKey();
                String[] classIds = key.split("-");
                List<String> ids = new ArrayList<>();
                for (String str : classIds) if (classDevIds.containsKey(str)) ids.addAll(classDevIds.get(str));

                if (ids.size() <= 0) continue;
                String[] idsStr = new String[ids.size()];
                for (int i = 0; i < ids.size(); i++) {
                    idsStr[i] = "'" + ids.get(i) + "'";
                }

                //消耗一个信号量，即servicePoolLimit.v的值-1。当达到最大线程限制时，servicePoolLimit.v的值为0，acquire请求会挂起
                try {
                    servicePoolLimit.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String finalSql = "select t.dev_name,t.dev_id,t.class_id,k.shape from locations t ," + entity.getValue() + " k where t.dev_id=k.dev_id and t.class_id=k.class_id and  t.dev_id in(" + String.join(",", idsStr) + ")";
                Future<List<LocationsWithShape>> future = servicePool.submit(() -> {
                    List<LocationsWithShape> res = new ArrayList<>();
                    try {
                        SQLReady sqlReady = new SQLReady(String.format(finalSql, entity.getValue()));
                        res = sqlManager.execute(sqlReady, LocationsWithShape.class);
                    } catch (Exception ex) {

                    } finally {
                        //线程任务执行完，将信号量还回去
                        servicePoolLimit.release();
                    }
                    return res;
                });
                resultFeatures.add(future);
            }
        });
        List<LocationsWithShape> resFeas = new ArrayList<>();
        for (Future<List<LocationsWithShape>> fs : resultFeatures) {
            try {
                resFeas.addAll(fs.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resFeas;
    }
}
