package com.opendev.securifi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.text.format.Time;

public class HelperFunctions {
	public static String PREFIX = "QCOM";

	public String format(Number n) {
		NumberFormat format = DecimalFormat.getInstance();
		format.setRoundingMode(RoundingMode.FLOOR);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(2);
		return format.format(n);
	}

	public double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	
	public String mGenerateFileName(String theParam, String theSuffix,
			int theTmpNo) {
		String Filename = null;
		Time now = new Time();
		now.setToNow();
		Filename = new String(PREFIX + "_" + theParam + "_"
				+ now.format3339(true) + "_" + theTmpNo + "_" + theSuffix);
		return Filename;
	}
}
