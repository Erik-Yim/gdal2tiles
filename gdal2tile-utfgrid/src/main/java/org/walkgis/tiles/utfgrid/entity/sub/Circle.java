package org.walkgis.tiles.utfgrid.entity.sub;

/**
 * @author JerFer
 * @date 2019/4/9---10:59
 */
public class Circle extends RegularShape {
    private Fill fill;
    private Integer radius;
    private boolean snapToPixel;
    private Stroke stroke;

    @Override
    public Fill getFill() {
        return fill;
    }

    @Override
    public void setFill(Fill fill) {
        this.fill = fill;
    }

    @Override
    public Integer getRadius() {
        return radius;
    }

    @Override
    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    @Override
    public boolean isSnapToPixel() {
        return snapToPixel;
    }

    @Override
    public void setSnapToPixel(boolean snapToPixel) {
        this.snapToPixel = snapToPixel;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }
}
