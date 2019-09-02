package com.ifchange.tob.common.helper;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ifchange.tob.common.core.CacheSerialize;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;

@Ignore
public class StringHelperTest {

    @Test
    public void splitText() {
        System.out.println(JsonHelper.toJSONString(StringHelper.list(StringHelper.EMPTY, "李li天tian一yi")));
    }

    @Test
    public void testFlock() {
        List<Integer> ls = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
        Set<Integer> list = Sets.newTreeSet(ls);
        CacheSerialize.Flock<Integer> flock = new CacheSerialize.Flock(list);
        System.out.println(flock.getSerializableId());
        System.out.println(list);
        System.out.println(list.equals(flock));
        System.out.println(Sets.newTreeSet(ls).equals(list));
        System.out.println(JsonHelper.toJSONString(flock));
        System.out.println(flock);
        CacheSerialize.Flock<Integer> fI = new CacheSerialize.Flock<>(JsonHelper.parseArray("[0,1,2,3,4,5,6,7,8,9]", Integer.class));
        System.out.println(fI.getSerializableId());
        System.out.println(flock.getSerializableId().equals(fI.getSerializableId()));
    }
}
