package com.microsoft.ajl.simplecalc;

import java.util.Collection;

import com.microsoft.ajl.simplecalc.model.BinaryCalculation;
import com.microsoft.ajl.simplecalc.model.BinaryCalculation.TimestampedBinaryCalculation;

public interface ICalculationHistoryService {
	void addHistoryEntry(BinaryCalculation bc);
	Collection<TimestampedBinaryCalculation> getHistoryEntries();
}
