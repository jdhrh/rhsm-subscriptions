/*
 * Copyright (c) 2019 - 2020 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.http;

import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Utility class for customizing HTTP clients used by API clients.
 */
public class HttpClient {

    private HttpClient() {
        throw new IllegalStateException("Utility class; should never be instantiated!");
    }

    /**
     * Customize the creation of the HTTP client the API will be using.
     *
     * @param serviceProperties client configuration properties
     * @param clientJson API client configuration json
     * @param isDebugging whether the API client is debugging
     * @return Client with customized connection settings
     */
    public static Client buildHttpClient(HttpClientProperties serviceProperties, Object clientJson,
        boolean isDebugging) {
        HttpClientBuilder apacheBuilder = HttpClientBuilder.create();

        // Bump the max connections so that we don't block on multiple async requests
        // to the service.
        apacheBuilder.setMaxConnPerRoute(serviceProperties.getMaxConnections());
        apacheBuilder.setMaxConnTotal(serviceProperties.getMaxConnections());
        org.apache.http.client.HttpClient httpClient = apacheBuilder.build();

        ClientHttpEngine engine = ApacheHttpClient4EngineFactory.create(httpClient);

        ClientConfiguration clientConfig = new ClientConfiguration(ResteasyProviderFactory.getInstance());
        clientConfig.register(clientJson);
        if (isDebugging) {
            clientConfig.register(org.jboss.logging.Logger.class);
        }
        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

        return ((ResteasyClientBuilder) clientBuilder).httpEngine(engine).build();
    }
}
