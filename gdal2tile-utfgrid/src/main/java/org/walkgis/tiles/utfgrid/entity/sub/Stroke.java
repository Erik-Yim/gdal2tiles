package org.walkgis.tiles.utfgrid.entity.sub;


import org.walkgis.tiles.utfgrid.entity.sub.penum.LineCap;
import org.walkgis.tiles.utfgrid.entity.sub.penum.LineJoin;

/**
 * @author JerFer
 * @date 2019/2/26---10:55
 */
public class Stroke {
    private String color = "#0000ff";
    private LineCap lineCap = LineCap.Round;
    private LineJoin lineJoin = LineJoin.Round;
    private float[] lineDash;

    private Integer lineDashOffset = 0;
    private Float miterLimit = 10.0f;
    private Float width = 3.0f;

    public Stroke() {
    }

    public Stroke(String color, LineCap lineCap, LineJoin lineJoin, float[] lineDash, Integer lineDashOffset, Float miterLimit, Float width) {
        this.color = color;
        this.lineCap = lineCap;
        this.lineJoin = lineJoin;
        this.lineDash = lineDash;
        this.lineDashOffset = lineDashOffset;
        this.miterLimit = miterLimit;
        this.width = width;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LineCap getLineCap() {
        return lineCap;
    }

    public void setLineCap(LineCap lineCap) {
        this.lineCap = lineCap;
    }

    public LineJoin getLineJoin() {
        return lineJoin;
    }

    public void setLineJoin(LineJoin lineJoin) {
        this.lineJoin = lineJoin;
    }

    public float[] getLineDash() {
        return lineDash;
    }

    public void setLineDash(float[] lineDash) {
        this.lineDash = lineDash;
    }

    public Integer getLineDashOffset() {
        return lineDashOffset;
    }

    public void setLineDashOffset(Integer lineDashOffset) {
        this.lineDashOffset = lineDashOffset;
    }

    public Float getMiterLimit() {
        return miterLimit;
    }

    public void setMiterLimit(Float miterLimit) {
        this.miterLimit = miterLimit;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }
}
