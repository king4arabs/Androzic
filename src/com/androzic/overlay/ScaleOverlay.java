/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012  Andrey Novikov <http://andreynovikov.info/>
 *
 * This file is part of Androzic application.
 *
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Androzic.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic.overlay;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.androzic.MapView;
import com.androzic.R;
import com.androzic.map.Map;
import com.androzic.util.StringFormatter;

public class ScaleOverlay extends MapOverlay
{
	private static final int SCALE_MOVE_DELAY = 2 * 1000000000;

	private Paint linePaint;
	private Paint textPaint;
	private Paint fillPaint;
	private boolean drawBackground;
	private double mpp;
	private long lastScaleMove;
	private int lastScalePos;

	public ScaleOverlay()
	{
		super();
		
		Resources resources = application.getResources();

		linePaint = new Paint();
        linePaint.setAntiAlias(false);
        linePaint.setStrokeWidth(2);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(resources.getColor(R.color.scalebar));
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStrokeWidth(2);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setTextSize(16);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setColor(resources.getColor(R.color.scalebar));
		fillPaint = new Paint();
		fillPaint.setAntiAlias(false);
		fillPaint.setStrokeWidth(1);
		fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		fillPaint.setColor(resources.getColor(R.color.scalebarbg));

		drawBackground = true;
		
    	mpp = 0;
    	lastScaleMove = 0;
    	lastScalePos = 1;
    	onPreferencesChanged(PreferenceManager.getDefaultSharedPreferences(application));
    	enabled = true;
	}

	@Override
	public synchronized void onMapChanged()
	{
    	Map map = application.getCurrentMap();
    	if (map == null)
    		return;
    	mpp = map.mpp / map.getZoom();
	}
	
	@Override
	public void onPrepareBuffer(final MapView.Viewport viewport, final Canvas c)
	{
		if (mpp == 0)
			return;
		
		int m = (int) (mpp * viewport.width / 6);
		if (m < 40)
			m = m / 10 * 10;
		else if (m < 80)
			m = 50;
		else if (m < 130)
			m = 100;
		else if (m < 300)
			m = 200;
		else if (m < 700)
			m = 500;
		else if (m < 900)
			m = 800;
		else if (m < 1300)
			m = 1000;
		else if (m < 3000)
			m = 2000;
		else if (m < 7000)
			m = 5000;
		else if (m < 10000)
			m = 8000;
		else if (m < 80000)
			m = (int) (Math.ceil(m * 1. / 10000) * 10000);
		else
			m = (int) (Math.ceil(m * 1. / 100000) * 100000);
		
		int x = (int) (m / mpp);
		
		if (x > viewport.width / 4)
		{
			x /= 2;
			m /= 2;
		}
		
		final int x2 = x * 2;
		final int x3 = x * 3;
		final int xd2 = x / 2;
		final int xd4 = x / 4;
		
		int cx = - viewport.lookAheadXY[0] - viewport.width / 2;
		int cy = - viewport.lookAheadXY[1] - viewport.height / 2;
		int cty = -10;

		int pos;
		if (viewport.bearing >= 0 && viewport.bearing < 90)
			pos = 1;
		else if (viewport.bearing >= 90 && viewport.bearing < 180)
			pos = 2;
		else if (viewport.bearing >= 180 && viewport.bearing < 270)
			pos = 3;
		else
			pos = 4;

		if (pos != lastScalePos)
		{
			long now = System.nanoTime();
			if (lastScaleMove == 0)
			{
				pos = lastScalePos;
				lastScaleMove = now;
			}
			else if (now > lastScaleMove + SCALE_MOVE_DELAY)
			{
				lastScalePos = pos;
				lastScaleMove = 0;
			}
			else
			{
				pos = lastScalePos;
			}
		}

		if (pos == 1)
		{
			cx += 30;
			cy += viewport.viewArea.bottom - 30;
		}
		else if (pos == 2)
		{
			cx += 30;
			cy += viewport.viewArea.top + 10;
			cty = 30;
		}
		else if (pos == 3)
		{
			cx += viewport.viewArea.right - x3 - 40;
			cy += viewport.viewArea.top + 10;
			cty = 30;
		}
		else
		{
			cx += viewport.viewArea.right - x3 - 40;
			cy += viewport.viewArea.bottom - 30;
		}

		int t = 2000;
		if (m <= t && m * 2 > t)
			t = m * 3;
		String[] d = StringFormatter.distanceC(m, t);
		String d2 = StringFormatter.distanceH(m*2, t);
		
		if (drawBackground)
		{
			Rect rect = new Rect();
			textPaint.getTextBounds(d[0], 0, d[0].length(), rect);
			int htw = rect.width() / 2;
			textPaint.getTextBounds(d2, 0, d2.length(), rect);
			int httw = rect.width() / 2;
			int th = rect.height();
			int bt = cty > 0 ? cy : cy + cty - th;
			int bb = cty > 0 ? cy + cty : cy + 10;
			rect = new Rect(cx-htw, bt, cx+x3+httw, bb);
			rect.inset(-2, -2);
			c.drawRect(rect, fillPaint);
		}

		c.drawLine(cx, cy, cx+x3, cy, linePaint);
		c.drawLine(cx, cy+10, cx+x3, cy+10, linePaint);
		c.drawLine(cx, cy, cx, cy+10, linePaint);
		c.drawLine(cx+x3, cy, cx+x3, cy+10, linePaint);
		c.drawLine(cx+x, cy, cx+x, cy+10, linePaint);
		c.drawLine(cx+x2, cy, cx+x2, cy+10, linePaint);
		c.drawLine(cx+x, cy+5, cx+x2, cy+5, linePaint);
		c.drawLine(cx, cy+5, cx+xd4, cy+5, linePaint);
		c.drawLine(cx+xd2, cy+5, cx+xd2+xd4, cy+5, linePaint);
		c.drawLine(cx+xd4, cy, cx+xd4, cy+10, linePaint);
		c.drawLine(cx+xd2, cy, cx+xd2, cy+10, linePaint);
		c.drawLine(cx+xd2+xd4, cy, cx+xd2+xd4, cy+10, linePaint);

		c.drawText("0", cx+x, cy+cty, textPaint);
		c.drawText(d[0], cx+x2, cy+cty, textPaint);
		c.drawText(d[0], cx, cy+cty, textPaint);
		c.drawText(d2, cx+x3, cy+cty, textPaint);
	}

	@Override
	public void onPrepareBufferEx(final MapView.Viewport viewport, final Canvas c)
	{
	}

	@Override
	public void onPreferencesChanged(SharedPreferences settings)
	{
		Resources resources = application.getResources();
		drawBackground = settings.getBoolean(application.getString(R.string.pref_scalebarbg), resources.getBoolean(R.bool.def_scalebarbg));
		int color = settings.getInt(application.getString(R.string.pref_scalebarcolor), resources.getColor(R.color.scalebar));
		linePaint.setColor(color);
		textPaint.setColor(color);
		fillPaint.setColor(settings.getInt(application.getString(R.string.pref_scalebarbgcolor), resources.getColor(R.color.scalebarbg)));
	}
}
