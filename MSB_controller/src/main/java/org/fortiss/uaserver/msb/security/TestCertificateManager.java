/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.msb.security;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

public class TestCertificateManager implements CertificateManager {

    private final KeyPair keyPair;
    private final X509Certificate certificate;

    public TestCertificateManager(KeyPair keyPair, X509Certificate certificate) {
        this.keyPair = keyPair;
        this.certificate = certificate;
    }

    @Override
    public Optional<KeyPair> getKeyPair(ByteString thumbprint) {
        return Optional.of(keyPair);
    }

    @Override
    public Optional<X509Certificate> getCertificate(ByteString thumbprint) {
        return Optional.of(certificate);
    }

    @Override
    public Set<KeyPair> getKeyPairs() {
        return Sets.newHashSet(keyPair);
    }

    @Override
    public Set<X509Certificate> getCertificates() {
        return Sets.newHashSet(certificate);
    }

}