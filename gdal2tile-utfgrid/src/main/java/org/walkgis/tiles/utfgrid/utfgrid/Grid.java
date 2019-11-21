package org.walkgis.tiles.utfgrid.utfgrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JerFer
 * @date 2019/4/9---13:02
 */
public class Grid {
    private double resolution = 4.0;
    private List<List<String>> rows;
    private Map<String, Map<String, Object>> feature_cache;

    public Grid() {
        this.rows = new ArrayList<>();
        this.feature_cache = new HashMap();
        this.resolution = 4.0;
    }

    public Grid(double resolution) {
        this.rows = new ArrayList<>();
        this.feature_cache = new HashMap();
        this.resolution = resolution;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public Map<String, Map<String, Object>> getFeature_cache() {
        return feature_cache;
    }

    public int width() {
        return rows.get(0).size();
    }

    public int height() {
        return rows.size();
    }

    public Map<String, Object> encode() {
        Map<String, Integer> keys = new HashMap();
        List<String> key_order = new ArrayList<>();
        Map data = new HashMap();
        List<String> utf_rows = new ArrayList<>();
        int codepoint = 32;
        for (int y = 0, length = height(); y < length; y++) {
            String row_utf = "";
            List<String> row = this.rows.get(y);
            for (int x = 0, length2 = width(); x < length2; x++) {
                String feature_id = row.get(x);
                if (keys.containsKey(feature_id))
                    row_utf += (char) keys.get(feature_id).intValue();
                else {
                    codepoint = Renderer.escape_codepoints(codepoint);
                    keys.put(feature_id, codepoint);
                    key_order.add(feature_id);
                    if (this.feature_cache.get(feature_id) != null)
                        data.put(feature_id, this.feature_cache.get(feature_id));
                    row_utf += (char) codepoint;
                    codepoint += 1;
                }
            }
            utf_rows.add(row_utf);
        }

        Map<String, Object> utf = new HashMap();
        utf.put("grid", utf_rows);
        utf.put("keys", key_order);
        utf.put("data", data);
        System.out.println(utf);

        return utf;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }
}
