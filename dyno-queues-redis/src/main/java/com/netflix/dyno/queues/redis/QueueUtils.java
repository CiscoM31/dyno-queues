package com.netflix.dyno.queues.redis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.netflix.dyno.connectionpool.exception.DynoException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Helper class to consolidate functions which might be reused across different DynoQueue implementations.
 */
public class QueueUtils {

    private static final int retryCount = 2;

    /**
     * Execute function with retries if required
     *
     * @param opName
     * @param keyName
     * @param r
     * @param <R>
     * @return
     */
    public static <R> R execute(String opName, String keyName, Callable<R> r) {
        return executeWithRetry(opName, keyName, r, 0);
    }

    private static <R> R executeWithRetry(String opName, String keyName, Callable<R> r, int retryNum) {

        try {

            return r.call();

        } catch (ExecutionException e) {

            if (e.getCause() instanceof DynoException) {
                if (retryNum < retryCount) {
                    return executeWithRetry(opName, keyName, r, ++retryNum);
                }
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Operation: ( " + opName + " ) failed on key: [" + keyName + " ].", e);
        }
    }

    /**
     * Construct standard objectmapper to use within the DynoQueue instances to read/write Message objects
     *
     * @return
     */
    public static ObjectMapper constructObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        om.disable(SerializationFeature.INDENT_OUTPUT);
        return om;
    }
}
