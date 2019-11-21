package org.walkgis.tiles.utfgrid.entity.sub;

import cn.com.enersun.dgpmicro.problem.entity.sub.base.Image;
import cn.com.enersun.dgpmicro.problem.entity.sub.penum.IconAnchorUnits;
import cn.com.enersun.dgpmicro.problem.entity.sub.penum.IconOrigin;

/**
 * @author JerFer
 * @date 2019/2/26---10:55
 */
public class Icon extends Image {
    private float[] anchor = new float[]{0.5f, 0.5f};
    private IconOrigin anchorOrigin = IconOrigin.TOPLEFT;
    private IconAnchorUnits anchorXUnits = IconAnchorUnits.fraction;
    private IconAnchorUnits anchorYUnits = IconAnchorUnits.fraction;
    private String color = "#0000ff";
    private float[] offset = new float[]{0.0f, 0.0f};
    private IconOrigin offsetOrigin = IconOrigin.TOPLEFT;
    private float opacity = 1.0f;
    private float scale = 1.0f;
    private boolean snapToPixel = true;
    private float rotation = 0.0f;
    private float[] size;
    private float[] imageSize;
    private String src;

    public float[] getAnchor() {
        return anchor;
    }

    public void setAnchor(float[] anchor) {
        this.anchor = anchor;
    }

    public IconOrigin getAnchorOrigin() {
        return anchorOrigin;
    }

    public void setAnchorOrigin(IconOrigin anchorOrigin) {
        this.anchorOrigin = anchorOrigin;
    }

    public IconAnchorUnits getAnchorXUnits() {
        return anchorXUnits;
    }

    public void setAnchorXUnits(IconAnchorUnits anchorXUnits) {
        this.anchorXUnits = anchorXUnits;
    }

    public IconAnchorUnits getAnchorYUnits() {
        return anchorYUnits;
    }

    public void setAnchorYUnits(IconAnchorUnits anchorYUnits) {
        this.anchorYUnits = anchorYUnits;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public float[] getOffset() {
        return offset;
    }

    public void setOffset(float[] offset) {
        this.offset = offset;
    }

    public IconOrigin getOffsetOrigin() {
        return offsetOrigin;
    }

    public void setOffsetOrigin(IconOrigin offsetOrigin) {
        this.offsetOrigin = offsetOrigin;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public boolean isSnapToPixel() {
        return snapToPixel;
    }

    public void setSnapToPixel(boolean snapToPixel) {
        this.snapToPixel = snapToPixel;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float[] getSize() {
        return size;
    }

    public void setSize(float[] size) {
        this.size = size;
    }

    public float[] getImageSize() {
        return imageSize;
    }

    public void setImageSize(float[] imageSize) {
        this.imageSize = imageSize;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
