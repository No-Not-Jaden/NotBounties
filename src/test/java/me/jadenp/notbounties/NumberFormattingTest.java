package me.jadenp.notbounties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NumberFormattingTest {

    @org.junit.jupiter.api.Test
    void balanceRemoveCurrency() {
        NumberFormatting.currency = Arrays.asList("DIAMOND", "GOLD_INGOT");
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0}, new double[]{50,50}, new float[]{1,1}), new double[]{15,15});
        compareArrays(NumberFormatting.balanceRemoveCurrency(29, new float[]{0,0}, new double[]{50,50}, new float[]{1,1}), new double[]{15,14});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{1,0}, new double[]{50,50}, new float[]{1,1}), new double[]{30,0});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{2,1}, new double[]{50,50}, new float[]{1,1}), new double[]{20,10});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0}, new double[]{10,20}, new float[]{1,1}), new double[]{10,20});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0}, new double[]{21,9}, new float[]{1,1}), new double[]{21,9});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{1,0}, new double[]{5,50}, new float[]{1,1}), new double[]{5,25});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0}, new double[]{50,50}, new float[]{2,1}), new double[]{7,16});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0}, new double[]{15,15}, new float[]{5,1}), new double[]{3,15});
        NumberFormatting.currency = Arrays.asList("DIAMOND", "%vault_eco_balance%", "GOLD_INGOT");
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0,0}, new double[]{50,50,50}, new float[]{1,1,1}), new double[]{10,10,10});
        compareArrays(NumberFormatting.balanceRemoveCurrency(29, new float[]{0,0,0}, new double[]{50,50,50}, new float[]{1,1,1}), new double[]{10,10,9});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{1,0,0}, new double[]{50,50,50}, new float[]{1,1,1}), new double[]{30,0,0});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{3,2,1}, new double[]{50,50,50}, new float[]{1,1,1}), new double[]{15,10,5});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0,0}, new double[]{5,15,10}, new float[]{1,1,1}), new double[]{5,15,10});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0,0}, new double[]{21,5,4}, new float[]{1,1,1}), new double[]{21,5,4});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{1,0,0}, new double[]{5,50,5}, new float[]{1,1,1}), new double[]{5,20,5});
        compareArrays(NumberFormatting.balanceRemoveCurrency(52.5, new float[]{0,0,0}, new double[]{10,28.5,14}, new float[]{1,1,1}), new double[]{10,28.5,14});
        compareArrays(NumberFormatting.balanceRemoveCurrency(30, new float[]{0,0,0}, new double[]{50,50,50}, new float[]{1,2,1}), new double[]{10,5,10});
        compareArrays(NumberFormatting.balanceRemoveCurrency(60, new float[]{0,0,0}, new double[]{10,20,30}, new float[]{1,4,3}), new double[]{10,5,10});
    }

    void compareArrays(double[] a1, double[] a2) {
        System.out.println(Arrays.toString(a1));
        System.out.println(Arrays.toString(a2));

        assertEquals(a1.length, a2.length);
        for (int i = 0; i < Math.min(a1.length, a2.length); i++) {
            assertEquals(round(a2[i], 3), round(a1[i], 3));
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}