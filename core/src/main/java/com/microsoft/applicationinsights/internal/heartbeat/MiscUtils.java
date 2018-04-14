package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This class contains some misc. functions used in heartbeat module.
 *
 * @author Dhaval Doshi
 */
public class MiscUtils {

  /**
   * Returns a list which contains items present in list2 but not in the set.
   * @param set
   * @param list2
   * @return
   */
  public static Set<String> except(Set<String> set, List<String> list2) {
    try {
      if (set == null || list2 == null) throw new IllegalArgumentException("Input is null");
      Set<String> result = new HashSet<>();
      for (String s : list2) {
        if (!set.contains(s)) {
          result.add(s);
        }
      }
      return result;
    }
    catch (Exception e) {
      InternalLogger.INSTANCE.warn("stack trace is %s", ExceptionUtils.getStackTrace(e));
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
      InternalLogger.INSTANCE.warn("exception while comparision, stack trace is %s",
          ExceptionUtils.getStackTrace(e));
    }
    finally{
      //return true so we don't add property when comparison exception
      return true;
    }

  }

}
