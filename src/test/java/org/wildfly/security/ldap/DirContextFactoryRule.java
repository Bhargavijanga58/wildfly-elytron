/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.wildfly.security.ldap;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.apacheds.LdapService;
import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.wildfly.security.auth.realm.ldap.SimpleDirContextFactoryBuilder;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DirContextFactoryRule implements TestRule {

    static final String SERVER_DN = "uid=server,dc=elytron,dc=wildfly,dc=org";
    static final String SERVER_CREDENTIAL = "serverPassword";
    static final int LDAP_PORT = 11390;

    private static final Provider provider = new WildFlyElytronProvider();

    @Override
    public Statement apply(Statement current, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Security.addProvider(provider);
                LdapService embeddedServer = startEmbeddedServer();

                try {
                    current.evaluate();
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (embeddedServer != null) {
                        embeddedServer.close();
                    }

                    Security.removeProvider(provider.getName());
                }
            }
        };
    }

    public ExceptionSupplier<DirContext, NamingException> create() {
        SocketFactory socketFactory;
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(getClass().getResource("/ca/jks/ca.truststore").getFile()), "Elytron".toCharArray());
            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, trustFactory.getTrustManagers(), null);
            socketFactory = context.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return () -> SimpleDirContextFactoryBuilder.builder()
                        .setProviderUrl(String.format("ldap://localhost:%d/", LDAP_PORT))
                        .setSecurityPrincipal(SERVER_DN)
                        .setSecurityCredential(SERVER_CREDENTIAL)
                        .setSocketFactory(socketFactory)
                        .build().obtainDirContext(DirContextFactory.ReferralMode.IGNORE);
    }

    private LdapService startEmbeddedServer() {
        try {
            return LdapService.builder()
                    .setWorkingDir(new File("./target/apache-ds/working"))
                    .createDirectoryService("Test Service")
                    .addPartition("Elytron", "dc=elytron,dc=wildfly,dc=org", 5, "uid")
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-credential-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/memberOf-schema.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-attribute-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-role-mapping-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-group-mapping-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-otp-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-keystore-tests.ldif"))
                    .importLdif(PasswordSupportSuiteChild.class.getResourceAsStream("/ldap/elytron-x509-verification.ldif"))
                    .addTcpServer("Default TCP", "localhost", LDAP_PORT, "/ca/jks/localhost.keystore", "Elytron")
                    .start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start LDAP embedded server.", e);
        }
    }
}
