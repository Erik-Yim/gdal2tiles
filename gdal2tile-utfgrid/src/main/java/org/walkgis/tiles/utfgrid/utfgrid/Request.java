package org.walkgis.tiles.utfgrid.utfgrid;

/**
 * @author JerFer
 * @date 2019/4/9---11:42
 */
public class Request {
    private int width;
    private int height;
    private Extent extent;

    public Request() {
    }

    public Request(int width, int height, Extent extent) {
        this.width = width;
        this.height = height;
        this.extent = extent;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Extent getExtent() {
        return extent;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }
}
