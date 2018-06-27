package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains some misc. functions used in heartbeat module.
 *
 * @author Dhaval Doshi
 */
public class MiscUtils {

  /**
   * Returns a list which contains result of List - Set
   *
   * @param list2
   * @param set
   * @return
   */
  public static Set<String> except(List<String> list2, Set<String> set) {
    try {
      if (set == null) {
        throw new IllegalArgumentException("Input is null");
      }
      if (list2 == null) {
        throw new IllegalArgumentException("Input is null");
      }
      Set<String> result = new HashSet<>(list2);
      result.removeAll(set);
      return result;
    } catch (Exception e) {
      // chomp;
    } finally {
      return set;
    }
  }
}
