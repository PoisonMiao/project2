package com.ifchange.tob.common.esearch;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.esearch.orm.EomModel;
import com.ifchange.tob.common.esearch.orm.Mapping;
import com.ifchange.tob.common.esearch.orm.Property;
import com.ifchange.tob.common.esearch.orm.Typical;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.JvmOSHelper;
import com.ifchange.tob.common.helper.MathHelper;
import com.ifchange.tob.common.helper.StringHelper;
import javafx.util.Pair;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class EomInitializer {
    private static final Map<String, Map<String, JSONObject>> EOM_MAP = Maps.newConcurrentMap();

    static final Map<Class<? extends EomModel>, Pair<String, String>> CIT_MAP = Maps.newConcurrentMap();
    static void prepare(String basePackage) {
        if(StringHelper.isBlank(basePackage)) {
            return;
        }
        Set<Class<?>> classes = JvmOSHelper.classesAnnotatedWith(basePackage, Mapping.class);
        for(Class<?> clazz: classes) {
            if(EomModel.class.isAssignableFrom(clazz)) {
                Mapping mapping = clazz.getAnnotation(Mapping.class);
                String indices = mapping.indices().length() < 1 ? mapping.name() : mapping.indices();
                int partitions = mapping.partitions() < 1 ? 1 : mapping.partitions();
                String eomKey = eomKey(indices, partitions);
                EOM_MAP.putIfAbsent(eomKey, Maps.newConcurrentMap());
                Map<String, JSONObject> types = EOM_MAP.get(eomKey);

                JSONObject json = new JSONObject();
                types.put(mapping.name(), json);
                // operation
                json.put("operation", mapping.operation());
                // settings
                JSONObject settings = new JSONObject();
                settings.put("index", ImmutableMap.of("number_of_shards", mapping.shards(), "number_of_replicas", mapping.replicas()));
                json.put("settings", settings);
                // mappings -> properties
                JSONObject mappings = new JSONObject(), properties = new JSONObject();
                mappings.put(mapping.name(), ImmutableMap.of("dynamic", mapping.dynamic(), "properties", properties));
                Field[] fieldArray = clazz.getDeclaredFields();
                if(!CollectsHelper.isNullOrEmpty(fieldArray)) {
                    Field[] parents = EomModel.class.getDeclaredFields();
                    if(!CollectsHelper.isNullOrEmpty(parents)) {
                        for(Field field: parents) {
                            mappingField(properties, field);
                        }
                    }
                    for(Field field: fieldArray) {
                        mappingField(properties, field);
                    }
                }
                json.put("mappings", mappings);
                //noinspection unchecked
                CIT_MAP.put((Class<? extends EomModel>) clazz, new Pair<>(eomKey, mapping.name()));
            } else {
                throw new EsearchException("EOM CLASS " + clazz.getName() + " must extends " + EomModel.class.getName());
            }
        }
    }

    static void processed() {
        if(CollectsHelper.isNullOrEmpty(EOM_MAP)) {
            return;
        }
        for(Map.Entry<String, Map<String, JSONObject>> entry: EOM_MAP.entrySet()) {
            String eomKey = entry.getKey();
            for(Map.Entry<String, JSONObject> json: entry.getValue().entrySet()) {
                JSONObject value = json.getValue();
                EsearchOperations operation = EsearchFactory.get(value.getString("operation"));
                Pair<String, Integer> pars = partitions(eomKey);
                for(int hash=0; hash < pars.getValue(); hash++) {
                    String indices = pars.getKey() + hash;
                    operation.onlyMakeGetIndex(indices, value.getJSONObject("settings"), value.getJSONObject("aliases"));
                    operation.typeMappingPut(indices, json.getKey(), value.getJSONObject("mappings"));
                }
            }
        }
        EOM_MAP.clear();
    }

    private static void mappingField(JSONObject properties, Field field) {
        Property property = field.getAnnotation(Property.class);
        if(null == property) {
            return;
        }
        JSONObject propertyJson = new JSONObject();
        propertyJson.put("include_in_all", property.all());
        if(Typical.Object == property.type()){
            JSONObject objects = new JSONObject();
            Field[] fields = field.getType().getDeclaredFields();
            for(Field sub: fields) {
                Property prop = sub.getAnnotation(Property.class);
                if(Typical.Object == prop.type()) {
                    throw new EsearchException("EOM class propertyAt object can not exceed 2 level...");
                }
                JSONObject objectJson = new JSONObject();
                objectJson.put("include_in_all", prop.all());
                objectJson.put("type", prop.type().toString());
                analyzed(prop, objectJson);
                objects.put(StringHelper.camel2Underline(sub.getName()), objectJson);
            }
            propertyJson.put("properties", objects);
        } else {
            propertyJson.put("type", property.type().toString());
            analyzed(property, propertyJson);
        }
        properties.put(StringHelper.camel2Underline(field.getName()), propertyJson);
    }

    static void analyzed(Property property, JSONObject json) {
        if(Typical.Keyword == property.type()) {
            if (property.analyzer().length() > 0) {
                json.put("index", property.analyzer());
            }
        } else {
            if (property.analyzer().length() > 0) {
                json.put("analyzer", property.analyzer());
            }
        }
    }

    static String eomKey(String indices, int partitions) {
        return indices + "@@@" + partitions;
    }

    static Pair<String, Integer> partitions(String eomKey) {
        List<String> pars = StringHelper.list("@@@", eomKey);
        return new Pair<>(pars.get(0), MathHelper.toInt(pars.get(1), 1));
    }
}
