package de.innologic.i18nservice.common.context;

import java.util.UUID;

public final class RequestContext {

    private RequestContext() {}

    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<>();

    public static void set(String requestId, String actor) {
        String rid = (requestId != null && !requestId.isBlank()) ? requestId : UUID.randomUUID().toString();
        String act = (actor != null && !actor.isBlank()) ? actor : "system";
        CTX.set(new Ctx(rid, act));
    }

    public static String requestId() {
        Ctx c = CTX.get();
        return c != null ? c.requestId : null;
    }

    public static String actor() {
        Ctx c = CTX.get();
        return c != null ? c.actor : "system";
    }

    public static void clear() {
        CTX.remove();
    }

    private record Ctx(String requestId, String actor) {}
}
