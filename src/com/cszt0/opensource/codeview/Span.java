package com.cszt0.opensource.codeview;

public final class Span
{
	private int start;
	private int end;
	
	private int color;
	private boolean colorEnable;
	private boolean bold;
	private boolean italic;

	public Span(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public int start() {
		return start;
	}
	
	public int end() {
		return end;
	}
	
	public void setPosition(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public boolean isBold() {
		return bold;
	}

	public void setItalic(boolean italic) {
		this.italic = italic;
	}

	public boolean isItalic() {
		return italic;
	}
	
	public void setColor(int color) {
		this.color = color;
		this.colorEnable = true;
	}
	
	public int getColor() {
		return color;
	}
	
	public boolean isColorEnable() {
		return colorEnable;
	}
}
