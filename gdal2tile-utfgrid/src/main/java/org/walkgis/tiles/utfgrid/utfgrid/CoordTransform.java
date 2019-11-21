package org.walkgis.tiles.utfgrid.utfgrid;

/**
 * @author JerFer
 * @date 2019/4/9---11:47
 */
public class CoordTransform {
    private Request request;
    private Extent extent;
    private Double offset_x;
    private Double offset_y;
    private Double sx;
    private Double sy;

    public CoordTransform() {
    }


    public CoordTransform(Request request) {
        this.request = request;
        this.extent = request.getExtent();
        this.offset_x = 0.0;
        this.offset_y = 0.0;
        this.sx = request.getWidth() / this.extent.width();
        this.sy = request.getHeight() / this.extent.height();
    }

    public CoordTransform(Request request, double offset_x, double offset_y) {
        this.request = request;
        this.extent = request.getExtent();
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        this.sx = request.getWidth() / this.extent.width();
        this.sy = request.getHeight() / this.extent.height();
    }

    /**
     * Lon/Lat to pixmap
     *
     * @param x
     * @param y
     * @return
     */
    public double[] forward(double x, double y) {
        return new double[]{
                (x - this.extent.getMinx()) * this.sx - this.offset_x,
                (this.extent.getMaxy() - y) * this.sy - this.offset_y
        };
    }

    /**
     * Pixmap to Lon/Lat
     *
     * @param x
     * @param y
     * @return
     */
    public double[] backward(int x, int y) {
        return new double[]{
                this.extent.getMinx() + (x + this.offset_x) / this.sx,
                this.extent.getMaxy() - (y + this.offset_y) / this.sy
        };
    }


    public Request getRequest() {
        return request;
    }
}
