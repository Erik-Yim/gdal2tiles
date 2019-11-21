package org.walkgis.tiles.utfgrid.entity.sub.penum;

/**
 * @author JerFer
 * @date 2019/2/26---11:01
 */
public enum LineCap {
    Butt(0), Round(1), Square(2);
    private Integer value;

    LineCap(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
