package org.walkgis.tiles.utfgrid.entity.sub.penum;

/**
 * @author JerFer
 * @date 2019/4/9---10:46
 */
public enum IconOrigin {
    BOTTOMLEFT("bottom-left"), BOTTOMRIGHT("bottom-right"), TOPLEFT("top-left"), TOPRIGHT("top-right"),;
    private String value;

    IconOrigin(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
