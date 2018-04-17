package com.microsoft.applicationinsights.internal.heartbeat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains some misc. functions used in heartbeat module.
 *
 * @author Dhaval Doshi
 */
class MiscUtils {

  /**
   * Returns a list which contains result of List - Set
   * @param list2
   * @param set
   * @return
   */
   static Set<String> except(List<String> list2, Set<String> set) {
    try {
      if (set == null) {
        throw new IllegalArgumentException("Input is null");
      }
      if( list2 == null) {
        throw new IllegalArgumentException("Input is null");
      }
      Set<String> result = new HashSet<>(list2);
      result.removeAll(set);
      return result;
    }
    catch (Exception e) {
      //chomp;
    }
    finally{
      return set;
    }

  }

  /**
   * This method returns true if keyword is present in the input list ignoring the case
   * @param keyword to compare
   * @param inputList list of string containing reserved keywords
   * @return true if input word is present in the list ignoring the case.
   */
  static boolean containsIgnoreCase(String keyword, Set<String> inputList) {
    try {
      if (keyword == null) throw new IllegalArgumentException("Keyword to compare is null");
      if (inputList == null) throw new IllegalArgumentException("List to compare is null");
      for (String key : inputList) {
        if (key.equalsIgnoreCase(keyword)) return true;
      }
      return false;
    }
    catch (Exception e) {
      //chomp;
    }
    finally{
      //return true so we don't add property when comparison exception
      return true;
    }

  }

}
