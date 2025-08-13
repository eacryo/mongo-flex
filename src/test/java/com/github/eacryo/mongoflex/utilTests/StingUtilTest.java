package com.github.eacryo.mongoflex.utilTests;

import com.github.eacryo.mongoflex.util.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


public class StingUtilTest {

    @Test
    public void testStingUtil() {
        String lower = "genshin";
        String camelCase = "zenlessZoneZero";
        Assertions.assertEquals(lower, StringUtil.camelToUnderscore(lower));
        Assertions.assertEquals("zenless_zone_zero", StringUtil.camelToUnderscore(camelCase));
    }
}
