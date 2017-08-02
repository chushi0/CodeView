package com.cszt0.opensource.codeview;

public abstract interface TextWatcher
{
	public abstract void onInputText(CharSequence text, int cursorPosition);
	
	public abstract void onLineChange(CharSequence oldLineSource, CharSequence newLineSource, int lineno);
	
	public abstract void onNewLine(CharSequence newLine, int lineno);
	
	public abstract void onDelete(int start, int len, CharSequence content);

}
