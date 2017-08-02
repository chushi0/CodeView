package com.cszt0.opensource.codeview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Arrays;

final class Code implements CharSequence
{
	private CodeView cv;
	private ArrayList<LineSource> lines;

	Code(CodeView cv) {
		this.cv = cv;
		lines = new ArrayList<>();
		lines.add(new LineSource(""));
	}

	private void newLine(CharSequence text, int position) {
		LineSource newLine = text instanceof LineSource ?(LineSource)text: new LineSource(text == null ?"": text.toString());
		if (position == -1) position = lines.size() - 1;
		lines.add(position, newLine);
		cv.onNewLine(newLine, position + 1);
	}

	public void delete(int start, int end) {
		CharSequence content = subSequence(start, end);
		int[] startPosition = getPosition(start);
		int[] endPosition = getPosition(end);
		if (startPosition[1] == endPosition[1]) {
			LineSource ls = lines.get(startPosition[1]);
			ls.delete(startPosition[0], endPosition[0]);
		} else {
			LineSource ls1 = lines.get(startPosition[1]);
			ls1.delete(startPosition[0], ls1.length());
			LineSource ls2 = lines.get(endPosition[1]);
			ls2.delete(0, endPosition[0]);
			ls1.append(ls2);
			for (int i = endPosition[1];i > startPosition[1];i--) {
				lines.remove(i);
			}
		}
		cv.onDelete(start, end - start, content);
	}

	public int getPosition(int[] pos) {
		int position = 0;
		for (int i=0;i < pos[1];i++) {
			position += lines.get(i).length() + 1;
		}
		return position + pos[0];
	}

	public int getLineCount() {
		return lines.size();
	}

	public float measureWidth(Paint paint) {
		float width = 0;
		for (LineSource ls:lines) {
			width = Math.max(width, paint.measureText(ls.toString()));
		}
		return width;
	}

	public int[] getPosition(int position) {
		int len = lines.size();
		for (int i=0;i < len;i++) {
			LineSource line = lines.get(i);
			int linelen = line.length();
			if (position > linelen) {
				position -= linelen + 1;
			} else {
				return new int[] {position, i};
			}
		}
		throw new IndexOutOfBoundsException("position=" + position + " length=" + length());
	}

	public synchronized void draw(Canvas canvas, float xScroll, float yScroll, int viewWidth, int viewHeight, Paint paint) {
		Paint.FontMetrics fontMetrics = paint.getFontMetrics();
		float height = fontMetrics.descent - fontMetrics.ascent;
		int len = lines.size();
		for (int i = 0;i < len;i++) {
			LineSource line = lines.get(i);
			float x = -xScroll;
			float y = -yScroll + height * i;
			if (y > viewHeight || y < -height) {
				continue;
			}
			line.draw(canvas, x, y, paint);
		}
	}

	public Code append(CharSequence p1) {
		String[] raw = p1.toString().split("\n", 0);
		int lineno = lines.size() - 1;
		LineSource end = lines.get(lineno);
		String oldLineSource = end.toString();
		end.append(raw[0]);
		String newLineSource = end.toString();
		cv.onLineChange(oldLineSource, newLineSource, lineno);
		int len = raw.length;
		for (int i=1;i < len;i++) {
			newLine(raw[i], -1);
		}
		return this;
	}

	public Code append(char p1) {
		if (p1 == '\n') {
			newLine(null, -1);
		} else {
			int lineno = lines.size() - 1;
			LineSource line = lines.get(lineno);
			String o = line.toString();
			line.append(p1);
			String n = line.toString();
			cv.onLineChange(o, n, lineno);
		}
		return this;
	}

	public Code appendAt(CharSequence text, int position) {
		int[] pos = getPosition(position);
		LineSource rawlineSource = lines.get(pos[1]);
		String[] raw = text.toString().split("\n", 0);
		if (raw.length == 1) {
			String o = rawlineSource.toString();
			rawlineSource.appendAt(text, pos[0]);
			String n = rawlineSource.toString();
			cv.onLineChange(o, n, pos[0]);
		} else {
			int length = raw.length;
			LineSource[] newLines = new LineSource[length];
			for (int i=0;i < length;i++) {
				newLines[i] = new LineSource(raw[i]);
			}
			newLines[0].appendAt(rawlineSource.subSequence(0, pos[0]), 0);
			newLines[length - 1].append(rawlineSource.subSequence(pos[0], rawlineSource.length()));
			lines.remove(pos[1]);
			lines.add(pos[1], newLines[0]);
			cv.onLineChange(rawlineSource, newLines[0], pos[1]);
			int len = newLines.length;
			for (int i=1;i < len;i++) {
				newLine(newLines[i], pos[1] + i);
			}
		}
		return this;
	}

	public Code appendAt(char c, int position) {
		int[] pos = getPosition(position);
		LineSource rawLineSource = lines.get(pos[1]);
		if (c == '\n') {
			LineSource s1 = new LineSource(rawLineSource.subSequence(0, pos[0]).toString());
			LineSource s2 = new LineSource(rawLineSource.subSequence(pos[0], rawLineSource.length()).toString());
			lines.remove(pos[1]);
			lines.add(pos[1], s2);
			cv.onLineChange(rawLineSource, s2, pos[1]);
			newLine(s1, pos[1]);
		} else {
			String o = rawLineSource.toString();
			rawLineSource.appendAt(c + "", pos[0]);
			cv.onLineChange(o, rawLineSource, pos[1]);
		}
		return this;
	}

	public Code deleteAt(int position) {
		int[] pos = getPosition(position);
		if (charAt(position) == '\n') {
			LineSource s1 = lines.get(pos[1]);
			LineSource s2 = lines.get(pos[1] + 1);
			s1.append(s2);
			lines.remove(pos[1] + 1);
			cv.onDelete(position, 1, "\n");
		} else {
			LineSource lineSource = lines.get(pos[1]);
			String c = String.valueOf(lineSource.charAt(pos[0]));
			lineSource.deleteAt(pos[0]);
			cv.onDelete(position, 1, c);
		}
		return this;
	}

	@Override
	public int length() {
		int len = -1;
		for (LineSource line:lines) {
			len += line.length() + 1;
		}
		return len;
	}

	@Override
	public char charAt(int p1) {
		int len = 0;
		int next;
		for (LineSource line:lines) {
			next = len + line.length();
			if (p1 >= len && p1 < next) {
				return line.charAt(p1 - len);
			}
			if (p1 == next) {
				return '\n';
			}
			len += line.length() + 1;
		}
		throw new StringIndexOutOfBoundsException(String.format("index=%d length=%d", p1, length()));
	}

	@Override
	public CharSequence subSequence(int p1, int p2) {
		int[] pos1 = getPosition(p1);
		int[] pos2 = getPosition(p2);
		if (pos1[1] == pos2[1]) {
			return lines.get(pos1[1]).subSequence(pos1[0], pos2[0]);
		}
		StringBuilder builder = new StringBuilder();
		LineSource ls1 = lines.get(pos1[1]);
		builder.append(ls1.subSequence(pos1[0], ls1.length()));
		builder.append("\n");
		for (int i=pos1[1] + 1;i < pos2[1];i++) {
			builder.append(lines.get(i));
			builder.append("\n");
		}
		builder.append(lines.get(pos2[1]).subSequence(0, pos2[0]));
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(length());
		for (LineSource line:lines) {
			builder.append(line);
			builder.append("\n");
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}

	public LineSource getLineSource(int lineno) {
		return lines.get(lineno);
	}

	public void notifyUpdate(Paint paint) {
		for (LineSource line:lines) {
			line.copyLightSpan();
		}
	}

	public void setSpan(Span span) {
		int start = span.start();
		int end = span.end();
		int[] pos_start = getPosition(start);
		int[] pos_end = getPosition(end);
		if (pos_start[1] == pos_end[1]) {
			span.setPosition(pos_start[0], pos_end[0]);
			lines.get(pos_start[1]).setSpan(span);
		} else {
			LineSource ls1 = lines.get(pos_start[1]);
			Span s1 = new Span(span);
			s1.setPosition(pos_start[0], ls1.length());
			ls1.setSpan(s1);
			LineSource ls2 = lines.get(pos_end[1]);
			Span s2 = new Span(span);
			s2.setPosition(0, pos_end[0]);
			ls2.setSpan(s2);
			for (int i = pos_start[1] + 1;i < pos_end[1];i++) {
				LineSource line = lines.get(i);
				Span s = new Span(span);
				s.setPosition(0, line.length());
				line.setSpan(s);
			}
		}
	}
}
