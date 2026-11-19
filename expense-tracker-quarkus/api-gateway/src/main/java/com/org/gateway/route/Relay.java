package com.org.gateway.route;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

/** Copies a downstream response back to the client: status, content type, body and a rewritten Location. */
final class Relay {

    private Relay() {
    }

    static Response of(Response downstream, UriInfo uriInfo) {
        try (downstream) {
            Response.ResponseBuilder out = Response.status(downstream.getStatus());
            if (downstream.hasEntity()) {
                out.entity(downstream.readEntity(String.class));
                MediaType type = downstream.getMediaType();
                if (type != null) {
                    out.type(type);
                }
            }
            URI location = downstream.getLocation();
            if (location != null) {
                // The downstream Location points at the internal host (e.g. expense-service:8080);
                // re-base it on the gateway's own public address.
                out.location(uriInfo.getBaseUriBuilder().replacePath(location.getRawPath()).build());
            }
            return out.build();
        }
    }
}
