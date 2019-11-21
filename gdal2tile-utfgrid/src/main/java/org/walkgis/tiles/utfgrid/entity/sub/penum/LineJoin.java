package org.walkgis.tiles.utfgrid.entity.sub.penum;

/**
 * @author JerFer
 * @date 2019/2/26---11:01
 */
public enum LineJoin {
    Bevel(2), Round(1), Miter(0);
    private Integer value;

    LineJoin(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
