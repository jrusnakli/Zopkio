/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.LinkedList;

public class IntegerProducer {
  private static final Logger logger = LogManager.getLogger(IntegerProducer.class.getCanonicalName());
  private static final Logger perfLogger = LogManager.getLogger("perf." + IntegerProducer.class.getCanonicalName());

  private static final int TIMEOUT = 5000;
  private static final int HTTP_OK = 200;

  private static LinkedList<Integer> msgQueue;

  public static void main(String[] args) throws IOException {
    String strUrl = "http://" + args[0] + ":" + args[1];
    msgQueue = new LinkedList<Integer>();

    while (true) {
      logger.info("Starting production of single int...");
      long startTime = System.nanoTime();

      int i = (int)(Math.random() * 100);
      msgQueue.add(new Integer(i));

      try {
        URL url = new URL(strUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(TIMEOUT);
        urlConnection.setReadTimeout(TIMEOUT);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");

        OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
        Integer msg = msgQueue.remove();
        String str = msg.toString();
        out.write(str);
        out.flush();
        out.close();
        logger.info("Sent: " + str);

        BufferedReader response = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        if (response.ready()) {
          if (urlConnection.getResponseCode() != HTTP_OK) {
            logger.error("Could not send: " + str);
          }
          logger.info("Sent: " + str);
        }
        response.close();
      }
      catch (SocketTimeoutException ste) {
        logger.error("Server timeout");
        return;
      }
      catch (IOException ioe) {
        logger.error(ioe.getMessage());
        return;
      }

      int sleepTime = (int)(Math.random() * 10000);
      try {
        Thread.sleep(sleepTime);
      } catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
      }

      float executionTime = (float)(System.nanoTime() - startTime) / 1000000;
      perfLogger.info(String.format("%.3f", executionTime));
      logger.info("Ending production of single int...");
    }
  }
}
