package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.List;

import org.emdev.utils.MathUtils;

public class Page {

    static final LogContext LCTX = LogContext.ROOT.lctx("Page", true);

    public final PageIndex index;
    public final PageType type;

    final IActivityController base;
    final PageTree nodes;

    RectF bounds;
    float aspectRatio;
    boolean recycled;
    float storedZoom;
    RectF zoomedBounds;

    int zoomLevel = 1;

    public Page(final IActivityController base, final PageIndex index, final PageType pt, final CodecPageInfo cpi) {
        this.base = base;
        this.index = index;
        this.type = pt != null ? pt : PageType.FULL_PAGE;
        this.bounds = new RectF(0, 0, cpi.width / type.getWidthScale(), cpi.height);

        setAspectRatio(cpi);

        nodes = new PageTree(this);
    }

    public void recycle(List<Bitmaps> bitmapsToRecycle) {
        recycled = true;
        nodes.recycleAll(bitmapsToRecycle, true);
    }

    public boolean draw(final Canvas canvas, final ViewState viewState) {
        return draw(canvas, viewState, false);
    }

    public boolean draw(final Canvas canvas, final ViewState viewState, final boolean drawInvisible) {
        if (drawInvisible || viewState.isPageVisible(this)) {
            final PagePaint paint = viewState.nightMode ? PagePaint.NIGHT : PagePaint.DAY;

            final RectF nodesBounds = viewState.getBounds(this);
            final RectF bounds = viewState.view.getAdjustedPageBounds(viewState, nodesBounds);

            if (!nodes.root.holder.hasBitmaps()) {
                canvas.drawRect(bounds, paint.fillPaint);

                final TextPaint textPaint = paint.textPaint;
                textPaint.setTextSize(24 * base.getZoomModel().getZoom());
                canvas.drawText(base.getContext().getString(R.string.text_page) + " " + (index.viewIndex + 1),
                        bounds.centerX(), bounds.centerY(), textPaint);
            }
            nodes.root.draw(canvas, viewState, nodesBounds, paint);

            return true;
        }
        return false;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    private boolean setAspectRatio(final float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            return true;
        }
        return false;
    }

    public boolean setAspectRatio(final CodecPageInfo page) {
        if (page != null) {
            return this.setAspectRatio(page.width / type.getWidthScale(), page.height);
        }
        return false;
    }

    public boolean setAspectRatio(final float width, final float height) {
        return setAspectRatio(width / height);
    }

    public void setBounds(final RectF pageBounds) {
        storedZoom = 0.0f;
        zoomedBounds = null;
        bounds = pageBounds;
    }

    public RectF getBounds(final float zoom) {
        if (zoom != storedZoom) {
            storedZoom = zoom;
            zoomedBounds = MathUtils.zoom(bounds, zoom);
        }
        return zoomedBounds;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("Page");
        buf.append("[");

        buf.append("index").append("=").append(index);
        buf.append(", ");
        buf.append("bounds").append("=").append(bounds);
        buf.append(", ");
        buf.append("aspectRatio").append("=").append(aspectRatio);
        buf.append(", ");
        buf.append("type").append("=").append(type.name());
        buf.append("]");
        return buf.toString();
    }

    public float getTargetRectScale() {
        return type.getWidthScale();
    }

    public float getTargetTranslate() {
        return type.getLeftPos();
    }

    public RectF getCroppedRegion() {
        return nodes.root.croppedBounds;
    }

}
