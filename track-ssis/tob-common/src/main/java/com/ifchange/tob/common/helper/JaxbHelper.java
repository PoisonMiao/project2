package com.ifchange.tob.common.helper;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javafx.util.Pair;
import org.dom4j.QName;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

/** XML处理类 **/
public final class JaxbHelper {
    private static final Set<String> DEFAULT_NS = Sets.newHashSet("", "##default");
    private static final Map<Class<?>, Pair<XmlRootElement, JAXBContext>> jaxbContextMap = Maps.newHashMap();
    private JaxbHelper() {
    }

    /** JAVA类生成 xml **/
    public static <T> String marshal(final T t, boolean pretty) {
        StringBuilder xml = new StringBuilder();
        try {
            Class<?> clazzT = t.getClass();
            Pair<XmlRootElement, JAXBContext> pair = jaxbPair(clazzT);
            Marshaller jaxbMarshaller = pair.getValue().createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            DocumentResult dr = new DocumentResult();
            jaxbMarshaller.marshal(t, dr);
            // XML根节点名称处理
            if(null != pair.getKey()) {
                String name = pair.getKey().name();
                String namespace = pair.getKey().namespace();
                if (DEFAULT_NS.contains(namespace)) {
                    if (!DEFAULT_NS.contains(name)) {
                        dr.getDocument().getRootElement().setQName(QName.get(name));
                    } else {
                        dr.getDocument().getRootElement().setQName(QName.get(clazzT.getSimpleName()));
                    }
                }
            }
            OutputFormat format = pretty ? OutputFormat.createPrettyPrint() : OutputFormat.createCompactFormat();
            format.setTrimText(true);
            format.setSuppressDeclaration(true);
            format.setExpandEmptyElements(true);
            StringWriter stringWriter = new StringWriter();
            XMLWriter writer = new XMLWriter(stringWriter, format);
            writer.write(dr.getDocument());
            xml.append(stringWriter.getBuffer());
        } catch (Exception e) {
            throw new RuntimeException("jaxb marshal error...", e);
        }
        return xml.toString();
    }

    /** xml string 转 class **/
    public static <T> T unmarshal(String xml, final Class<T> clazz) {
        try {
            return (T) (getUnmarshaller(clazz, null).unmarshal(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("jaxb unmarshal error...", e);
        }
    }
    /** xml string 转 class **/
    public static <T> T unmarshal(String xml, final Class<T> clazz, final ValidationEventHandler handler) {
        try {
            return (T) (getUnmarshaller(clazz, handler).unmarshal(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("jaxb unmarshal error...", e);
        }
    }
    /** xml stream 转 class **/
    public static <T> T unmarshal(final InputStream is, final Class<T> clazz) {
        try {
            return (T) (getUnmarshaller(clazz, null).unmarshal(is));
        } catch (Exception e) {
            throw new RuntimeException("jaxb unmarshal error...", e);
        }
    }
    /** xml stream 转 class **/
    public static <T> T unmarshal(final InputStream is, final Class<T> clazz, final ValidationEventHandler handler) {
        try {
            return (T) (getUnmarshaller(clazz, handler).unmarshal(is));
        } catch (Exception e) {
            throw new RuntimeException("jaxb unmarshal error...", e);
        }
    }

    private static <T> Unmarshaller getUnmarshaller(final Class<T> clazz, final ValidationEventHandler handler) throws Exception {
        Unmarshaller jaxbUnmarshaller = jaxbPair(clazz).getValue().createUnmarshaller();
        if (handler != null) {
            jaxbUnmarshaller.setEventHandler(handler);
        }
        return jaxbUnmarshaller;
    }

    private static <T> Pair<XmlRootElement, JAXBContext> jaxbPair(Class<T> clazz) throws JAXBException {
        Pair<XmlRootElement, JAXBContext> pair = jaxbContextMap.get(clazz);
        if (null == pair) synchronized(jaxbContextMap){
            XmlRootElement root = clazz.getAnnotation(XmlRootElement.class);
            JAXBContext context = JAXBContext.newInstance(clazz);
            pair = new Pair<>(root, context);
            jaxbContextMap.put(clazz, pair);
        }
        return pair;
    }
}
