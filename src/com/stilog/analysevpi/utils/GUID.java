/**
 *
 * Copyright 2008 Stilog IST
 *
 * Licensed under the Visual Planning License.
 * All rights reserved.
 *
 */
package com.stilog.analysevpi.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Hashtable;

/**
 * Class : GUID
 * 
 * @author thierry (16 oct. 2008)
 * 
 */
public class GUID {

  /** */
  private static SecureRandom mySecureRand;

  /** */
  private static String s_id;

  /**
   * Static block to take care of one time secureRandom seed. It takes a few seconds to initialize SecureRandom. You might want to consider removing this static block or replacing it with a "time since first loaded" seed to reduce this time. This
   * block will run only once per JVM instance.
   */

  static {
    mySecureRand = new SecureRandom();
    try {
      s_id = InetAddress.getLocalHost().toString();
    } catch( UnknownHostException e ) {
      e.printStackTrace();
      s_id = "localhost";
    }
  }

  /**
   * Default constructor. With no specification of security option, this constructor defaults to lower security, high performance.
   */
  private GUID() {

  }

  /**
   * Method to generate the random GUID
   * 
   * @return
   */
  public static String makeGUID() {
    StringBuffer sbValueBeforeMD5 = new StringBuffer();

    try {
      long time = System.currentTimeMillis();
      long rand = mySecureRand.nextLong();

      sbValueBeforeMD5.append(s_id);
      sbValueBeforeMD5.append(":");
      sbValueBeforeMD5.append(Long.toString(time));
      sbValueBeforeMD5.append(":");
      sbValueBeforeMD5.append(Long.toString(rand));

      String valueBeforeMD5 = sbValueBeforeMD5.toString();

      return makeGUID(valueBeforeMD5);
    } catch( Exception e ) {
      System.out.println("Error:" + e);
      return null;
    }
  }

  /**
   * @param valueBeforeMD5
   * @return
   */
  public static String makeGUID(String valueBeforeMD5) {

    try {
      MessageDigest md5 = null;

      try {
        md5 = MessageDigest.getInstance("MD5");
      } catch( NoSuchAlgorithmException e ) {
        System.out.println("Error: " + e);
        return null;
      }

      md5.update(valueBeforeMD5.getBytes());

      byte[] array = md5.digest();
      StringBuffer sb = new StringBuffer();
      for( int j = 0; j < array.length; ++j ) {
        int b = array[j] & 0xFF;
        if( b < 0x10 ) {
          sb.append('0');
        }
        sb.append(Integer.toHexString(b));
      }

      String valueAfterMD5 = sb.toString();

      return toGUIDString(valueAfterMD5);
    } catch( Exception e ) {
      System.out.println("Error:" + e);
      return null;
    }
  }

  /**
   * @param valueAfterMD5
   * @return
   */
  private static String toGUIDString(String valueAfterMD5) {

    String raw = valueAfterMD5.toUpperCase();
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < 8; i++ ) {
      if( sb.length() > 0 ) {
        sb.append("-");
      }
      sb.append(raw.substring(i * 4, (i + 1) * 4));
    }

    return sb.toString();
  }

  public static void main(String args[]) {
    for( int i = 0; i < 100; i++ ) {
      System.out.println("RandomGUID=" + GUID.makeGUID());
    }
  }

}
