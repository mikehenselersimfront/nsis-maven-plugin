/*
 * Copyright 2008 Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.nsis.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Variation on the StreamPumper theme.
 *
 * @version $Id: ProcessOutputHandler.java 18289 2013-05-10 12:37:34Z rfscholte$
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @author Nadahar
 */
public class ProcessOutputHandler implements Runnable {

	private static final int SIZE = 1024;

	private final BufferedReader reader;

	private final ProcessOutputConsumer consumer;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param inputStream the {@link InputStream} to handle.
	 * @param consumer the {@link ProcessOutputConsumer} that will consume the
	 *            output.
	 * @param inputCharset the {@link Charset} to use when interpreting the
	 *            {@link InputStream}.
	 */
	public ProcessOutputHandler(InputStream inputStream, ProcessOutputConsumer consumer, Charset inputCharset) {
		this.reader = new BufferedReader(
			new InputStreamReader(inputStream, inputCharset == null ? StandardCharsets.UTF_8 : inputCharset),
			SIZE
		);
		this.consumer = consumer;
	}

	@Override
	public void run() {
		try {
			String line = reader.readLine();

			while (line != null) {
				if (consumer != null) {
					consumer.consumeOutputLine(line);
				}

				line = reader.readLine();
			}
		} catch (IOException e) {
			// Catch IOException blindly.
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// Just catch
			}
		}
	}

	/**
	 * Starts this {@link ProcessOutputConsumer}.
	 */
	public void startThread() {
		Thread thread = new Thread(this, "ProcessOutputHandler");
		thread.start();
	}
}
