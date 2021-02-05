/*
 * Copyright 2020: Thomson Reuters Global Resources. All Rights Reserved.
 *
 * Proprietary and Confidential information of TRGR. Disclosure, Use or
 * Reproduction without the written authorization of TRGR is prohibited.
 */

package heliopolis.p3x972.cascade.aux;

import java.net.InetAddress;

public class ServerInfo {
  private static String localHostName = null;

  private ServerInfo() {}

  public static String getLocalHostName() {
    if (localHostName == null) {
      try {
        localHostName = InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        // ignore
      }
    }
    return localHostName;
  }
}
