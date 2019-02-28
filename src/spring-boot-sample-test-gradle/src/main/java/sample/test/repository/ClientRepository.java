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
package sample.test.repository;

import ee.ria.xroad.common.conf.serverconf.dao.ClientDAOImpl;
import ee.ria.xroad.common.conf.serverconf.dao.ServerConfDAOImpl;
import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.identifier.ClientId;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

/**
 * client repository
 */
@Slf4j
@Repository
@Transactional
public class ClientRepository {

    @Autowired
    private EntityManager entityManager;

    private Session getCurrentSession() {
        return entityManager.unwrap(Session.class);
    }

    /**
     * return one client
     * @param id
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClientType getClient(ClientId id) {
        ClientDAOImpl clientDAO = new ClientDAOImpl();
        return clientDAO.getClient(getCurrentSession(), id);
    }

    /**
     * return all clients
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClientType> getAllClients() {
        ServerConfDAOImpl serverConf = new ServerConfDAOImpl();
        List<ClientType> clientTypes = serverConf.getConf(getCurrentSession()).getClient();
        Hibernate.initialize(clientTypes);
        return clientTypes;
    }

//    /**
//     * return one client
//     * @param id
//     */
//    public ClientType getClient(ClientId id) {
//        ClientDAOImpl clientDAO = new ClientDAOImpl();
//        return DatabaseContextHelper.serverConfTransaction(
//                session -> {
//                    return clientDAO.getClient(session, id);
//                });
//    }

//    /**
//     * return all clients
//     * @return
//     */
//    public List<ClientType> getAllClients() {
//        ServerConfDAOImpl serverConf = new ServerConfDAOImpl();
//        return DatabaseContextHelper.serverConfTransaction(
//                session -> {
//                    List<ClientType> clientTypes = serverConf.getConf().getClient();
//                    Hibernate.initialize(clientTypes);
//                    return clientTypes;
//                });
//    }
}

