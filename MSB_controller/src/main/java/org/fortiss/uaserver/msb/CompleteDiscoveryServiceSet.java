/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fortiss.uaserver.msb;

import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.services.DiscoveryServiceSet;
import org.eclipse.milo.opcua.stack.core.application.services.ServiceRequest;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ApplicationType;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.GetEndpointsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.GetEndpointsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.eclipse.milo.opcua.stack.server.Endpoint;
import org.eclipse.milo.opcua.stack.server.tcp.UaTcpStackServer;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.a;

/**
 * @author fortiss
 */
public class CompleteDiscoveryServiceSet implements DiscoveryServiceSet {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CompleteDiscoveryServiceSet.class);

    private UaTcpStackServer ldsServer;
    private MsbUaServer msbServer;
    private ServerRegistrationManagement serverExpMgnt;

    private BiConsumer<String, Boolean> onRegisterCallback;

    public CompleteDiscoveryServiceSet(MsbUaServer msbServer, BiConsumer<String, Boolean> onRegisterCallback) {
        this.msbServer = msbServer;
        ldsServer = (UaTcpStackServer) msbServer.getServer().getServer();
        serverExpMgnt = new ServerRegistrationManagement();
        this.onRegisterCallback = onRegisterCallback;
    }

    private EndpointDescription mapEndpoint(Endpoint endpoint) {
        List<UserTokenPolicy> userTokenPolicies = ldsServer.getConfig().getUserTokenPolicies();

        return new EndpointDescription(
            endpoint.getEndpointUri().toString(),
            ldsServer.getApplicationDescription(),
            certificateByteString(endpoint.getCertificate()),
            endpoint.getMessageSecurity(),
            endpoint.getSecurityPolicy().getSecurityPolicyUri(),
            userTokenPolicies.toArray(new UserTokenPolicy[userTokenPolicies.size()]),
            Stack.UA_TCP_BINARY_TRANSPORT_URI,
            ubyte(endpoint.getSecurityLevel())
        );
    }

    private ByteString certificateByteString(Optional<X509Certificate> certificate) {
        if (certificate.isPresent()) {
            try {
                return ByteString.of(certificate.get().getEncoded());
            } catch (CertificateEncodingException e) {
                // logger.error("Error decoding certificate.", e);
                return ByteString.NULL_VALUE;
            }
        } else {
            return ByteString.NULL_VALUE;
        }
    }

    @Override
    public void onRegisterServer(ServiceRequest<RegisterServerRequest, RegisterServerResponse> serviceRequest)
        throws UaException {

        RegisterServerRequest request = serviceRequest.getRequest();

        List<String> allDiscoveryUrls = newArrayList(request.getServer().getDiscoveryUrls());

        RegisterServerResponse response = new RegisterServerResponse(
            serviceRequest.createResponseHeader());

        serviceRequest.setResponse(response);


        if (!request.getServer().getIsOnline()) {
            logger.info("Server {} sent an unregister request.", request.getServer().getServerUri());
            unregisterServerAppUri(request.getServer().getServerUri());
            if (onRegisterCallback != null) {
                onRegisterCallback.accept(request.getServer().getServerUri(), false);
            }
        } else {
            logger.info("Server {} sent a register request or update.", request.getServer().getServerUri());
            ApplicationDescription description = new ApplicationDescription(
                request.getServer().getServerUri(),
                request.getServer().getProductUri(),
                request.getServer().getServerNames()[0],
                ApplicationType.Server,
                null, null,
                a(allDiscoveryUrls, String.class)
            );

            if (onRegisterCallback != null && !serverExpMgnt.getRegisteredApplicationDescription().keySet()
                .contains(request.getServer().getServerUri())) {
                onRegisterCallback.accept(request.getServer().getServerUri(), true);
            }

            // if item already exists, then the corresponding timestamp will be updated. Otherwise a new entry is added
            serverExpMgnt.addElement(request.getServer().getServerUri(), description);
        }
    }

    @Override
    public void onGetEndpoints(ServiceRequest<GetEndpointsRequest, GetEndpointsResponse> serviceRequest) {
        GetEndpointsRequest request = serviceRequest.getRequest();

        List<String> profileUris = request.getProfileUris() != null
            ? newArrayList(request.getProfileUris())
            : new ArrayList<>();

        List<EndpointDescription> allEndpoints = ldsServer.getEndpoints().stream()
            .map(CompleteDiscoveryServiceSet.this::mapEndpoint)
            .filter(ed -> filterProfileUris(ed, profileUris))
            .collect(toList());

        List<EndpointDescription> matchingEndpoints = allEndpoints.stream()
            .filter(ed -> filterEndpointUrls(ed, request.getEndpointUrl()))
            .collect(toList());

        GetEndpointsResponse response = new GetEndpointsResponse(
            serviceRequest.createResponseHeader(),
            matchingEndpoints.isEmpty()
                ? a(allEndpoints, EndpointDescription.class)
                : a(matchingEndpoints, EndpointDescription.class)
        );

        serviceRequest.setResponse(response);
    }

    private boolean filterProfileUris(EndpointDescription endpoint, List<String> profileUris) {
        return profileUris.size() == 0 || profileUris.contains(endpoint.getTransportProfileUri());
    }

    private boolean filterEndpointUrls(EndpointDescription endpoint, String endpointUrl) {
        try {
            String requestedHost = new URI(endpointUrl).parseServerAuthority().getHost();
            String endpointHost = new URI(endpoint.getEndpointUrl()).parseServerAuthority().getHost();

            return requestedHost.equalsIgnoreCase(endpointHost);
        } catch (Throwable e) {
            // logger.warn("Unable to create URI.", e);
            return false;
        }
    }

    @Override
    public void onFindServers(ServiceRequest<FindServersRequest, FindServersResponse> serviceRequest) {

        FindServersRequest request = serviceRequest.getRequest();

        List<String> serverUris = request.getServerUris() != null
            ? newArrayList(request.getServerUris())
            : new ArrayList<>();

        List<ApplicationDescription> applicationDescriptions
            = newArrayList(getApplicationDescription(request.getEndpointUrl()));

        applicationDescriptions = applicationDescriptions.stream()
            .filter(ad -> filterServerUris(ad, serverUris))
            .collect(toList());

        FindServersResponse response = new FindServersResponse(
            serviceRequest.createResponseHeader(),
            a(applicationDescriptions, ApplicationDescription.class)
        );

        serviceRequest.setResponse(response);
    }

    public void onFindAllServers(ServiceRequest<FindServersRequest, FindServersResponse> service) throws UaException {
        FindServersRequest request = service.getRequest();

        List<ApplicationDescription> servers = new ArrayList<>();

        List<String> serverUris = request.getServerUris() != null
            ? newArrayList(request.getServerUris())
            : new ArrayList<>();

        // Information from other registered machines
        for (ApplicationDescription description : serverExpMgnt.getRegisteredApplicationDescription().values()) {

            if (serverUris.isEmpty()) {
                servers.add(description);
            } else if (serverUris.contains(description.getApplicationUri())) {
                servers.add(description);
            }
        }

        // Information from the LDS
        List<ApplicationDescription> applicationDescriptions
            = newArrayList(getApplicationDescription(request.getEndpointUrl()));

        applicationDescriptions = applicationDescriptions.stream()
            .filter(ad -> filterServerUris(ad, serverUris))
            .collect(toList());

        servers.addAll(applicationDescriptions);

        ResponseHeader header = service.createResponseHeader();
        FindServersResponse response = new FindServersResponse(
            header, servers.toArray(new ApplicationDescription[servers.size()]));

        service.setResponse(response);
    }

    private ApplicationDescription getApplicationDescription(String endpointUrl) {

        List<String> allDiscoveryUrls = newArrayList(ldsServer.getDiscoveryUrls());

        List<String> matchingDiscoveryUrls = allDiscoveryUrls.stream()
            .filter(discoveryUrl -> {
                try {
                    String requestedHost = new URI(endpointUrl).parseServerAuthority().getHost();
                    String discoveryHost = new URI(discoveryUrl).parseServerAuthority().getHost();

                    return requestedHost.equalsIgnoreCase(discoveryHost);
                } catch (Throwable e) {
                    // logger.warn("Unable to create URI.", e);
                    return false;
                }
            })
            .collect(toList());

        return new ApplicationDescription(
            ldsServer.getConfig().getApplicationUri(),
            ldsServer.getConfig().getProductUri(),
            ldsServer.getConfig().getApplicationName(),
            ApplicationType.Server,
            null, null,
            matchingDiscoveryUrls.isEmpty()
                ? a(allDiscoveryUrls, String.class)
                : a(matchingDiscoveryUrls, String.class)
        );
    }

    private boolean filterServerUris(ApplicationDescription ad, List<String> serverUris) {
        return serverUris.size() == 0 || serverUris.contains(ad.getApplicationUri());
    }

    public void unregisterServerAppUri(String appUri) {
        serverExpMgnt.removeElement(appUri);
        onRegisterCallback.accept(appUri, false);
    }

    public ApplicationDescription getRegisteredServer(String appUri) {
        return serverExpMgnt.getRegisteredApplicationDescription().get(appUri);
    }

}
