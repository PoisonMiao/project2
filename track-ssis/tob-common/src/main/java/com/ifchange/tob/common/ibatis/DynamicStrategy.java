package com.ifchange.tob.common.ibatis;

import com.google.common.collect.Sets;
import com.ifchange.tob.common.helper.CollectsHelper;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.LinkedHashSet;

public interface DynamicStrategy {
    LinkedHashSet<Target> strategy();

    /** 目标策略生成LinkedHashSet **/
    static LinkedHashSet<Target> ofTargetSet(Target ...targets) {
        LinkedHashSet<Target> targetSet = Sets.newLinkedHashSet();
        if(!CollectsHelper.isNullOrEmpty(targets)) {
            targetSet.addAll(Arrays.asList(targets));
        }
        return targetSet;
    }
    /** 表后缀生成LinkedHashSet **/
    static LinkedHashSet<String> ofTableSuffixSet(String ...tableSuffixes) {
        LinkedHashSet<String> tableSuffixSet = Sets.newLinkedHashSet();
        if(!CollectsHelper.isNullOrEmpty(tableSuffixes)) {
            tableSuffixSet.addAll(Arrays.asList(tableSuffixes));
        }
        return tableSuffixSet;
    }

    class Target {
        final String datasource;
        final LinkedHashSet<String> tableSuffix;

        public Target(String datasource, LinkedHashSet<String> tableSuffix) {
            this.datasource = datasource;
            this.tableSuffix = tableSuffix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Target)) return false;
            Target target = (Target) o;
            return new EqualsBuilder().append(datasource, target.datasource).append(tableSuffix, target.tableSuffix).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(datasource).append(tableSuffix).toHashCode();
        }
    }
}
