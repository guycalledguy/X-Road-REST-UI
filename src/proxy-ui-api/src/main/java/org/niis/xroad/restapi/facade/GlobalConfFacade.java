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
package org.niis.xroad.restapi.facade;

import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.conf.globalconf.GlobalGroupInfo;
import ee.ria.xroad.common.conf.globalconf.MemberInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.GlobalGroupId;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * GlobalConf facade.
 * Pure facade / wrapper, just delegates to GlobalConf. Zero business logic.
 * Exists to make testing easier by offering non-static methods.
 */
@Slf4j
@Component
public class GlobalConfFacade {

    /**
     * {@link GlobalConf#getMemberName(ClientId)}
     */
    public String getMemberName(ClientId identifier) {
        return GlobalConf.getMemberName(identifier);
    }

    /**
     * {@link GlobalConf#getGlobalGroupDescription(GlobalGroupId)}
     */
    public String getGlobalGroupDescription(GlobalGroupId identifier) {
        return GlobalConf.getGlobalGroupDescription(identifier);
    }

    /**
     * {@link GlobalConf#getMembers(String...)}
     */
    public List<MemberInfo> getMembers(String... instanceIdentifiers) {
        return GlobalConf.getMembers(instanceIdentifiers);
    }

    /**
     * {@link GlobalConf#getMemberClasses(String...)}
     */
    public Set<String> getMemberClasses(String instanceIdentifier) {
        return GlobalConf.getMemberClasses(instanceIdentifier);
    }

    /**
     * {@link GlobalConf#getMemberClasses(String...)}
     */
    public Set<String> getMemberClasses() {
        return GlobalConf.getMemberClasses();
    }

    /**
     * {@link GlobalConf#getInstanceIdentifier()}
     */
    public String getInstanceIdentifier() {
        return GlobalConf.getInstanceIdentifier();
    }

    /**
     * {@link GlobalConf#getInstanceIdentifiers()}
     */
    public List<String> getInstanceIdentifiers() {
        return GlobalConf.getInstanceIdentifiers();
    }

    /**
     * {@link GlobalConf#getGlobalGroups(String...)} ()}
     */
    public List<GlobalGroupInfo> getGlobalGroups(String... instanceIdentifiers) {
        return GlobalConf.getGlobalGroups(instanceIdentifiers);
    }
}
