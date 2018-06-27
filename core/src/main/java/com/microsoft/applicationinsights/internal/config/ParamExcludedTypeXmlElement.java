package com.microsoft.applicationinsights.internal.config;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/** This is the class for binding the xml array list of {@code <ExcludedTypes>} */
public class ParamExcludedTypeXmlElement {

  private List<String> excludedType;

  public List<String> getExcludedType() {
    return excludedType;
  }

  @XmlElement(name = "ExcludedType")
  public void setExcludedType(List<String> excludedType) {
    this.excludedType = excludedType;
  }
}
