package com.ifchange.tob.common.helper;

import com.google.common.collect.Lists;
import com.ifchange.tob.common.support.OperationLog;
import javafx.util.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

@Ignore
public class BeanHelperTest {

    @Test
    public void form2Object() {
        String xxx = "a=1&b=2&a=3&c=5&b=sdfa=asa";
        System.out.println(StrCastHelper.form2Bean(xxx, Map.class));

        System.out.println(HttpHelper.ofQuery(Lists.newArrayList(
                new Pair<>("12", "ada"),
                new Pair<>("1a2", "ada"),
                new Pair<>("1a2", "ada")
        )));

        System.out.println(OperationLog[].class);
        System.out.println(String[].class);
        System.out.println(Integer[].class);
        System.out.println(List.class);
    }
}
