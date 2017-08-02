package com.cszt0.opensource.codeview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

final class SpanMap
{
	private ArrayList<Span> spans;

	public SpanMap() {
		spans = new ArrayList<>();
	}

	public void removeSpan(int startPosition, int endPosition) {
		int len = endPosition - startPosition;
		for (Span s:spans) {
			int start = s.start();
			int end = s.end();
			if (start > endPosition) {
				start -= len;
				end -= len;
				s.setPosition(start, end);
			} else if (start > startPosition) {
				start = startPosition;
				if (end < endPosition) {
					end = startPosition;
				} else {
					end -= len;
				}
				s.setPosition(start, end);
			}
		}
	}

	public void updateSpan(int position, int what) {
		for (Span s:spans) {
			int start = s.start();
			int end = s.end();
			if (start >= position) {
				start += what;
			}
			if (end >= position) {
				end += what;
			}
			s.setPosition(start, end);
		}
	}

	public ArrayList<Span> entry(int len) {
		ArrayList<Span> res = new ArrayList<>(spans.size() * 2);
		int current = 0;
		int size = spans.size();
		for (int i = 0;i < size;i++) {
			Span span = spans.get(i);
			int start = span.start();
			if (start == current) {
				res.add(span);
			} else {
				res.add(new Span(current, start));
				res.add(span);
			}
			current = span.end();
		}
		if (current != len) {
			res.add(new Span(current, len));
		}
		return res;
	}

	public void addSpan(Span span) {
		int start = span.start();
		int end = span.end();
		int len = spans.size();
		for (int i=0;i < len;i++) {
			Span s = spans.get(i);
			int s_start = s.start();
			int s_end = s.end();
			// 存在交叉部分，分离
			if (start < s_end && end > s_start) {
				spans.remove(i);
				if (start > s_start) {
					Span clone = new Span(s);
					clone.setPosition(s_start, start);
					spans.add(clone);
				}
				if (end < s_end) {
					Span clone = new Span(s);
					clone.setPosition(end, s_end);
					spans.add(clone);
				}
				break;
			}
		}
		spans.add(span);
		sortSpan();
	}

	private void sortSpan() {
		Collections.sort(spans, new Comparator<Span>() {
				@Override
				public int compare(Span p1, Span p2) {
					return p1.start() - p2.start();
				}
			});
	}
}
