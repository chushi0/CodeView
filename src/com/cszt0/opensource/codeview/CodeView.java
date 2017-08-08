package com.cszt0.opensource.codeview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import java.util.Timer;
import java.util.TimerTask;
import android.content.res.TypedArray;

public class CodeView extends View
{
	private static final int DEFAULT_TEXT_COLOR = 0xff000000;
	private static final int DEFAULT_TEXT_SIZE = 35;
	private static final int DEFAULT_TEXT_SELECTION_BACKGROUND_COLOR = 0xac1010fb;
	private static final int DEFAULT_CURSOR_WIDTH = 3;
	private static final int DEFAULT_CURSOR_COLOR = 0xff1010aa;
	private static final int DEFAULT_LINE_NUMBER_COLOR = 0x802b2b2b;

	private int textColor;
	private int textSelectionBackgroundColor;
	private int cursorWidth;
	private int cursorColor;
	private int lineNumberColor;

	private Paint mPaint;
	private InputMethodManager imm;
	private Code mCode;
	private int cursorPosition;
	private Thread highLightThread;
	private HighLightTask highLightTask;
	private TimerTask flashCursorTask;
	private Timer flashCursor;
	private boolean isCursorShowing;

	private float xScroll, yScroll;
	private float xMaxScroll, yMaxScroll;
	private float downX, downY;
	private float lastXScroll, lastYScroll;
	private float vX, vY;
	private boolean responseKeyboard;
	private boolean isTouching;

	private boolean isSelection;
	private boolean isSelectioning;
	private int selectionStart;
	private int selectionEnd;

	public CodeView(Context context) {
		this(context, null);
	}

	public CodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setTypeface(Typeface.MONOSPACE);
		flashCursorTask = new TimerTask() {
			@Override
			public void run() {
				isCursorShowing = !isCursorShowing;
				postInvalidate();
			}
		};
		flashCursor = new Timer();
		imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		mCode = new Code();
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.cszt0_CodeView);
		mPaint.setTextSize(a.getDimension(R.styleable.cszt0_CodeView_textSize, DEFAULT_TEXT_SIZE));
		textColor = a.getColor(R.styleable.cszt0_CodeView_textColor, DEFAULT_TEXT_COLOR);
		textSelectionBackgroundColor = a.getColor(R.styleable.cszt0_CodeView_textSelectionBackgroundColor, DEFAULT_TEXT_SELECTION_BACKGROUND_COLOR);
		cursorWidth = a.getInteger(R.styleable.cszt0_CodeView_cursorWidth, DEFAULT_CURSOR_WIDTH);
		cursorColor = a.getColor(R.styleable.cszt0_CodeView_cursorColor, DEFAULT_CURSOR_COLOR);
		lineNumberColor = a.getColor(R.styleable.cszt0_CodeView_lineNumberColor, DEFAULT_LINE_NUMBER_COLOR);
		String defaultText = a.getString(R.styleable.cszt0_CodeView_text);
		if (defaultText != null) {
			mCode.append(defaultText);
		}
		String highlightClassName = a.getString(R.styleable.cszt0_CodeView_highlightClass);
		if (highlightClassName != null && highlightClassName.length() > 0) {
			try {
				Class highlightClass = Class.forName(highlightClassName);
				Object o = highlightClass.newInstance();
				setHighLightTask((HighLightTask) o);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Cannot find highlight class " + highlightClassName, e);
			} catch (InstantiationException e) {
				throw new RuntimeException("Cannot create instance of highlight class " + highlightClassName, e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Cannot access highlight class " + highlightClassName, e);
			}
		}
		a.recycle();
	}

	public void setHighLightTask(HighLightTask task) {
		if (highLightThread != null && highLightThread.isAlive()) {
			highLightThread.interrupt();
		}
		task.bindCodeView(this);
		highLightTask = task;
		highLightThread = new Thread(highLightTask);
		highLightThread.start();
		notifyUpdate();
	}

	public void stopHighLightTask() {
		highLightThread.interrupt();
	}

	public String getSelectionText() {
		if (!isSelection) {
			return "";
		}
		CharSequence s;
		if (selectionStart < selectionEnd) {
			s = mCode.subSequence(selectionStart, selectionEnd);
		} else {
			s = mCode.subSequence(selectionEnd, selectionStart);
		}
		return s.toString();
	}

	public void setCursorPosition(int position) {
		cursorPosition = position;
		checkCursorPosition();
		scrollToCursor();
		invalidate();
	}

	public void appendAt(String s, int position) {
		mCode.appendAt(s, position);
		if (cursorPosition > position) {
			cursorPosition += s.length();
			checkCursorPosition();
		}
		invalidate();
	}

	public void append(String s) {
		mCode.append(s);
		invalidate();
	}

	public void deleteAt(int position) {
		mCode.deleteAt(position);
		if (cursorPosition > position) {
			cursorPosition--;
			checkCursorPosition();
		}
		invalidate();
	}

	public void deleteAll(int startPosition, int endPosition) {
		delete(startPosition, endPosition);
		invalidate();
	}

	public int getLineCount() {
		return mCode.getLineCount();
	}

	public int currentLine() {
		return mCode.getPosition(cursorPosition)[1];
	}

	public int currentLineOffset() {
		return mCode.getPosition(cursorPosition)[0];
	}

	@Override
	public void scrollTo(int x, int y) {
		scrollTo((float)x, (float)y);
	}

	public void scrollTo(float x, float y) {
		xScroll = x;
		yScroll = y;
		checkScroll();
		invalidate();
	}

	@Override
	public void scrollBy(int x, int y) {
		scrollBy((float)x, (float)y);
	}

	public void scrollBy(float x, float y) {
		xScroll += x;
		yScroll += y;
		checkScroll();
		invalidate();
	}
	
	public float getXMaxScroll() {
		return xMaxScroll;
	}
	
	public float getYMaxScroll() {
		return yMaxScroll;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		xMaxScroll = mCode.measureWidth(mPaint);
		yMaxScroll = (mPaint.descent() - mPaint.ascent()) * mCode.getLineCount();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawLineNumber(canvas);
		float leftPadding = mPaint.measureText(String.valueOf(mCode.getLineCount()));
		canvas.save();
		canvas.translate(leftPadding, 0);
		drawSelection(canvas);
		drawText(canvas);
		drawCursor(canvas);
		canvas.restore();
	}

	private void drawSelection(Canvas canvas) {
		if (isSelection) {
			int[] startPosition = mCode.getPosition(Math.min(selectionStart, selectionEnd));
			int[] endPosition = mCode.getPosition(Math.max(selectionStart, selectionEnd));
			Paint paint = new Paint(mPaint);
			paint.setColor(textSelectionBackgroundColor);
			float height = paint.descent() - paint.ascent();
			if (startPosition[1] == endPosition[1]) {
				LineSource line = mCode.getLineSource(startPosition[1]);
				float startX = paint.measureText(line.subSequence(0, startPosition[0]).toString());
				float endX = paint.measureText(line.subSequence(0, endPosition[0]).toString());
				canvas.drawRect(-xScroll + startX, -yScroll + height * startPosition[1], -xScroll + endX, -yScroll + height * (startPosition[1] + 1), paint);
			} else {
				LineSource ls1 = mCode.getLineSource(startPosition[1]);
				canvas.drawRect(-xScroll + paint.measureText(ls1.subSequence(0, startPosition[0]).toString()), -yScroll + height * startPosition[1], -xScroll + paint.measureText(ls1.toString()), -yScroll + height * (startPosition[1] + 1), paint);
				LineSource ls2 = mCode.getLineSource(endPosition[1]);
				canvas.drawRect(-xScroll, -yScroll + height * endPosition[1], -xScroll + paint.measureText(ls2.subSequence(0, endPosition[0]).toString()), -yScroll + height * (endPosition[1] + 1), paint);
				int len = endPosition[1] - startPosition[1];
				int offset = startPosition[1];
				int h = getHeight();
				for (int i=1;i < len;i++) {
					float top = -yScroll + height * (offset + i);
					float bottom = -yScroll + height * (offset + i + 1);
					if (top > h || bottom < 0) {
						continue;
					}
					canvas.drawRect(-xScroll, top, -xScroll + paint.measureText(mCode.getLineSource(offset + i).toString()), bottom, paint);
				}
			}
		}
	}

	private void drawText(Canvas canvas) {
		mPaint.setColor(textColor);
		mCode.draw(canvas, xScroll, yScroll, getWidth(), getHeight(), mPaint);
	}

	private void drawLineNumber(Canvas canvas) {
		Paint paint = new Paint(mPaint);
		int count = mCode.getLineCount();
		float height = paint.descent() - paint.ascent();
		float baseline = height * 3 / 4;
		paint.setColor(lineNumberColor);
		for (int i=0;i < count;i++) {
			float x = -xScroll;
			float y = -yScroll + height * i;
			if (y > getHeight() || y < -height) {
				continue;
			}
			canvas.drawText(String.valueOf(i + 1), x, y + baseline, paint);
		}
		float leftPadding = paint.measureText(String.valueOf(count));
		canvas.drawLine(-xScroll + leftPadding, -yScroll, -xScroll + leftPadding, -yScroll + height * count, paint);
	}

	private void drawCursor(Canvas canvas) {
		if (isCursorShowing && isEnabled()) {
			Paint cursorPaint = new Paint(mPaint);
			cursorPaint.setColor(cursorColor);
			cursorPaint.setStrokeWidth(cursorWidth);
			int[] position = mCode.getPosition(cursorPosition);
			float width = mPaint.measureText(mCode.getLineSource(position[1]).subSequence(0, position[0]).toString());
			float height = mPaint.descent() - mPaint.ascent();
			canvas.drawLine(-xScroll + width, -yScroll + height * position[1], -xScroll + width, -yScroll + height * (position[1] + 1), cursorPaint);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!isEnabled()) {
			return super.onKeyDown(keyCode, event);
		}
		switch (keyCode) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				isSelectioning = true;
				if (!isSelection) {
					selectionStart = cursorPosition;
					selectionEnd = cursorPosition;
				}
				return true;
			case KeyEvent.KEYCODE_ENTER:
				deleteIfSelection();
				mCode.appendAt('\n', cursorPosition);
				cursorPosition++;
				notifyUpdate();
				scrollToCursor();
				invalidate();
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				cursorPosition--;
				checkCursorPosition();
				if (isSelectioning) {
					selectionEnd = cursorPosition;
					isSelection = true;
				} else {
					isSelection = false;
				}
				scrollToCursor();
				invalidate();
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				cursorPosition++;
				checkCursorPosition();
				if (isSelectioning) {
					selectionEnd = cursorPosition;
					isSelection = true;
				} else {
					isSelection = false;
				}
				scrollToCursor();
				invalidate();
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:{
					int[] pos = mCode.getPosition(cursorPosition);
					if (pos[1] == 0) {
						cursorPosition = 0;
						if (isSelectioning) {
							selectionEnd = cursorPosition;
							isSelection = true;
						} else {
							isSelection = false;
						}
						scrollToCursor();
						invalidate();
						return true;
					}
					pos[1]--;
					int len = mCode.getLineSource(pos[1]).length();
					pos[0] = Math.min(pos[0], len);
					cursorPosition = mCode.getPosition(pos);
					checkCursorPosition();
					if (isSelectioning) {
						selectionEnd = cursorPosition;
						isSelection = true;
					} else {
						isSelection = false;
					}
					scrollToCursor();
					invalidate();
					return true;
				}
			case KeyEvent.KEYCODE_DPAD_DOWN:{
					int[] pos = mCode.getPosition(cursorPosition);
					pos[1]++;
					if (pos[1] == mCode.getLineCount()) {
						cursorPosition = mCode.length();
						if (isSelectioning) {
							selectionEnd = cursorPosition;
							isSelection = true;
						} else {
							isSelection = false;
						}
						scrollToCursor();
						invalidate();
						return true;
					}
					int len = mCode.getLineSource(pos[1]).length();
					pos[0] = Math.min(pos[0], len);
					cursorPosition = mCode.getPosition(pos);
					checkCursorPosition();
					if (isSelectioning) {
						selectionEnd = cursorPosition;
						isSelection = true;
					} else {
						isSelection = false;
					}
					scrollToCursor();
					invalidate();
					return true;
				}
			case KeyEvent.KEYCODE_DEL:
				if (deleteIfSelection()) {
					return true;
				}
				if (cursorPosition == 0) {
					return true;
				}
				mCode.deleteAt(cursorPosition - 1);
				cursorPosition--;
				notifyUpdate();
				scrollToCursor();
				invalidate();
				return true;
			case KeyEvent.KEYCODE_TAB:
				return true;
		}
		if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
			deleteIfSelection();
			mCode.append(String.valueOf(keyCode - 7));
			cursorPosition++;
			notifyUpdate();
			scrollToCursor();
			invalidate();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				isSelectioning = false;
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void scrollToCursor() {
		int[] position = mCode.getPosition(cursorPosition);
		float x = mPaint.measureText(mCode.getLineSource(position[1]).subSequence(0, position[0]).toString());
		float height = mPaint.descent() - mPaint.ascent();
		float y = height * position[1];
		float leftPadding = mPaint.measureText(String.valueOf(mCode.getLineCount()));
		if (x < xScroll - leftPadding) {
			xScroll = x + leftPadding;
		}
		if (x > xScroll + getWidth() - leftPadding) {
			xScroll = x - getWidth() + leftPadding;
		}
		if (y < yScroll) {
			yScroll = y;
		}
		if (y > yScroll + getHeight() + height) {
			yScroll = y - getHeight() - height;
		}
		checkScroll();
	}

	private void checkCursorPosition() {
		if (cursorPosition < 0) {
			cursorPosition = 0;
		}
		if (cursorPosition > mCode.length()) {
			cursorPosition = mCode.length();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				downX = x;
				downY = y;
				responseKeyboard = true;
				lastXScroll = x;
				lastYScroll = y;
				vX = 0;
				vY = 0;
				isTouching = true;
				return true;
			case MotionEvent.ACTION_MOVE:
				float xOffset = lastXScroll - x;
				float yOffset = lastYScroll - y;
				xScroll += xOffset;
				yScroll += yOffset;
				checkScroll();
				if (Math.pow(downX - x, 2) + Math.pow(downY - y, 2) > 100) {
					responseKeyboard = false;
				}
				vX = lastXScroll - x;
				vY = lastYScroll - y;
				lastXScroll = x;
				lastYScroll = y;
				invalidate();
				return true;
			case MotionEvent.ACTION_UP:
				isTouching = false;
				if (responseKeyboard) {
					showKeyboard();
					isSelection = false;
					isSelectioning = false;
					float leftPadding = mPaint.measureText(String.valueOf(mCode.getLineCount()));
					float px = xScroll + x - leftPadding;
					float py = yScroll + y;
					float height = mPaint.descent() - mPaint.ascent();
					int line = (int) Math.floor(py / height);
					if (line < 0) line = 0;
					if (line >= mCode.getLineCount()) line = mCode.getLineCount() - 1;
					LineSource lineSource = mCode.getLineSource(line);
					int len = lineSource.length();
					float offset = px;
					int pos = 0;
					for (int i=0;i < len;i++) {
						float width = Math.abs(mPaint.measureText(lineSource.subSequence(0, i).toString()) - px);
						if (width < offset) {
							pos = i;
							offset = width;
						}
					}
					if (offset > mPaint.getTextSize()) {
						pos++;
					}
					cursorPosition = mCode.getPosition(new int[] {pos,line});
					scrollToCursor();
					invalidate();
					return true;
				}
				getHandler().post(new Runnable() {
						@Override
						public void run() {
							if (Math.abs(vX) >= 0.1 && Math.abs(vY) >= 0.1 && !isTouching && isShown()) {
								xScroll += vX;
								yScroll += vY;
								checkScroll();
								vX *= 0.98f;
								vY *= 0.98f;
								invalidate();
								getHandler().post(this);
							}
						}
					});
		}
		return true;
	}

	private void delete(int start, int end) {
		mCode.delete(start, end);
		cursorPosition = start;
		checkCursorPosition();
		scrollToCursor();
	}

	private boolean deleteIfSelection() {
		if (isSelection) {
			isSelection = false;
			if (selectionStart < selectionEnd) {
				delete(selectionStart, selectionEnd);
			} else {
				delete(selectionEnd, selectionStart);
			}
			return true;
		}
		return false;
	}

	private void checkScroll() {
		xScroll = Math.max(0, Math.min(xMaxScroll, xScroll));
		yScroll = Math.max(0, Math.min(yMaxScroll, yScroll));
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		showKeyboard();
		flashCursor.scheduleAtFixedRate(flashCursorTask, 0, 500);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		flashCursor.cancel();
	}

	private void notifyUpdate() {
		if (highLightTask != null) {
			highLightTask.notifyUpdate();
		}
	}

	private void showKeyboard() {
		setFocusableInTouchMode(true);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
	}

	@Override
	public boolean onCheckIsTextEditor() {
		return true;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
		outAttrs.inputType = InputType.TYPE_NULL;
		return new IInputConnection();
	}

	public Paint getPaint() {
		return mPaint;
	}

	public String getText() {
		return mCode.toString();
	}

	public CharSequence getCode() {
		return mCode;
	}

	public void setText(String code) {
		mCode = new Code();
		mCode.append(code);
		cursorPosition = 0;
		notifyUpdate();
		invalidate();
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}

	public int getTextColor() {
		return textColor;
	}

	public void setTextSelectionBackgroundColor(int textSelectionBackgroundColor) {
		this.textSelectionBackgroundColor = textSelectionBackgroundColor;
	}

	public int getTextSelectionBackgroundColor() {
		return textSelectionBackgroundColor;
	}

	public void setCursorWidth(int cursorWidth) {
		this.cursorWidth = cursorWidth;
	}

	public int getCursorWidth() {
		return cursorWidth;
	}

	public void setCursorColor(int cursorColor) {
		this.cursorColor = cursorColor;
	}

	public int getCursorColor() {
		return cursorColor;
	}

	public void setLineNumberColor(int lineNumberColor) {
		this.lineNumberColor = lineNumberColor;
	}

	public int getLineNumberColor() {
		return lineNumberColor;
	}

	class IInputConnection extends BaseInputConnection
	{
		public IInputConnection() {
			super(CodeView.this, true);
		}

		@Override
		public boolean commitText(CharSequence text, int newCursorPosition) {
			deleteIfSelection();
			mCode.appendAt(text, cursorPosition);
			cursorPosition += text.length() - 1 + newCursorPosition;
			notifyUpdate();
			scrollToCursor();
			invalidate();
			return true;
		}
	}
}
