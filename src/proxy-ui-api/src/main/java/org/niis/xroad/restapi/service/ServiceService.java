/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.service;

import ee.ria.xroad.common.conf.serverconf.model.AccessRightType;
import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceDescriptionType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceType;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.XRoadId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.dto.AccessRightHolderDto;
import org.niis.xroad.restapi.exceptions.BadRequestException;
import org.niis.xroad.restapi.exceptions.Error;
import org.niis.xroad.restapi.exceptions.NotFoundException;
import org.niis.xroad.restapi.repository.ClientRepository;
import org.niis.xroad.restapi.repository.ServiceDescriptionRepository;
import org.niis.xroad.restapi.util.FormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * service class for handling services
 */
@Slf4j
@Service
@Transactional
@PreAuthorize("denyAll")
public class ServiceService {

    public static final String ERROR_SERVICE_NOT_FOUND = "services.service_not_found";
    public static final String ERROR_ACCESSRIGHT_NOT_FOUND = "services.accessright_not_found";

    private static final String HTTPS = "https";

    private final ClientRepository clientRepository;
    private final ServiceDescriptionRepository serviceDescriptionRepository;

    @Autowired
    public ServiceService(ClientRepository clientRepository,
            ServiceDescriptionRepository serviceDescriptionRepository) {
        this.clientRepository = clientRepository;
        this.serviceDescriptionRepository = serviceDescriptionRepository;
    }

    /**
     * get ServiceType by ClientId and service code that includes service version
     * see {@link FormatUtils#getServiceFullName(ServiceType)}
     * @param clientId
     * @param fullServiceCode
     * @return
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ServiceType getService(ClientId clientId, String fullServiceCode) {
        ClientType client = clientRepository.getClient(clientId);
        if (client == null) {
            throw new NotFoundException("Client " + clientId.toShortString() + " not found",
                    new Error(ClientService.CLIENT_NOT_FOUND_ERROR_CODE));
        }
        return getServiceFromClient(client, fullServiceCode);
    }

    /**
     * @param client
     * @param fullServiceCode
     * @return {@link ServiceType}
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ServiceType getServiceFromClient(ClientType client, String fullServiceCode) {
        Optional<ServiceType> foundService = client.getServiceDescription()
                .stream()
                .map(ServiceDescriptionType::getService)
                .flatMap(List::stream)
                .filter(serviceType -> FormatUtils.getServiceFullName(serviceType).equals(fullServiceCode))
                .findFirst();
        return foundService.orElseThrow(() -> new NotFoundException("Service " + fullServiceCode + " not found",
                new Error(ERROR_SERVICE_NOT_FOUND)));
    }

    /**
     * update a Service. clientId and fullServiceCode identify the updated service.
     * @param clientId clientId of the client associated with the service
     * @param fullServiceCode service code that includes service version
     * see {@link FormatUtils#getServiceFullName(ServiceType)}
     * @param url
     * @param urlAll
     * @param timeout
     * @param timeoutAll
     * @param sslAuth
     * @param sslAuthAll
     * @return ServiceType
     */
    @PreAuthorize("hasAuthority('EDIT_SERVICE_PARAMS')")
    public ServiceType updateService(ClientId clientId, String fullServiceCode,
            String url, boolean urlAll, Integer timeout, boolean timeoutAll,
            boolean sslAuth, boolean sslAuthAll) {
        if (!FormatUtils.isValidUrl(url)) {
            throw new BadRequestException("URL is not valid: " + url);
        }

        ServiceType serviceType = getService(clientId, fullServiceCode);

        if (serviceType == null) {
            throw new NotFoundException("Service " + fullServiceCode + " not found");
        }

        ServiceDescriptionType serviceDescriptionType = serviceType.getServiceDescription();

        serviceDescriptionType.getService().forEach(service -> {
            boolean serviceMatch = service == serviceType;
            if (urlAll || serviceMatch) {
                service.setUrl(url);
            }
            if (timeoutAll || serviceMatch) {
                service.setTimeout(timeout);
            }
            if (sslAuthAll || serviceMatch) {
                if (service.getUrl().startsWith(HTTPS)) {
                    service.setSslAuthentication(sslAuth);
                } else {
                    service.setSslAuthentication(null);
                }
            }
        });

        serviceDescriptionRepository.saveOrUpdate(serviceDescriptionType);

        return serviceType;
    }

    private AccessRightHolderDto accessRightTypeToDto(AccessRightType accessRightType,
            Map<String, String> localGroupDescMap) {
        AccessRightHolderDto accessRightHolderDto = new AccessRightHolderDto();
        accessRightHolderDto.setRightsGiven(
                FormatUtils.fromDateToOffsetDateTime(accessRightType.getRightsGiven()));
        accessRightHolderDto.setSubjectId(accessRightType.getSubjectId());
        accessRightHolderDto.setLocalGroupDescMap(localGroupDescMap);
        return accessRightHolderDto;
    }

    /**
     * Get access right holders by Service
     * @param clientId
     * @param fullServiceCode
     * @return
     */
    @PreAuthorize("hasAuthority('VIEW_SERVICE_ACL')")
    public List<AccessRightHolderDto> getAccessRightHoldersByService(ClientId clientId, String fullServiceCode) {
        ClientType clientType = clientRepository.getClient(clientId);
        if (clientType == null) {
            throw new NotFoundException("Client " + clientId.toShortString() + " not found",
                    new Error(ClientService.CLIENT_NOT_FOUND_ERROR_CODE));
        }

        ServiceType serviceType = getServiceFromClient(clientType, fullServiceCode);

        List<AccessRightHolderDto> accessRightHolderDtos = new ArrayList<>();

        Map<String, String> localGroupDescMap = new HashMap<>();

        clientType.getLocalGroup().forEach(localGroupType -> localGroupDescMap.put(localGroupType.getGroupCode(),
                localGroupType.getDescription()));

        clientType.getAcl().forEach(accessRightType -> {
            if (accessRightType.getEndpoint().getServiceCode().equals(serviceType.getServiceCode())) {
                AccessRightHolderDto accessRightHolderDto = accessRightTypeToDto(accessRightType, localGroupDescMap);
                accessRightHolderDtos.add(accessRightHolderDto);
            }
        });

        return accessRightHolderDtos;
    }

    /**
     * Remove AccessRights from a Service
     * @param clientId
     * @param fullServiceCode
     * @param subjectIds
     */
    @PreAuthorize("hasAuthority('EDIT_SERVICE_ACL')")
    public void deleteServiceAccessRights(ClientId clientId, String fullServiceCode, Set<XRoadId> subjectIds) {
        ClientType clientType = clientRepository.getClient(clientId);
        if (clientType == null) {
            throw new NotFoundException("Client " + clientId.toShortString() + " not found",
                    new Error(ClientService.CLIENT_NOT_FOUND_ERROR_CODE));
        }

        ServiceType serviceType = getServiceFromClient(clientType, fullServiceCode);

        List<AccessRightType> accessRightsToBeRemoved = clientType.getAcl()
                .stream()
                .filter(accessRightType -> accessRightType.getEndpoint().getServiceCode()
                        .equals(serviceType.getServiceCode()) && subjectIds.contains(accessRightType.getSubjectId()))
                .collect(Collectors.toList());

        List<XRoadId> subjectsToBeRemoved = accessRightsToBeRemoved
                .stream()
                .map(AccessRightType::getSubjectId)
                .collect(Collectors.toList());

        if (!subjectsToBeRemoved.containsAll(subjectIds)) {
            subjectIds.removeAll(subjectsToBeRemoved);

            List<String> redundantSubjects = subjectIds
                    .stream()
                    .map(XRoadId::toShortString)
                    .collect(Collectors.toList());
            throw new BadRequestException(new Error(ERROR_ACCESSRIGHT_NOT_FOUND, redundantSubjects));
        }

        clientType.getAcl().removeAll(accessRightsToBeRemoved);

        clientRepository.saveOrUpdate(clientType);
    }

}
