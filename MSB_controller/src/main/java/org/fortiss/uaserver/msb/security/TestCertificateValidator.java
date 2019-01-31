/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.msb.security;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.util.CertificateValidationUtil;

public class TestCertificateValidator implements CertificateValidator {

    private final Set<X509Certificate> trustedCertificates = Sets.newConcurrentHashSet();

    public TestCertificateValidator(X509Certificate certificate) {
        trustedCertificates.add(certificate);
    }

    public TestCertificateValidator(X509Certificate... certificates) {
        Collections.addAll(trustedCertificates, certificates);
    }

    @Override
    public void validate(X509Certificate certificate) throws UaException {
        CertificateValidationUtil.validateCertificateValidity(certificate);
    }

    @Override
    public void verifyTrustChain(X509Certificate certificate, List<X509Certificate> chain) throws UaException {
        CertificateValidationUtil.validateTrustChain(
            certificate, chain, trustedCertificates, Sets.<X509Certificate>newHashSet());
    }

}