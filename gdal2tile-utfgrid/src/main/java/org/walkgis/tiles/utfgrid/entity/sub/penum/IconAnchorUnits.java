package org.walkgis.tiles.utfgrid.entity.sub.penum;

/**
 * @author JerFer
 * @date 2019/4/9---10:46
 */
public enum IconAnchorUnits {
    fraction("fraction"), pixels("pixels");
    private String value;

    IconAnchorUnits(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
