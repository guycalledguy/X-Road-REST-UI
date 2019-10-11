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

import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.conf.serverconf.model.GroupMemberType;
import ee.ria.xroad.common.conf.serverconf.model.LocalGroupType;
import ee.ria.xroad.common.identifier.ClientId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.exceptions.ConflictException;
import org.niis.xroad.restapi.exceptions.ResourceNotFoundException;
import org.niis.xroad.restapi.repository.ClientRepository;
import org.niis.xroad.restapi.repository.LocalGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LocalGroup service
 */
@Slf4j
@Service
@Transactional
@PreAuthorize("denyAll")
public class LocalGroupService {

    private final LocalGroupRepository localGroupRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;

    /**
     * LocalGroupService constructor
     * @param localGroupRepository
     * @param clientRepository
     * @param clientService
     */
    @Autowired
    public LocalGroupService(LocalGroupRepository localGroupRepository, ClientRepository clientRepository,
            ClientService clientService) {
        this.localGroupRepository = localGroupRepository;
        this.clientRepository = clientRepository;
        this.clientService = clientService;
    }

    /**
     * Return local group
     * @param groupId
     * @return LocalGroupType
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_LOCAL_GROUPS')")
    public LocalGroupType getLocalGroup(Long groupId) {
        return localGroupRepository.getLocalGroup(groupId);
    }

    /**
     * Edit local group description
     * @return LocalGroupType
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_DESC')")
    public LocalGroupType updateDescription(Long groupId, String description) {
        LocalGroupType localGroupType = getLocalGroup(groupId);
        if (localGroupType == null) {
            throw new ResourceNotFoundException("LocalGroup with id " + groupId + " not found");
        }
        localGroupType.setDescription(description);
        localGroupType.setUpdated(new Date());
        localGroupRepository.saveOrUpdate(localGroupType);
        return localGroupType;
    }

    /**
     * Adds a local group to a client
     * @param id
     * @param localGroupTypeToAdd
     */
    @PreAuthorize("hasAuthority('ADD_LOCAL_GROUP')")
    public LocalGroupType addLocalGroup(ClientId id, LocalGroupType localGroupTypeToAdd) {
        ClientType clientType = clientRepository.getClient(id);
        if (clientType == null) {
            throw new ResourceNotFoundException("client with id " + id + " not found");
        }
        Optional<LocalGroupType> existingLocalGroupType = clientType.getLocalGroup().stream()
                .filter(localGroupType -> localGroupType.getGroupCode().equals(
                        localGroupTypeToAdd.getGroupCode())).findFirst();
        if (existingLocalGroupType.isPresent()) {
            throw new ConflictException(
                    "local group with code " + localGroupTypeToAdd.getGroupCode() + " already added");
        }
        localGroupRepository.persist(localGroupTypeToAdd);
        clientType.getLocalGroup().add(localGroupTypeToAdd);
        clientRepository.saveOrUpdate(clientType);
        return localGroupTypeToAdd;
    }

    /**
     * Adds a members to LocalGroup
     * @param memberIds
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_MEMBERS')")
    public void addLocalGroupMembers(Long groupId, List<ClientId> memberIds) {
        LocalGroupType localGroupType = getLocalGroup(groupId);
        if (localGroupType == null) {
            throw new ResourceNotFoundException("LocalGroup with id " + groupId + " not found");
        }
        List<GroupMemberType> membersToBeAdded = new ArrayList<>(memberIds.size());
        memberIds.forEach(memberId -> {
            Optional<ClientType> foundMember = clientService.findByClientId(memberId);
            if (!foundMember.isPresent()) {
                throw new ResourceNotFoundException("client with id " + memberId.toShortString() + " not found");
            }
            ClientId clientIdToBeAdded = foundMember.get().getIdentifier();
            boolean isAdded = localGroupType.getGroupMember().stream().anyMatch(groupMemberType ->
                    groupMemberType.getGroupMemberId().toShortString().trim()
                            .equals(clientIdToBeAdded.toShortString().trim()));
            if (isAdded) {
                throw new ConflictException("local group member already exists in group");
            }
            GroupMemberType groupMemberType = new GroupMemberType();
            groupMemberType.setAdded(new Date());
            groupMemberType.setGroupMemberId(clientIdToBeAdded);
            membersToBeAdded.add(groupMemberType);
        });
        localGroupRepository.saveOrUpdateAll(membersToBeAdded);
        localGroupType.getGroupMember().addAll(membersToBeAdded);
        localGroupRepository.saveOrUpdate(localGroupType);
    }

    /**
     * Deletes a local group
     * @param groupId
     */
    @PreAuthorize("hasAuthority('DELETE_LOCAL_GROUP')")
    public void deleteLocalGroup(Long groupId) {
        LocalGroupType existingLocalGroupType = getLocalGroup(groupId);
        if (existingLocalGroupType == null) {
            throw new ResourceNotFoundException("LocalGroup with id " + groupId + " not found");
        }
        localGroupRepository.delete(existingLocalGroupType);
    }

    /**
     * deletes a member from given local group
     * @param localGroupType
     * @param items
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_MEMBERS')")
    public void deleteGroupMember(LocalGroupType localGroupType, List<ClientId> items) {
        List<GroupMemberType> membersToBeRemoved = localGroupType.getGroupMember().stream()
                .filter(member -> items.stream()
                        .anyMatch(item -> item.toShortString().trim()
                                .equals(member.getGroupMemberId().toShortString().trim())))
                .collect(Collectors.toList());
        if (membersToBeRemoved.isEmpty()) {
            throw new ResourceNotFoundException("the requested group member was not found in local group");
        }
        localGroupType.getGroupMember().removeAll(membersToBeRemoved);
        localGroupRepository.saveOrUpdate(localGroupType);
    }
}
