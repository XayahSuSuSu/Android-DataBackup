package com.xayah.dex;

import org.junit.Test;

public class ExampleUnitTest {
    @Test
    public void test_CCHelper() {
        System.out.println(new CCHelper().s2t("You Only live once! 生如长河，渡船千艘，唯自渡方是真渡。开启, 台湾, 香港"));
        System.out.println(new CCHelper().t2s("You Only live once! 生如長河，渡船千艘，唯自渡方是真渡。開啟, 台灣, 香港"));
    }
}
