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
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
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
     * @return the LocalGroupType, or null if not found
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_LOCAL_GROUPS')")
    public LocalGroupType getLocalGroup(Long groupId) {
        return localGroupRepository.getLocalGroup(groupId);
    }

    /**
     * Edit local group description
     * @return LocalGroupType
     * @throws LocalGroupNotFoundException if local group with given id was not found
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_DESC')")
    public LocalGroupType updateDescription(Long groupId, String description) throws LocalGroupNotFoundException {
        LocalGroupType localGroupType = getLocalGroup(groupId);
        if (localGroupType == null) {
            throw new LocalGroupNotFoundException("LocalGroup with id " + groupId + " not found");
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
     * @throws DuplicateLocalGroupCodeException if local group with given code already exists
     * @throws ClientNotFoundException if client with given id was not found
     */
    @PreAuthorize("hasAuthority('ADD_LOCAL_GROUP')")
    public LocalGroupType addLocalGroup(ClientId id, LocalGroupType localGroupTypeToAdd)
            throws DuplicateLocalGroupCodeException, ClientNotFoundException {
        ClientType clientType = clientRepository.getClient(id);
        if (clientType == null) {
            throw new ClientNotFoundException("client with id " + id + " not found");
        }
        Optional<LocalGroupType> existingLocalGroupType = clientType.getLocalGroup().stream()
                .filter(localGroupType -> localGroupType.getGroupCode().equals(
                        localGroupTypeToAdd.getGroupCode())).findFirst();
        if (existingLocalGroupType.isPresent()) {
            throw new DuplicateLocalGroupCodeException(
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
     * @throws MemberAlreadyExistsException if given member already exists in the group
     * @throws LocalGroupNotFoundException if local group with given id was not found
     * @throws LocalGroupMemberNotFoundException if local group member was not found
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_MEMBERS')")
    public void addLocalGroupMembers(Long groupId, List<ClientId> memberIds) throws MemberAlreadyExistsException,
            LocalGroupNotFoundException, LocalGroupMemberNotFoundException {
        LocalGroupType localGroupType = getLocalGroup(groupId);
        if (localGroupType == null) {
            throw new LocalGroupNotFoundException("LocalGroup with id " + groupId + " not found");
        }
        List<GroupMemberType> membersToBeAdded = new ArrayList<>(memberIds.size());
        for (ClientId memberId: memberIds) {
            Optional<ClientType> foundMember = clientService.findByClientId(memberId);
            if (!foundMember.isPresent()) {
                throw new LocalGroupMemberNotFoundException("client with id "
                        + memberId.toShortString() + " not found");
            }
            ClientId clientIdToBeAdded = foundMember.get().getIdentifier();
            boolean isAdded = localGroupType.getGroupMember().stream()
                    .anyMatch(groupMemberType -> groupMemberType.getGroupMemberId().toShortString().trim()
                            .equals(clientIdToBeAdded.toShortString().trim()));
            if (isAdded) {
                throw new MemberAlreadyExistsException("local group member already exists in group");
            }
            GroupMemberType groupMemberType = new GroupMemberType();
            groupMemberType.setAdded(new Date());
            groupMemberType.setGroupMemberId(clientIdToBeAdded);
            membersToBeAdded.add(groupMemberType);
        }
        localGroupRepository.saveOrUpdateAll(membersToBeAdded);
        localGroupType.getGroupMember().addAll(membersToBeAdded);
        localGroupRepository.saveOrUpdate(localGroupType);
    }

    /**
     * Deletes a local group
     * @param groupId
     * @throws LocalGroupNotFoundException if local group with given id was not found
     */
    @PreAuthorize("hasAuthority('DELETE_LOCAL_GROUP')")
    public void deleteLocalGroup(Long groupId) throws LocalGroupNotFoundException {
        LocalGroupType existingLocalGroupType = getLocalGroup(groupId);
        if (existingLocalGroupType == null) {
            throw new LocalGroupNotFoundException("LocalGroup with id " + groupId + " not found");
        }
        localGroupRepository.delete(existingLocalGroupType);
    }

    /**
     * deletes a member from given local group
     * @param localGroupType
     * @param items
     * @throws LocalGroupMemberNotFoundException if local group member was not found in the group
     */
    @PreAuthorize("hasAuthority('EDIT_LOCAL_GROUP_MEMBERS')")
    public void deleteGroupMember(LocalGroupType localGroupType, List<ClientId> items)
            throws LocalGroupMemberNotFoundException {
        List<GroupMemberType> membersToBeRemoved = localGroupType.getGroupMember().stream()
                .filter(member -> items.stream()
                        .anyMatch(item -> item.toShortString().trim()
                                .equals(member.getGroupMemberId().toShortString().trim())))
                .collect(Collectors.toList());
        if (membersToBeRemoved.isEmpty()) {
            throw new LocalGroupMemberNotFoundException("the requested group member was not found in local group");
        }
        localGroupType.getGroupMember().removeAll(membersToBeRemoved);
        localGroupRepository.saveOrUpdate(localGroupType);
    }

    /**
     * Thrown when attempt to add member that already exists
     */
    public static class MemberAlreadyExistsException extends ServiceException {
        public static final String ERROR_LOCAL_GROUP_MEMBER_ALREADY_EXISTS = "local_group_member_already_exists";
        public MemberAlreadyExistsException(String s) {
            super(s, new ErrorDeviation(ERROR_LOCAL_GROUP_MEMBER_ALREADY_EXISTS));
        }
    }

    /**
     * Thrown when attempt to add member that already exists
     */
    public static class DuplicateLocalGroupCodeException extends ServiceException {
        public static final String ERROR_DUPLICATE_LOCAL_GROUP_CODE = "local_group_code_already_exists";
        public DuplicateLocalGroupCodeException(String s) {
            super(s, new ErrorDeviation(ERROR_DUPLICATE_LOCAL_GROUP_CODE));
        }
    }

    /**
     * If local group member was not found
     */
    public static class LocalGroupMemberNotFoundException extends NotFoundException {
        public static final String ERROR_LOCAL_GROUP_MEMBER_NOT_FOUND = "local_group_member_not_found";
        public LocalGroupMemberNotFoundException(String s) {
            super(s, new ErrorDeviation(ERROR_LOCAL_GROUP_MEMBER_NOT_FOUND));
        }
    }
}
