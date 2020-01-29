package com.kal.ssps.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class JsonUtil {

    private static ObjectMapper om = new ObjectMapper();
    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    public static <T extends Collection> int jsonCollectionSize(String str, Class<T> clazz)  {
        if (str == null || str.trim().length() == 0)
            return 0;

        try {
            Collection coll = om.readValue(str, clazz);
            return coll.size();
        } catch (IOException e) {

        }

        return -1;
    }

    public static<T> T readValue(InputStream is, Class<T> clazz) {
        try {
            return om.readValue(is, clazz);
        } catch (IOException ex) {
            String msg = String.format("Exception in reading from input-stream to type %s.", clazz);
            logger.error(msg, ex);
        }

        return null;
    }


    public static<T> T readValue(String str, Class<T> clazz) {
        try {
            return om.readValue(str, clazz);
        } catch (IOException ex) {
            String msg = String.format("Exception in converting %s to type %s.",str, clazz);
            logger.error(msg, ex);
        }

        return null;
    }

    public static<T> String writeValueAsString(T obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (IOException ex) {
            logger.error("", ex);
        }

        return obj.toString();
    }

}