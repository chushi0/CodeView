package com.cszt0.opensource.codeview;

import android.graphics.Paint;

public abstract class HighLightTask implements Runnable
{
	private CodeView cv;
	private boolean update;
	private Code currentCode;

	public void bindCodeView(CodeView cv) {
		this.cv = cv;
	}

	@Override
	public final void run() {
		while (true) {
			if (update) {
				try {
					update = false;
					currentCode = (Code) cv.getCode();
					doHighLight(currentCode.toString());
					Paint paint = cv.getPaint();
					currentCode.notifyUpdate(paint);
					cv.postInvalidate();
				} catch (IndexOutOfBoundsException e) {
					update = true;
				}
			}
		}
	}

	protected abstract void doHighLight(String code);

	protected void highlight(Span span) {
		currentCode.setSpan(new Span(span));
	}

	public void notifyUpdate() {
		update = true;
	}
}
