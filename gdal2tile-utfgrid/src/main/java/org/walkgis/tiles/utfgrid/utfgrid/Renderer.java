package org.walkgis.tiles.utfgrid.utfgrid;

import cn.com.enersun.dgpmicro.problem.translation.LocationsWithShape;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author JerFer
 * @date 2019/4/9---12:58
 */
public class Renderer {
    private Grid grid;
    private CoordTransform ctrans;
    private Request req;

    public Renderer(Grid grid, CoordTransform ctrans) {
        this.grid = grid;
        this.ctrans = ctrans;
        this.req = ctrans.getRequest();
    }

    public void apply(List<LocationsWithShape> data) throws ParseException {
        RTree<String, com.github.davidmoten.rtree.geometry.Geometry> tree = RTree.create();
        for (LocationsWithShape locationsWithShape : data) {
            Object shapeObject = locationsWithShape.getShape();
            if (shapeObject instanceof PGobject) {
                Geometry geometry = new WKBReader().read(WKBReader.hexToBytes(((PGobject) shapeObject).getValue()));
                if (geometry != null) {
                    if (geometry.getGeometryType().equalsIgnoreCase("Point")) {
                        Point point = (Point) geometry;
                        tree = tree.add(locationsWithShape.getId(), Geometries.pointGeographic(point.getX(), point.getY()));
                    } else {
                        Envelope envelope = geometry.getEnvelopeInternal();
                        tree = tree.add(locationsWithShape.getId(), Geometries.rectangleGeographic(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()));
                    }
                }
            }
        }

        for (int y = 0, height = this.req.getHeight(); y < height; y += this.grid.getResolution()) {
            List<String> row = new ArrayList();
            for (int x = 0, width = this.req.getWidth(); x < width; x += this.grid.getResolution()) {
                double[] minxy = this.ctrans.backward(x, y);
                double[] maxxy = this.ctrans.backward(x + 1, y + 1);
                String wkt = String.format("POLYGON ((%f %f, %f %f, %f %f, %f %f, %f %f))", minxy[0], maxxy[1], minxy[0], minxy[1], maxxy[0], minxy[1], maxxy[0], maxxy[1], minxy[0], maxxy[1]);
                Polygon g = (Polygon) new WKTReader().read(wkt);
                g.setSRID(4326);
                Envelope envelope = g.getEnvelopeInternal();
                com.github.davidmoten.rtree.geometry.Rectangle rectangle = Geometries.rectangleGeographic(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());

                //空间查询
                AtomicBoolean found = new AtomicBoolean(false);
                Observable<Entry<String, com.github.davidmoten.rtree.geometry.Geometry>> results = tree.search(rectangle);

                results.forEach(entity -> {
                    String feature_id = entity.value();
                    row.add(feature_id);

                    LocationsWithShape locationsWithShape = data.stream().filter(a -> a.getId().equalsIgnoreCase(feature_id)).findAny().get();
                    Map<String, Object> attrs = new HashMap<>();
                    attrs.put("id", locationsWithShape.getId());
                    this.grid.getFeature_cache().put(feature_id, attrs);
                    found.set(true);
                });
                if (!found.get())
                    row.add("");
            }
            this.grid.getRows().add(row);
        }
    }

    /**
     * Skip the codepoints that cannot be encoded directly in JSON.
     *
     * @return
     */
    public static int escape_codepoints(int codepoint) {
        if (codepoint == 34)
            codepoint += 1;
        else if (codepoint == 92)
            codepoint += 1;
        return codepoint;
    }

    public static int decode_id(int codepoint) {
//        codepoint = ord(codepoint);
        if (codepoint >= 93)
            codepoint -= 1;
        if (codepoint >= 35)
            codepoint -= 1;
        codepoint -= 32;
        return codepoint;
    }
}
