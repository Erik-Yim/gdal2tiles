package org.walkgis.tiles.utfgrid.entity;


import org.walkgis.tiles.utfgrid.entity.sub.Circle;
import org.walkgis.tiles.utfgrid.entity.sub.Fill;
import org.walkgis.tiles.utfgrid.entity.sub.Stroke;
import org.walkgis.tiles.utfgrid.entity.sub.Text;


/**
 * @author JerFer
 * @date 2019/2/26---10:51
 */
public class Style {
    private Fill fill = new Fill();
    private Circle image;
    private Stroke stroke = new Stroke();
    private Text text;
    private Integer zIndex;

    public Style() {

    }

    public Style(Fill fill, Circle image, Stroke stroke, Text text, Integer zIndex) {
        this.fill = fill;
        this.image = image;
        this.stroke = stroke;
        this.text = text;
        this.zIndex = zIndex;
    }

    public Fill getFill() {
        return fill;
    }

    public void setFill(Fill fill) {
        this.fill = fill;
    }

    public Circle getImage() {
        return image;
    }

    public void setImage(Circle image) {
        this.image = image;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Integer getzIndex() {
        return zIndex;
    }

    public void setzIndex(Integer zIndex) {
        this.zIndex = zIndex;
    }
}
