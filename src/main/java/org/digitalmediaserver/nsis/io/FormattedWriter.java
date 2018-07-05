/*
 * Copyright 2018 Digital Media Server
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.nsis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.digitalmediaserver.nsis.Utils;


/**
 * A {@link Writer} extension that overrides all calls to the super class using
 * {@link Writer} "as an interface". This class will, unlike {@link Writer},
 * handle {@link Locale} and {@link Charset} conversion. It also allows a
 * customized line separator and a buffer with a configurable approximate size.
 * <p>
 * In addition to the inherited {@link #write} methods this class implements
 * {@link #writeln(String)}, {@link #newLine()},
 * {@link #write(String, Object...)} and {@link #writeln(String, Object...)}.
 * The latter two methods allows formatting as if
 * {@link String#format(String, Object...)} called but with the {@link Locale}
 * from the constructor applied.
 * <p>
 * The buffer size is "lazy" in that it only checks between writes and flushes
 * if the threshold has been exceeded. The flushing is only of the buffer to the
 * underlying {@link OutputStream}, if the {@link OutputStream} is also buffered
 * it isn't flushed until the {@link FormattedWriter} is closed.
 *
 * @author Nadahar
 */
public class FormattedWriter extends Writer implements AutoCloseable {

	/** The default automatic flush threshold/approximate buffer size */
	public static final int DEFAULT_AUTO_FLUSH_THRESHOLD = 1024;

	private final Formatter formatter;
	private final StringBuilder sb;
	private final OutputStream outputStream;
	private final Charset charset;
	private final int autoFlushThreshold;
	private final String lineSeparator;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param outputStream the {@link OutputStream} to write to.
	 * @param locale the {@link Locale} to use for formatting. If {@code null},
	 *            {@link Locale#ROOT} is used.
	 * @param charset the {@link Charset} to use when writing. If {@code null},
	 *            {@code UTF-8} is used.
	 * @param lineSeparator the line separator to use with {@link #newLine()},
	 *            {@link #writeln(String)} and
	 *            {@link #writeln(String, Object...)}. if blank the system
	 *            default line separator is used.
	 * @param autoFlushThreshold the buffer flush threshold. If less than 64,
	 *            {@value #DEFAULT_AUTO_FLUSH_THRESHOLD} is used.
	 */
	public FormattedWriter(
		@Nonnull OutputStream outputStream,
		@Nullable Locale locale,
		@Nullable Charset charset,
		@Nullable String lineSeparator,
		int autoFlushThreshold
	) {
		if (locale == null) {
			locale = Locale.ROOT;
		}
		this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
		this.lineSeparator = Utils.isBlank(lineSeparator) ? System.lineSeparator() : lineSeparator;
		this.autoFlushThreshold = autoFlushThreshold > 64 ? autoFlushThreshold : DEFAULT_AUTO_FLUSH_THRESHOLD;
		sb = new StringBuilder((int) (this.autoFlushThreshold * 1.2));
		this.outputStream = outputStream;
		this.formatter = new Formatter(sb, locale);
	}

	/**
	 * Creates a new instance that writes to the specified {@link Path} using
	 * {@link Locale#ROOT} and {@code UTF-8}.
	 *
	 * @param path the {@link Path} to write to. If the file already exists, it
	 *            will be overwritten.
	 * @param lineSeparator the line separator to use with {@link #newLine()},
	 *            {@link #writeln(String)} and
	 *            {@link #writeln(String, Object...)}. if blank the system
	 *            default line separator is used.
	 * @throws IOException If an error occurs during the operation.
	 */
	public FormattedWriter(
		@Nonnull Path path,
		@Nullable String lineSeparator
	) throws IOException {
		this(path, null, null, lineSeparator, 0);
	}

	/**
	 * Creates a new instance that writes to the specified {@link Path}.
	 *
	 * @param path the {@link Path} to write to. If the file already exists, it
	 *            will be overwritten.
	 * @param locale the {@link Locale} to use for formatting. If {@code null},
	 *            {@link Locale#ROOT} is used.
	 * @param charset the {@link Charset} to use when writing. If {@code null},
	 *            {@code UTF-8} is used.
	 * @param lineSeparator the line separator to use with {@link #newLine()},
	 *            {@link #writeln(String)} and
	 *            {@link #writeln(String, Object...)}. if blank the system
	 *            default line separator is used.
	 * @param autoFlushThreshold the buffer flush threshold. If less than 64,
	 *            {@value #DEFAULT_AUTO_FLUSH_THRESHOLD} is used.
	 * @throws IOException If an error occurs during the operation.
	 */
	public FormattedWriter(
		@Nonnull Path path,
		@Nullable Locale locale,
		@Nullable Charset charset,
		@Nullable String lineSeparator,
		int autoFlushThreshold
	) throws IOException {
		if (locale == null) {
			locale = Locale.ROOT;
		}
		this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
		this.lineSeparator = Utils.isBlank(lineSeparator) ? System.lineSeparator() : lineSeparator;
		this.autoFlushThreshold = autoFlushThreshold > 64 ? autoFlushThreshold : DEFAULT_AUTO_FLUSH_THRESHOLD;
		sb = new StringBuilder((int) (this.autoFlushThreshold * 1.2));
		this.outputStream =  Files.newOutputStream(path);
		this.formatter = new Formatter(sb, locale);
	}

	@Override
	public void write(int c) throws IOException {
		sb.append(c);
		flushBuffer(true);
	}

	@Override
	public void write(@Nullable String string, int offset, int length) throws IOException {
		if (string == null) {
			return;
		}
		sb.append(string.substring(offset, offset + length));
		flushBuffer(true);
	}

	@Override
	public void write(@Nullable char[] chars) throws IOException {
		if (chars == null) {
			return;
		}
		sb.append(chars);
		flushBuffer(true);
	}

	@Override
	public void write(char[] chars, int offset, int length) throws IOException {
		if (chars == null) {
			return;
		}
		sb.append(chars, offset, length);
		flushBuffer(true);
	}

	@Override
	public void write(@Nullable String string) throws IOException {
		if (string == null) {
			return;
		}
		sb.append(string);
		flushBuffer(true);
	}

	/**
	 * Writes a line.
	 *
	 * @param string the {@link String} to write.
	 * @throws IOException If an error occurs during the operation.
	 */
	public void writeln(@Nullable String string) throws IOException {
		if (string != null) {
			sb.append(string);
		}
		sb.append(lineSeparator);
		flushBuffer(true);
	}

	/**
	 * Writes a formatted string as if
	 * {@link String#format(Locale, String, Object...)} had been called.
	 *
	 * @param format the <a href=
	 *            "https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax"
	 *            >format string</a>
	 * @param args Arguments referenced by the format specifiers in the
	 *            {@code format} string. If there are more arguments than format
	 *            specifiers, the extra arguments are ignored. The number of
	 *            arguments is variable and may be zero. The maximum number of
	 *            arguments is limited by the maximum dimension of a Java array.
	 *            The behavior on a {@code null} argument depends on the
	 *            conversion.
	 * @throws IOException If an error occurs during the operation.
	 */
	public void write(@Nullable String format, Object... args) throws IOException {
		if (format == null) {
			return;
		}
		formatter.format(format, args);
		flushBuffer(true);
	}

	/**
	 * Writes a formatted line as if
	 * {@link String#format(Locale, String, Object...)} had been called followed
	 * by {@link #newLine()}.
	 *
	 * @param format the <a href=
	 *            "https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax"
	 *            >format string</a>
	 * @param args Arguments referenced by the format specifiers in the
	 *            {@code format} string. If there are more arguments than format
	 *            specifiers, the extra arguments are ignored. The number of
	 *            arguments is variable and may be zero. The maximum number of
	 *            arguments is limited by the maximum dimension of a Java array.
	 *            The behavior on a {@code null} argument depends on the
	 *            conversion.
	 * @throws IOException If an error occurs during the operation.
	 */
	public void writeln(@Nullable String format, Object... args) throws IOException {
		if (format != null) {
			formatter.format(format, args);
		}
		sb.append(lineSeparator);
		flushBuffer(true);
	}

	@Override
	public Writer append(char c) throws IOException {
		sb.append(c);
		flushBuffer(true);
		return this;
	}

	@Override
	public Writer append(@Nullable CharSequence charSequence) throws IOException {
		if (charSequence == null) {
			return this;
		}
		sb.append(charSequence);
		flushBuffer(true);
		return this;
	}

	@Override
	public Writer append(@Nullable CharSequence charSequence, int start, int end) throws IOException {
		if (charSequence == null) {
			return this;
		}
		sb.append(charSequence.subSequence(start, end));
		flushBuffer(true);
		return this;
	}

	/**
	 * Writes the line separator specified in the constructor.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	public void newLine() throws IOException {
		sb.append(lineSeparator);
		flushBuffer(true);
	}

	/**
	 * Flushes the buffer to the {@link OutputStream} as needed.
	 *
	 * @param ifAboveThreshold if {@code true} the buffer will only be flushed
	 *            if it's larger than the threshold, if {@code false} it will be
	 *            flushed regardless.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void flushBuffer(boolean ifAboveThreshold) throws IOException {
		if (!ifAboveThreshold && sb.length() > 0 || sb.length() >= autoFlushThreshold) {
			byte[] bytes = sb.toString().getBytes(charset);
			sb.setLength(0);
			outputStream.write(bytes);
		}
	}

	@Override
	public void flush() throws IOException {
		flushBuffer(false);
		outputStream.flush();
	}

	@Override
	public void close() throws IOException {
		flush();
		outputStream.close();
	}
}
