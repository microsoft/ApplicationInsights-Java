package com.microsoft.applicationinsights.internal.config;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/** This class is used to bind the xml array list of {@code <IncludeTypes>} */
public class ParamIncludedTypeXmlElement {

  private List<String> includedType;

  public List<String> getIncludedType() {
    return includedType;
  }

  @XmlElement(name = "IncludedType")
  public void setIncludedType(List<String> includedType) {
    this.includedType = includedType;
  }
}
