package com.cszt0.opensource.codeview.highlight;

import com.cszt0.opensource.codeview.HighLightTask;
import com.cszt0.opensource.codeview.Span;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighLightPrintA extends HighLightTask
{
	private String[] keyword;
	private String[] symbol;
	private String[] todo;
	private String[] value;

	private Span keywordSpan;
	private Span symbolSpan;
	private Span todoSpan;
	private Span valueSpan;

	private boolean needUpdatePattern;
	private Pattern all;
	private Pattern keywordPattern;
	private Pattern symbolPattern;
	private Pattern todoPattern;
	private Pattern valuePattern;

	public HighLightPrintA() {
		keyword = new String[] {};
		symbol = new String[] {};
		todo = new String[] {};
		value = new String[] {};
		keywordSpan = new Span();
		symbolSpan = new Span();
		todoSpan = new Span();
		valueSpan = new Span();
		needUpdatePattern = true;
	}

	@Override
	protected synchronized void doHighLight(String code) {
		if (needUpdatePattern) {
			updatePattern();
		}
		Pattern pattern=all;
		Matcher matcher=pattern.matcher(code);
		while (matcher.find()) {
			Span span;
			String match=code.substring(matcher.start(), matcher.end());
			if (match.startsWith("/") && todoPattern.matcher(match).find()) {
				span = new Span(todoSpan);
			} else if (valuePattern.matcher(match).find()) {
				span = new Span(valueSpan);
			} else if (keywordPattern.matcher(match).find()) {
				span = new Span(keywordSpan);
			} else {
				span = new Span(symbolSpan);
			}
			span.setPosition(matcher.start(), matcher.end());
			highlight(span);
		}
	}

	public synchronized void updatePattern() {
		StringBuilder builder;
		if (keyword.length == 0) {
			builder = new StringBuilder("(.*+.)");
		} else {
			builder = new StringBuilder("(");
			for (String word:keyword) {
				builder.append("(?=\\b)");
				builder.append(word);
				builder.append("(?=\\b)|");
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(")");
		}
		String keywordPatternString = builder.toString();
		if (symbol.length == 0) {
			builder = new StringBuilder("(.*+.)");
		} else {
			builder = new StringBuilder("(");
			for (String sym:symbol) {
				builder.append("\\Q");
				builder.append(sym);
				builder.append("\\E|");
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(")");
		}
		String symbolPatternString = builder.toString();
		if (todo.length == 0) {
			builder = new StringBuilder("(.*+.)");
		} else {
			builder = new StringBuilder("(");
			for (String t:todo) {
				builder.append(t);
				builder.append("|");
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(")");
		}
		String todoPatternString = builder.toString();
		if (value.length == 0) {
			builder = new StringBuilder("(.*+.)");
		} else {
			builder = new StringBuilder("(");
			for (String v:value) {
				builder.append(v);
				builder.append("|");
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(")");
		}
		String valuePatternString = builder.toString();
		String allString = String.format("(%s|%s|%s|%s)", keywordPatternString, symbolPatternString, todoPatternString, valuePatternString);
		all = Pattern.compile(allString);
		keywordPattern = Pattern.compile(keywordPatternString);
		symbolPattern = Pattern.compile(symbolPatternString);
		todoPattern = Pattern.compile(todoPatternString);
		valuePattern = Pattern.compile(valuePatternString);
	}

	public void setKeyword(String[] keywords, Span span) {
		keyword = keywords;
		keywordSpan = span;
		needUpdatePattern = true;
	}
	public void setSymbol(String[] symbols, Span span) {
		symbol = symbols;
		symbolSpan = span;
		needUpdatePattern = true;
	}
	public void setTodoRegexp(String[] todoRegexp, Span span) {
		todo = todoRegexp;
		todoSpan = span;
		needUpdatePattern = true;
	}
	public void setValueRegexp(String[] valueRegexp, Span span) {
		value = valueRegexp;
		valueSpan = span;
		needUpdatePattern = true;
	}
}
