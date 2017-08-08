package com.cszt0.opensource.codeview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Looper;
import java.util.ArrayList;

final class LineSource implements CharSequence
{
	private String raw;
	private SpanMap span;
	private SpanMap lighing;

	LineSource(String raw) {
		this.raw = raw;
		span = new SpanMap();
		lighing = new SpanMap();
	}

	public void delete(int startPosition, int endPosition) {
		raw = raw.substring(0, startPosition) + raw.substring(endPosition);
		span.removeSpan(startPosition, endPosition);
	}

	public void copyLightSpan() {
		span = lighing;
		lighing = new SpanMap();
	}

	@Override
	public int length() {
		return raw.length();
	}

	@Override
	public char charAt(int p1) {
		return raw.charAt(p1);
	}

	@Override
	public String subSequence(int p1, int p2) {
		return raw.substring(p1, p2);
	}
	@Override
	public String toString() {
		return raw;
	}

	public LineSource append(CharSequence p1) {
		raw = raw + p1;
		return this;
	}

	public LineSource append(char p1) {
		raw = raw + p1;
		return this;
	}

	public LineSource appendAt(CharSequence text, int position) {
		raw = raw.substring(0, position) + text + raw.substring(position);
		span.updateSpan(position, text.length());
		return this;
	}

	public LineSource deleteAt(int position) {
		raw = raw.substring(0, position) + (raw.length() > position ?raw.substring(position + 1): "");
		span.updateSpan(position, -1);
		return this;
	}

	public synchronized void draw(Canvas canvas, float x, float y, Paint paint) {
		Paint.FontMetrics fontMetrics = paint.getFontMetrics();
		float height = fontMetrics.descent - fontMetrics.ascent;
		float baseline = height * 3 / 4;
		try {
			Paint colorPaint = new Paint(paint);
			ArrayList<Span> spans = span.entry(length());
			float xOffset = 0;
			for (Span span:spans) {
				int fontstyle = Typeface.NORMAL;
				if (span.isBold()) {
					fontstyle |= Typeface.BOLD;
				}
				if (span.isItalic()) {
					fontstyle |= Typeface.ITALIC;
				}
				String substring = raw.substring(span.start(), span.end());
				if (span.isColorEnable()) {
					colorPaint.setColor(span.getColor());
					colorPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, fontstyle));
					canvas.drawText(substring, x + xOffset, y + baseline, colorPaint);
					xOffset += colorPaint.measureText(substring);
				} else {
					paint.setTypeface(Typeface.create(Typeface.MONOSPACE, fontstyle));
					canvas.drawText(substring, x + xOffset, y + baseline, paint);
					xOffset += paint.measureText(substring);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			canvas.drawText(raw, x, y + baseline, paint);
		}
	}

	public void setSpan(Span span) {
		if (Looper.myLooper() == null) {
			lighing.addSpan(span);
		} else {
			this.span.addSpan(span);
		}
	}

}
