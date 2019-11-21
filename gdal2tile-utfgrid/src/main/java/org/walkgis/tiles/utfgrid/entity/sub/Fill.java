package org.walkgis.tiles.utfgrid.entity.sub;


/**
 * @author JerFer
 * @date 2019/2/26---10:55
 */
public class Fill {
    private String color = "#0000ff";

    public Fill() {
    }

    public Fill(String color) {
        this.color = color;
    }


    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
