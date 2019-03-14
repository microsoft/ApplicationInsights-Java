package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.Collection;
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
   * Returns a set which contains result of List - Set
   * @param target
   * @param toRemove
   * @return
   */
   public static Set<String> except(Collection<String> target, Collection<String> toRemove) {
      if (toRemove == null) {
        throw new IllegalArgumentException("Input is null");
      }
      if(target == null) {
        throw new IllegalArgumentException("Input is null");
      }
      Set<String> result = new HashSet<>(target);
      result.removeAll(toRemove);
      return result;
  }

}
