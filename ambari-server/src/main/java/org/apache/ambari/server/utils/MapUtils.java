/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MapUtils {
  public static Map<String, String> fillMap(String fileName)  {
    String line = null;
    Map<String, String>  map = new HashMap<>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
      while ((line = reader.readLine()) != null) {
        if (line.contains("=")) {
          String[] strings = line.split("=");
          map.put(strings[0], strings[1]);
        }
      }
    } catch (Exception e) {

    }
    return map;
  }

  public static Long parseLong(Map<String, Object> properties, String key) {
    Object idObj = properties.get(key);
    Long id = null;
    if (idObj != null)  {
      if(idObj instanceof Long) {
        id = (Long) idObj;
      } else {
        String idStr = (String) idObj;
        if(StringUtils.isNotBlank(idStr)) {
          id = Long.parseLong(idStr);
        }
      }
    }
    return id;
  }
}
