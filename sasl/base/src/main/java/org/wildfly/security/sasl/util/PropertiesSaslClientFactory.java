/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.security.sasl.util;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import org.wildfly.common.math.HashMath;

/**
 * A {@code SaslClientFactory} that allows properties to be added to a delegate {@code SaslClientFactory}.
 * @author Kabir Khan
 */
public class PropertiesSaslClientFactory extends AbstractDelegatingSaslClientFactory {

    private final Map<String, ?> properties;

    /**
     * Constructor
     * @param delegate the underlying {@code SaslClientFactory}
     * @param properties the properties
     */
    public PropertiesSaslClientFactory(SaslClientFactory delegate, Map<String, ?> properties) {
        super(delegate);
        this.properties = new HashMap<>(properties);
    }

    @Override
    public String[] getMechanismNames(Map<String, ?> props) {
        return delegate.getMechanismNames(combine(props, properties));
    }

    @Override
    public SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh) throws SaslException {
        return super.createSaslClient(mechanisms, authorizationId, protocol, serverName, combine(props, properties), cbh);
    }

    private static Map<String, ?> combine(Map<String, ?> provided, Map<String, ?> configured) {
        Map<String, Object> combined = new HashMap<>(provided);
        combined.putAll( configured);

        return combined;
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final Object other) {
        return other instanceof PropertiesSaslClientFactory && equals((PropertiesSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final AbstractDelegatingSaslClientFactory other) {
        return other instanceof PropertiesSaslClientFactory && equals((PropertiesSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final PropertiesSaslClientFactory other) {
        return super.equals(other) && properties.equals(other.properties);
    }

    protected int calculateHashCode() {
        return HashMath.multiHashOrdered(HashMath.multiHashOrdered(super.calculateHashCode(), getClass().hashCode()), properties.hashCode());
    }
}
