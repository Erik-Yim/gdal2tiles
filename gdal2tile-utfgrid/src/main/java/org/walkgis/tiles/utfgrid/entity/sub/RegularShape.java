package org.walkgis.tiles.utfgrid.entity.sub;


import org.walkgis.tiles.utfgrid.entity.sub.base.Image;

/**
 * @author JerFer
 * @date 2019/4/4---15:19
 */
public class RegularShape extends Image {
    private Fill fill;
    private int points;
    private Integer radius;
    private float radius1;
    private float radius2;
    private float angle;
    private boolean snapToPixel = true;
    private Stroke stroke;
    private float rotation = 0;

    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        this.fill = fill;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public float getRadius1() {
        return radius1;
    }

    public void setRadius1(float radius1) {
        this.radius1 = radius1;
    }

    public float getRadius2() {
        return radius2;
    }

    public void setRadius2(float radius2) {
        this.radius2 = radius2;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public boolean isSnapToPixel() {
        return snapToPixel;
    }

    public void setSnapToPixel(boolean snapToPixel) {
        this.snapToPixel = snapToPixel;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
